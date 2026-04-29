package com.seehold.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.seehold.constant.PromptConstant;
import com.seehold.entity.ChatMemoryEntity;
import com.seehold.entity.ChatSession;
import com.seehold.mapper.ChatMemoryMapper;
import com.seehold.mapper.ChatSessionMapper;

import com.seehold.service.ChatSessionService;
import io.netty.util.internal.StringUtil;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatSessionServiceImpl implements ChatSessionService {

    private final ChatSessionMapper chatSessionMapper;

    private final ChatMemoryMapper chatMemoryMapper;

    private final ChatClient fastClient;

    private final ChatClient kbClient;

    @Value("${spring.ai.openai.chat.options.model}")
    @Getter
    private String modelType;

    private final PromptTemplate summaryPrompt = new PromptTemplate(PromptConstant.SUMMARY_TEMPLATE_PROMPT);

    private final PromptTemplate titlePrompt = new PromptTemplate(PromptConstant.TITLE_TEMPLATE_PROMPT);

    @Override
    @Transactional
    public String chatWithKb(Long userId, String sessionId, String message) {
        // 1. 获取或创建会话
        ChatSession session = getSession(userId, sessionId);
        if (session == null) {
            session = createSession(userId);
        }

        // 2. 鉴权：确认会话归属当前用户
        if (!session.getUserId().equals(userId)) {
            throw new AccessDeniedException("无权访问该会话");
        }

        // 3. AI 对话（核心逻辑内聚在 Service 中）
        String systemContext = "当前用户ID: " + userId;
        String sId = session.getSessionId();
        String content = kbClient.prompt()
                .system(systemContext)
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sId))
                .call()
                .content();

        // 4. 更新会话统计
        updateSession(session.getSessionId());

        // 5. 按策略触发异步摘要
        int nextCount = (session.getMessageCount() == null ? 0 : session.getMessageCount()) + 1;
        if (nextCount == 1 || nextCount % 3 == 0) {
            generateSummary(session.getSessionId(), content, message);
        }

        return content;
    }

    @Override
    public ChatSession getSession(Long userId, String requestedSessionId) {
        //如果会话存在
        if (!StringUtil.isNullOrEmpty(requestedSessionId)) {
            ChatSession session = chatSessionMapper.selectBySessionId(requestedSessionId);
            if (session != null &&
                    session.getUserId().equals(userId) &&
                    Boolean.TRUE.equals(session.getIsActive())
            ) {
                log.info("恢复会话 {}", requestedSessionId);
                return session;
            }
        }
        return null;
    }

    @Override
    public ChatSession createSession(Long userId) {
        //todo 后续把第一次的聊天记录传入，生成title和summary
        String sessionId = UUID.randomUUID().toString();
        ChatSession session = ChatSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .title("新会话")
                .summary("新会话")
                .modelType(modelType)
                .isActive(true)
                .messageCount(0)
                .build();
        log.info("创建会话 {}", session);
        chatSessionMapper.insert(session);
        return session;
    }


    @Override
    public void updateSession(String sessionId) {
        chatSessionMapper.incrementMessageCount(sessionId);
    }

    @Override
    public List<ChatSession> listUserSessions(Long userId) {
        List<ChatSession> chatSessions = chatSessionMapper.selectList(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, userId)
                        .eq(ChatSession::getIsActive, true)
                        .orderByDesc(ChatSession::getLastMessageTime)
        );

        return chatSessions != null ? chatSessions : List.of();
    }

    @Override
    public List<ChatMemoryEntity> getSessionMessages(String sessionId, Long userId) {
        ChatSession session = chatSessionMapper.selectBySessionId(sessionId);
        if (session != null && session.getUserId().equals(userId)) {
            List<ChatMemoryEntity> chatMemories = chatMemoryMapper.selectList(
                    new LambdaQueryWrapper<ChatMemoryEntity>()
                            .eq(ChatMemoryEntity::getConversationId, sessionId)
                            .orderByAsc(ChatMemoryEntity::getTimestamp)
            );
            return chatMemories != null ? chatMemories : List.of();
        }
        return List.of();
    }

    @Override
    @Transactional
    public void deleteSession(String sessionId, Long userId) {
        ChatSession session = chatSessionMapper.selectBySessionId(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new AccessDeniedException("无权删除该会话");
        }

        // 软删除元数据
        chatSessionMapper.update(null, new LambdaUpdateWrapper<ChatSession>()
                .eq(ChatSession::getSessionId, sessionId)
                .set(ChatSession::getIsActive, false));

        log.info("删除会话: {}", sessionId);
    }

    /**
     * 自动生成会话摘要
     *
     * @param sessionId
     */
    @Override
    @Async
    @Transactional
    public void generateSummary(String sessionId, String content, String question) {
        try {
            // 获取最近 3 条消息生成摘要
            List<ChatMemoryEntity> messages = getRecentMessages(sessionId);

            StringBuilder context = new StringBuilder();

            if (messages.isEmpty()) {
                //todo 处理没有消息的情况
                context.append("USER: ").append(question).append("\n")
                        .append("ASSISTANT: ").append(content).append("\n");
            } else {
                // getRecentMessages 是倒序，这里翻转为正序，保证上下文可读
                Collections.reverse(messages);
                for (ChatMemoryEntity message : messages) {
                    context.append(message.getType()).append(": ")
                            .append(message.getContent())
                            .append("\n");
                }
            }

            ChatSession session = chatSessionMapper.selectBySessionId(sessionId);

            String summary = fastClient
                    .prompt(summaryPrompt.create(Map.of(
                            "summary", session.getSummary(),
                            "messages", context.toString()
                    )))
                    .call()
                    .content();

            String safeSummary = (summary == null || summary.isBlank()) ? "新会话" : summary;
            String title = fastClient
                    .prompt(titlePrompt.create(Map.of("summary", safeSummary)))
                    .call()
                    .content();

            // 生成摘更新
            LambdaUpdateWrapper<ChatSession> wrapper = new LambdaUpdateWrapper<ChatSession>()
                    .eq(ChatSession::getSessionId, sessionId)
                    .set(ChatSession::getSummary, summary)
                    .set(ChatSession::getTitle, title);

            chatSessionMapper.update(null, wrapper);

        } catch (Exception e) {
            log.warn("生成摘要失败: {}", sessionId, e);
        }
    }

    private List<ChatMemoryEntity> getRecentMessages(String sessionId) {
        List<ChatMemoryEntity> chatMemories = chatMemoryMapper.selectList(new LambdaQueryWrapper<ChatMemoryEntity>()
                .eq(ChatMemoryEntity::getConversationId, sessionId)
                .orderByDesc(ChatMemoryEntity::getTimestamp)
                .last("LIMIT " + 3));
        return chatMemories != null ? chatMemories : List.of();
    }

}

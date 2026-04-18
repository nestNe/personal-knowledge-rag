package com.seehold.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.seehold.constant.PromptConstant;
import com.seehold.entity.ChatMemory;
import com.seehold.entity.ChatSession;
import com.seehold.mapper.ChatMemoryMapper;
import com.seehold.mapper.ChatSessionMapper;

import com.seehold.service.ChatSessionService;
import io.netty.util.internal.StringUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Collections;

@Service
@AllArgsConstructor
@Slf4j
public class ChatSessionServiceImpl implements ChatSessionService {

    private final ChatSessionMapper chatSessionMapper;

    private final ChatMemoryMapper chatMemoryMapper;

    private final ChatClient fastClient;

    private final PromptTemplate summaryPrompt = new PromptTemplate(PromptConstant.SUMMARY_TEMPLATE_PROMPT);

    private final PromptTemplate titlePrompt = new PromptTemplate(PromptConstant.TITLE_TEMPLATE_PROMPT);

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
                .modelType("ASSISTANT")
                .isActive(true)
                .messageCount(0)
                .build();
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
    public List<ChatMemory> getSessionMessages(String sessionId, Long userId) {
        ChatSession session = chatSessionMapper.selectBySessionId(sessionId);
        if (session != null && session.getUserId().equals(userId)) {
            List<ChatMemory> chatMemories = chatMemoryMapper.selectList(
                    new LambdaQueryWrapper<ChatMemory>()
                            .eq(ChatMemory::getConversationId, sessionId)
                            .orderByAsc(ChatMemory::getTimestamp)
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
            List<ChatMemory> messages = getRecentMessages(sessionId);

            StringBuilder context = new StringBuilder();

            if (messages.isEmpty()) {
                //todo 处理没有消息的情况
                context.append("USER: ").append(question).append("\n")
                        .append("ASSISTANT: ").append(content).append("\n");
            } else {
                // getRecentMessages 是倒序，这里翻转为正序，保证上下文可读
                Collections.reverse(messages);
                for (ChatMemory message : messages) {
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

    private List<ChatMemory> getRecentMessages(String sessionId) {
        List<ChatMemory> chatMemories = chatMemoryMapper.selectList(new LambdaQueryWrapper<ChatMemory>()
                .eq(ChatMemory::getConversationId, sessionId)
                .orderByDesc(ChatMemory::getTimestamp)
                .last("LIMIT " + 3));
        return chatMemories != null ? chatMemories : List.of();
    }

}

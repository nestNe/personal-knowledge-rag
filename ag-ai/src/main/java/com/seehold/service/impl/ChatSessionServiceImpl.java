package com.seehold.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.seehold.entity.ChatMemory;
import com.seehold.entity.ChatSession;
import com.seehold.mapper.ChatMemoryMapper;
import com.seehold.mapper.ChatSessionMapper;

import com.seehold.service.ChatSessionService;
import io.netty.util.internal.StringUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class ChatSessionServiceImpl implements ChatSessionService {

    private final ChatSessionMapper chatSessionMapper;

    private final ChatMemoryMapper chatMemoryMapper;

    private final ChatClient fastClient;

    @Override
    public ChatSession getSession(Long userId, String requestedSessionId) {
        //如果会话存在
        if (StringUtil.isNullOrEmpty(requestedSessionId)) {
            ChatSession session = chatSessionMapper.selectBySessionId(requestedSessionId);
            if (session != null &&
                    session.getUserId().equals(userId) &&
                    session.getIsActive().equals(true)
            ) {
                log.info("恢复会话 {}", requestedSessionId);
                return session;
            }
        }
        return null;
    }

    @Override
    public ChatSession createSession(Long userId, String content, String sessionId) {
        //todo 后续把第一次的聊天记录传入，生成title和summary
        ChatSession session = ChatSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .title("New Session")
                .summary("New Session")
                .modelType("ASSISTANT")
                .isActive(true)
                .messageCount(1)
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
     * @param chatClient
     */
    @Override
    @Async
    @Transactional
    public void generateSummary(String sessionId, ChatClient chatClient) {
        try {
            // 获取最近几条消息生成摘要
            List<ChatMemory> messages = getRecentMessages(sessionId);
            if (messages.isEmpty()) return;

            String context = messages.stream()
                    .map(m -> m.getType() + ": " + m.getContent().substring(0, Math.min(50, m.getContent().length())))
                    .collect(Collectors.joining("\n"));

            String prompt = "请用一句话总结以下对话的核心主题（15字以内）：\n" + context;

            String summary = fastClient.prompt(prompt)
                    .call()
                    .content();

            // 首次生成摘要时同时更新标题 todo ...
            ChatSession session = chatSessionMapper.selectBySessionId(sessionId);
            LambdaUpdateWrapper<ChatSession> wrapper = new LambdaUpdateWrapper<ChatSession>()
                    .eq(ChatSession::getSessionId, sessionId)
                    .set(ChatSession::getSummary, summary);

            if (messages.size() < 3) {
                wrapper.set(ChatSession::getTitle, summary);
            }

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

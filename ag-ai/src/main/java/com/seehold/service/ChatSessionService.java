package com.seehold.service;

import com.seehold.entity.ChatMemory;
import com.seehold.entity.ChatSession;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

public interface ChatSessionService {

    /**
     * 获取或创建会话（核心方法）
     */
    ChatSession getSession(Long userId, String requestedSessionId);

    ChatSession createSession(Long userId, String content,String sessionId);

    /**
     * 更新会话统计（每次对话后调用）
     */
    void updateSession(String sessionId);

    /**
     * 获取用户的会话列表（不包含详细消息）
     */
    List<ChatSession> listUserSessions(Long userId);

    /**
     * 获取单个会话的详细聊天记录（联查 Spring AI 表）
     * 返回格式符合 Spring AI Message 结构
     */
    List<ChatMemory> getSessionMessages(String sessionId, Long userId);

    /**
     * 删除会话（软删除）
     */
    void deleteSession(String sessionId, Long userId);

    /**
     * AI 自动生成会话摘要（异步）
     */
    void generateSummary(String sessionId, ChatClient chatClient);
}

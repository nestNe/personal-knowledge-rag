package com.seehold.service;

import com.seehold.entity.ChatMemoryEntity;
import com.seehold.entity.ChatSession;

import java.util.List;

public interface ChatSessionService {

    /**
     * 执行知识库对话（完整业务流程）
     * @param userId   当前用户ID
     * @param sessionId 请求的会话ID（可为null，null时自动创建）
     * @param message  用户消息
     * @return AI 回复内容
     */
    String chatWithKb(Long userId, String sessionId, String message);


    /**
     * 获取或创建会话（核心方法）
     */
    ChatSession getSession(Long userId, String requestedSessionId);

    ChatSession createSession(Long userId);

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
    List<ChatMemoryEntity> getSessionMessages(String sessionId, Long userId);

    /**
     * 删除会话（软删除）
     */
    void deleteSession(String sessionId, Long userId);

    /**
     * AI 自动生成会话摘要（异步）
     */
    void generateSummary(String sessionId, String content,String question);
}

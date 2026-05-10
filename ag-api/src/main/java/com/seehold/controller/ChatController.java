package com.seehold.controller;

import com.seehold.dto.SessionDetailDTO;
import com.seehold.entity.ChatMemoryEntity;
import com.seehold.entity.ChatSession;
import com.seehold.security.UserDetailsImpl;
import com.seehold.service.ChatSessionService;
import lombok.AllArgsConstructor;
import com.seehold.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@Slf4j
@RequestMapping("/api/ai")
public class ChatController {

    private final ChatSessionService chatSessionService;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 统一聊天接口 — Manager 模型自动分析意图，
     * 将任务分发给合适的 Worker（用户管理 / 知识库 / 代码执行）。
     * 支持会话记忆，不传 sessionId 时自动创建新会话。
     */
    @PostMapping("/chat")
    @PreAuthorize("hasAuthority('agent:chat')")
    public Result<String> chat(
            @RequestParam("message") String message,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        log.info("Received message: {}", message);
        String content = chatSessionService.chatWithAgent(userDetails.getId(), sessionId, message);
        return Result.success(content);
    }

    /**
     * 知识库聊天接口（保留兼容，前端 KB 模式仍在使用）
     */
    @PostMapping("/chat/kb")
    @PreAuthorize("hasAuthority('agent:chat')")
    public Result<String> chatKb(
            @RequestParam("message") String message,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        log.info("Received message: {}", message);
        String content = chatSessionService.chatWithKb(userDetails.getId(), sessionId, message);
        return Result.success(content);
    }

    /**
     * 获取当前用户的会话列表（用于左侧边栏）
     */
    @GetMapping("/sessions")
    @PreAuthorize("hasAuthority('agent:chat')")
    public Result<List<ChatSession>> listSessions(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        List<ChatSession> sessions = chatSessionService.listUserSessions(userDetails.getId());
        return Result.success(sessions);
    }

    /**
     * 获取单个会话的详细聊天记录（用于进入会话后加载历史）
     */
    @GetMapping("/sessions/{sessionId}/messages")
    @PreAuthorize("hasAuthority('agent:chat')")
    public Result<SessionDetailDTO> getSessionMessages(
            @PathVariable String sessionId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        // 查询会话元数据
        ChatSession session = chatSessionService.getSession(userDetails.getId(), sessionId);
        if (session == null || !session.getUserId().equals(userDetails.getId())) {
            return Result.error("会话不存在或无权限");
        }

        // 查询详细聊天记录（从 Spring AI 表）
        List<ChatMemoryEntity> memories = chatSessionService.getSessionMessages(sessionId, userDetails.getId());

        return Result.success(SessionDetailDTO.builder()
                .chatSession(session)
                .chatMemories(memories)
                .build());
    }


}

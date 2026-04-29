package com.seehold.controller;

import com.seehold.dto.SessionDetailDTO;
import com.seehold.entity.ChatMemoryEntity;
import com.seehold.entity.ChatSession;
import com.seehold.security.UserDetailsImpl;
import com.seehold.service.ChatSessionService;
import lombok.AllArgsConstructor;
import com.seehold.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
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

    private final ChatClient userManageClient;


    private final ChatSessionService chatSessionService;

    private final RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/chat")
    @PreAuthorize("hasAuthority('agent:chat')")
    public Result<String> chat(@RequestParam("message") String message,
                               @AuthenticationPrincipal UserDetailsImpl userDetails) {
        log.info("Received message : {}", message);
        String prompt = "当前用户id:" + userDetails.getId() + ", " + message;
        String res = userManageClient
                .prompt(prompt)
                .call()
                .content();

        return Result.success(res);
    }

    /**
     * 聊天接口
     * 记忆功能、摘要功能、知识库功能
     *
     * @param message
     * @param sessionId
     * @param userDetails
     * @return
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

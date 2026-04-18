package com.seehold.controller;

import com.seehold.dto.SessionDetailDTO;
import com.seehold.entity.ChatSession;
import com.seehold.security.UserDetailsImpl;
import com.seehold.service.ChatSessionService;
import lombok.AllArgsConstructor;
import com.seehold.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@Slf4j
@RequestMapping("/api/ai")
public class ChatController {

    private final ChatClient userManageClient;

    private final ChatClient kbClient;

    private final ChatSessionService chatSessionService;

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

        Long userId = userDetails.getId();
        String systemContext = "当前用户ID: " + userId;

        // 1. 获取会话
        ChatSession session = chatSessionService.getSession(userId, sessionId);

        //新会话
        if (session == null)
            session = chatSessionService.createSession(userId);

        // 3. 调用 AI（使用 Spring AI 的 Advisor 机制）
        String content = "";
        if (session.getSessionId() != null && session.getUserId().equals(userId))
            content = chatCore(systemContext, message, session.getSessionId());

        // 4. 更新会话统计
        chatSessionService.updateSession(session.getSessionId());

        // 5. 异步生成摘要（首条消息 + 每 3 条消息触发一次）
        int nextCount = (session.getMessageCount() == null ? 0 : session.getMessageCount()) + 1;
        if (nextCount == 1 || nextCount % 3 == 0)
            chatSessionService.generateSummary(session.getSessionId(), content, message);

        return Result.success(content);
    }

    public String chatCore(String systemContext, String message, String sessionId) {
        return kbClient.prompt()
                .system(systemContext)
                .user(message)
                .advisors(a -> a
                        .param(ChatMemory.CONVERSATION_ID, sessionId)
                )
                .call()
                .content();
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
        List<com.seehold.entity.ChatMemory> memories = chatSessionService.getSessionMessages(sessionId, userDetails.getId());

        return Result.success(SessionDetailDTO.builder()
                .chatSession(session)
                .chatMemories(memories)
                .build());
    }


}

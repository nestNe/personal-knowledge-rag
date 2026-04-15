package com.seehold.controller;

import com.seehold.security.UserDetailsImpl;
import lombok.AllArgsConstructor;
import com.seehold.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
@Slf4j
@RequestMapping("/api/ai")
public class ChatController {

    private final ChatClient userManageClient;

    private final ChatClient kbClient;

    /**
     * 对话
     *
     * @param message
     * @return
     */
    @PostMapping("/chat")
    @PreAuthorize("hasAuthority('agent:chat')")
    public Result<String> chat(@RequestParam("message") String message,
                               @AuthenticationPrincipal UserDetailsImpl userDetails) {
        log.info("Received message: {}", message);
        String prompt = "当前用户id:" + userDetails.getId() + ", " + message;
        String res = userManageClient
                .prompt(prompt)
                .call()
                .content();

        return Result.success(res);
    }

    @PostMapping("/chat/kb")
    @PreAuthorize("hasAuthority('agent:chat')")
    public Result<String> chatKb(@RequestParam("message") String message, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        log.info("Received message: {}", message);
        String prompt = "当前用户id:" + userDetails.getId() + "," + message;
        String res = kbClient
                .prompt(prompt)
                .call()
                .content();
        return Result.success(res);
    }

}

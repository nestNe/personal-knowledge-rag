package com.seehold.controller;

import com.seehold.security.UserDetailsImpl;
import lombok.AllArgsConstructor;
import com.seehold.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@AllArgsConstructor
@Slf4j
@RequestMapping("/api/ai")
public class ChatController {

    private final ChatClient userManageClient;

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

        log.info("Result of message: {}", res);
        return Result.success(res);
    }

}

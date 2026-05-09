package com.seehold.config;

import com.seehold.constant.PromptConstant;
import com.seehold.tools.CodeExecutorTools;
import com.seehold.tools.SearchEmbedTools;
import com.seehold.tools.UserTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient fastClient(OpenAiChatModel model) {
        return ChatClient.builder(model)
                .defaultSystem(PromptConstant.FAST_PROMPT)
                .build();
    }

    @Bean
    public ChatClient userManageClient(OpenAiChatModel model, UserTools userTools) {
        return ChatClient.builder(model)
                .defaultSystem(PromptConstant.USER_MANAGE_PROMPT)
                .defaultTools(userTools)
                .build();
    }

    @Bean
    public ChatClient kbClient(OpenAiChatModel model,
                               ChatMemory chatMemory,
                               SearchEmbedTools searchEmbedTools) {
        return ChatClient.builder(model)
                .defaultSystem(PromptConstant.KB_PROMPT)
                .defaultTools(searchEmbedTools)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    /**
     * 代码执行 Client：挂载 CodeExecutorTools，让模型能自主生成并运行代码。
     *
     * <p>也可以把 CodeExecutorTools 追加到 kbClient 的 defaultTools 中，
     * 让知识库对话同时具备代码执行能力：</p>
     * <pre>
     *   .defaultTools(searchEmbedTools, codeExecutorTools)
     * </pre>
     */
    @Bean
    public ChatClient codeClient(OpenAiChatModel model,
                                 ChatMemory chatMemory,
                                 CodeExecutorTools codeExecutorTools) {
        return ChatClient.builder(model)
                .defaultSystem(PromptConstant.CODE_EXECUTOR_PROMPT)
                .defaultTools(codeExecutorTools)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }
}

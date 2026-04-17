package com.seehold.config;

import com.seehold.constant.PromptConstant;
import com.seehold.tools.SearchEmbedTools;
import com.seehold.tools.UserTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

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
}

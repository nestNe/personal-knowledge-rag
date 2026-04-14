package com.seehold.config;

import com.seehold.constant.PromptConstant;
import com.seehold.tools.SearchEmbedTools;
import com.seehold.tools.UserTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ChatClientConfig {

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
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    /*@Bean
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(1536)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .initializeSchema(true)
                .schemaName("public")
                .vectorTableName("base_framework")
                .maxDocumentBatchSize(10000)
                .build();
    }*/
}

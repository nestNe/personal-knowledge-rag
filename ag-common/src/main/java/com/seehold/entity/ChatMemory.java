package com.seehold.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@TableName("spring_ai_chat_memory")
public class ChatMemory {
    private String conversationId;
    private String content;
    private String type;        // USER/ASSISTANT/SYSTEM/TOOL
    private LocalDateTime timestamp;
}
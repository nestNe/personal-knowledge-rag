package com.seehold.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatMemory {
    private String conversationId;
    private String content;
    private String type;        // USER/ASSISTANT/SYSTEM/TOOL
    private LocalDateTime timestamp;
}
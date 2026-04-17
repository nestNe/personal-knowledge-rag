package com.seehold.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@TableName("spring_ai_chat_session")
public class ChatSession {
    private Long id;
    private String sessionId;
    private Long userId;
    private String title;
    private String summary;        // 新增：AI 生成的会话摘要
    private String modelType;
    private Boolean isActive;
    private Integer messageCount;
    private LocalDateTime lastMessageTime;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

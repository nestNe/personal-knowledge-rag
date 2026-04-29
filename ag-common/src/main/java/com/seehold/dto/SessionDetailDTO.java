package com.seehold.dto;

import com.seehold.entity.ChatMemoryEntity;
import com.seehold.entity.ChatSession;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SessionDetailDTO {
    private ChatSession chatSession;
    private List<ChatMemoryEntity> chatMemories;
}

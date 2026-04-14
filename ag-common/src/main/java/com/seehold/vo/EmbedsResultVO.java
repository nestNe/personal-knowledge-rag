package com.seehold.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EmbedsResultVO {
    private List<EmbedResultVO> embeds;
    private List<String> errs;
}

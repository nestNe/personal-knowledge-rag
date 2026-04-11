package com.seehold.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmbedResultVO {
    private String filename;        // 文件名
    private String category;        // 分类
    private int totalLines;         // 总行数
    private int successCount;       // 成功入库数
    private long costTimeMs;        // 耗时（毫秒）
}
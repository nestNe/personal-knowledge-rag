package com.seehold.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 对应 pgvector 的向量表：vector_store
 * 这里只建最小字段用于查询（embedding 不参与本次接口返回）。
 */
@Data
@TableName("vector_store")
public class VectorStoreEntity {

    @TableId("id")
    private String id;

    @TableField("content")
    private String content;

    @TableField("metadata")
    private String metadata;
}

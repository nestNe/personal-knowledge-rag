package com.seehold.vo;

import com.seehold.entity.MetaData;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VectorStoreRecordVO {

    private String id;

    private String content;

    private MetaData metaData;
}

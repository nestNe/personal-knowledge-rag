package com.seehold.service;

import com.seehold.vo.PageVO;
import com.seehold.vo.VectorStoreRecordVO;

public interface VectorStoreQueryService {

    PageVO<VectorStoreRecordVO> pageByCurrentUser(Long userId, Long current, Long size, String keyword);

    VectorStoreRecordVO getDetailById(String id);
}

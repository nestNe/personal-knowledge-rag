package com.seehold.service;

import com.seehold.vo.EmbedResultVO;
import org.springframework.web.multipart.MultipartFile;

public interface KnowledgeEmbedService {
    EmbedResultVO processSingleFile(MultipartFile file, String category) throws Exception;
}

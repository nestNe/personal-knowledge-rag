package com.seehold.controller;

import com.seehold.result.Result;
import com.seehold.service.KnowledgeEmbedService;
import com.seehold.vo.EmbedResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/embedding")
@RequiredArgsConstructor
@Slf4j
public class EmbeddingController {

    private final KnowledgeEmbedService knowledgeEmbedService;

    /**
     * 接收单个文本文件
     *
     * @param file
     * @param type
     * @return
     */
    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('agent:embedding')")
    public Result<EmbedResultVO> embedBatch(@RequestParam("files") MultipartFile file, @RequestParam("type") String type) {
        if (file.isEmpty())
            return Result.error("文件不能为空");

        // 校验文件类型
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".txt"))
            return Result.error("仅支持 .txt 文本文件");

        try {
            EmbedResultVO result = knowledgeEmbedService.processSingleFile(file, type);
            return Result.success(result);
        } catch (Exception e) {
            log.error("文件向量化失败: {}", filename, e);
            return Result.error("处理失败: " + e.getMessage());
        }
    }

    /**
     * 批量向量化存储
     * 多线程
     *
     * @param files
     * @return
     */
    @PostMapping("/batchs")
    @PreAuthorize("hasAuthority('agent:embedding')")
    public Result<String> embedBatchs(
            @RequestParam("files") List<MultipartFile> files
    ) {

        return Result.success();
    }

}

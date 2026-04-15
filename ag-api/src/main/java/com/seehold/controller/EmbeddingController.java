package com.seehold.controller;

import com.seehold.result.Result;
import com.seehold.security.UserDetailsImpl;
import com.seehold.service.KnowledgeEmbedService;
import com.seehold.vo.EmbedResultVO;
import com.seehold.vo.EmbedsResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
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
    public Result<EmbedResultVO> embedBatch(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        if (file.isEmpty())
            return Result.error("文件不能为空");

        // 校验文件类型
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".txt"))
            return Result.error("仅支持 .txt 文本文件");

        try {
            EmbedResultVO result = knowledgeEmbedService.processSingleFile(file, type, userDetails.getId());
            return Result.success(result);
        } catch (Exception e) {
            log.error("文件向量化失败: {}", filename, e);
            return Result.error("处理失败: " + e.getMessage());
        }
    }


    /**
     * 批量向量存储
     * todo 多线程批处理
     *
     * @param files
     * @param type
     * @return
     */
    @PostMapping("/batches")
    @PreAuthorize("hasAuthority('agent:embedding')")
    public Result<EmbedsResultVO> embedBatches(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("type") String type,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        if (files == null || files.isEmpty())
            return Result.error("文件不能为空");

        List<EmbedResultVO> results = new ArrayList<>();
        List<String> errs = new ArrayList<>();

        for (MultipartFile file : files) {
            // 校验文件类型
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".txt")) {
                errs.add("文件 " + filename + " 仅支持 .txt 文本文件");
                continue;
            }

            try {
                EmbedResultVO result = knowledgeEmbedService.processSingleFile(file, type, userDetails.getId());
                results.add(result);
            } catch (Exception e) {
                log.error("文件向量化失败: {}", filename, e);
                errs.add("文件 " + filename + " 处理失败: " + e.getMessage());
            }
        }

        return Result.success(EmbedsResultVO.builder()
                .embeds(results)
                .errs(errs)
                .build());
    }

}

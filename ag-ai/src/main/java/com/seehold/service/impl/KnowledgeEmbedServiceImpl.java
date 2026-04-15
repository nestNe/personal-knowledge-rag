package com.seehold.service.impl;

import com.seehold.service.KnowledgeEmbedService;
import com.seehold.vo.EmbedResultVO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class KnowledgeEmbedServiceImpl implements KnowledgeEmbedService {

    private final VectorStore vectorStore;

    /**
     * 处理单文件：按行分割并入库
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public EmbedResultVO processSingleFile(MultipartFile file, String category, Long userId) throws IOException {
        String filename = file.getOriginalFilename();
        long startTime = System.currentTimeMillis();

        // 1. 读取文件内容（UTF-8编码）
        //todo 优化为StringBuilder
        String content;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            content = reader.lines().collect(Collectors.joining("\n"));
        }

        // 2. 按行分割并过滤
        List<String> paragraphs = Arrays.stream(content.split("\\n\\s*\\n"))
                .map(String::trim)
                .filter(p -> !p.isEmpty())           // 过滤空段落
                .filter(p -> p.length() >= 10)       // 段落通常较长，阈值可适当调高
                .toList();

        if (paragraphs.isEmpty()) {
            throw new IllegalArgumentException("文件内容为空或没有有效文本行");
        }

        log.info("文件 [{}] 读取完成，有效段落数: {}", filename, paragraphs.size());

        // 3. 构建 Document 列表（带元数据）
        List<Document> documents = new ArrayList<>();
        int lineNum = 0;
        for (String paragraph : paragraphs) {
            lineNum++;
            // 元数据记录来源信息
            Map<String, Object> metadata = Map.of(
                    "userId", userId.toString(),
                    "source", filename == null ? "unknown" : filename,
                    "category", category,
                    "lineNumber", lineNum,
                    "totalLines", paragraphs.size(),
                    "uploadTime", LocalDateTime.now().toString()
            );

            documents.add(new Document(paragraph, metadata));
        }

        // 4. 批量向量化入库（分批处理，避免单次请求过大）
        int batchSize = 100;
        int successCount = 0;

        for (int i = 0; i < documents.size(); i += batchSize) {
            List<Document> batch = documents.subList(i, Math.min(i + batchSize, documents.size()));
            try {
                vectorStore.add(batch);
                successCount += batch.size();
                log.debug("批量入库进度: {}/{}", successCount, documents.size());
            } catch (Exception e) {
                log.error("批量入库失败，批次: {}-{}/{}", i, i + batch.size(), documents.size(), e);
                throw new RuntimeException("向量入库失败: " + e.getMessage());
            }
        }

        long costTime = System.currentTimeMillis() - startTime;
        log.info("文件 [{}] 处理完成，入库 {} 条，耗时 {}ms", filename, successCount, costTime);

        // 5. 返回结果
        return EmbedResultVO.builder()
                .filename(filename)
                .category(category)
                .totalLines(paragraphs.size())
                .successCount(successCount)
                .costTimeMs(costTime)
                .build();
    }

}

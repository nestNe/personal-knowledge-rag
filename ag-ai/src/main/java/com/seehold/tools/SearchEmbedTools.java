package com.seehold.tools;

import com.seehold.query.EmbedQuery;
import com.seehold.query.EmbedQueryResult;
import com.seehold.query.EmbedResult;
import com.seehold.entity.MetaData;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@AllArgsConstructor
public class SearchEmbedTools {

    private final VectorStore vectorStore;

    @Tool(description = """
            从用户的个人知识库中检索与当前问题相关的文本片段。
            如果返回结果为空，可能当前用户没有相关的个人知识库。
            """)
    public EmbedQueryResult searchEmbed(EmbedQuery embedQuery) {
        log.info("调用searchEmbed, embedQuery: {}", embedQuery);
        List<EmbedResult> embeds = new ArrayList<>();

        SearchRequest.Builder request = SearchRequest.builder()
                .query(embedQuery.getQuery())
                .topK(embedQuery.getTopK());

        //TODO 后续在数据库查询分类，再精确匹配
        if (embedQuery.getUserId() != null && !embedQuery.getUserId().isEmpty()) {
            Filter.Expression filterExpr = new Filter.Expression(
                    Filter.ExpressionType.EQ,
                    new Filter.Key("userId"),
                    new Filter.Value(embedQuery.getUserId())
            );
            request.filterExpression(filterExpr);
        }

        List<Document> documents = vectorStore.similaritySearch(request.build());

        documents.forEach(document -> {
            Map<String, Object> map = document.getMetadata();
            MetaData metaData = MetaData.builder()
                    .source(map.get("source").toString())
                    .category(map.get("category").toString())
                    .lineNumber(Integer.parseInt(map.get("lineNumber").toString()))
                    .totalLines(Integer.parseInt(map.get("totalLines").toString()))
                    .uploadTime(map.get("uploadTime").toString())
                    .userId(map.get("userId").toString())
                    .build();

            EmbedResult embResult = EmbedResult.builder()
                    .text(document.getText())
                    .score(document.getScore() != null ? document.getScore() : 0.0)
                    .metaData(metaData != null ? metaData : MetaData.builder().build())
                    .build();
            embeds.add(embResult);
        });

        log.info("调用searchEmbed, result: score({}) >> {}", embeds.get(0).getScore(),embeds.get(0).getMetaData());

        return EmbedQueryResult.builder()
                .topK(embedQuery.getTopK())
                .embeds(embeds)
                .build();
    }
}

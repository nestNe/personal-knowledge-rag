package com.seehold.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.seehold.dto.VectorStoreRowDTO;
import com.seehold.entity.VectorStoreEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VectorStoreMapper extends BaseMapper<VectorStoreEntity> {

    @Select("""
            <script>
            SELECT
                id::text AS id,
                content,
                metadata->>'userId' AS user_id,
                metadata->>'source' AS source,
                metadata->>'category' AS category,
                metadata->>'lineNumber' AS line_number,
                metadata->>'totalLines' AS total_lines,
                metadata->>'uploadTime' AS upload_time
            FROM vector_store
            WHERE metadata->>'userId' = #{userId}
              <if test="keyword != null and keyword != ''">
                AND (
                    content ILIKE CONCAT('%', #{keyword}, '%')
                    OR metadata->>'source' ILIKE CONCAT('%', #{keyword}, '%')
                    OR metadata->>'category' ILIKE CONCAT('%', #{keyword}, '%')
                )
              </if>
            ORDER BY id DESC
            </script>
            """)
    IPage<VectorStoreRowDTO> selectPageByUserId(IPage<?> page,
                                               @Param("userId") String userId,
                                               @Param("keyword") String keyword);

    @Select("""
            SELECT
                id::text AS id,
                content,
                metadata->>'userId' AS user_id,
                metadata->>'source' AS source,
                metadata->>'category' AS category,
                metadata->>'lineNumber' AS line_number,
                metadata->>'totalLines' AS total_lines,
                metadata->>'uploadTime' AS upload_time
            FROM vector_store
            WHERE id = CAST(#{id} AS uuid)
            """)
    VectorStoreRowDTO selectDetailById(@Param("id") String id);
}

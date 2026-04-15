package com.seehold.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.seehold.dto.VectorStoreRowDTO;
import com.seehold.entity.MetaData;
import com.seehold.mapper.VectorStoreMapper;
import com.seehold.service.VectorStoreQueryService;
import com.seehold.vo.PageVO;
import com.seehold.vo.VectorStoreRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VectorStoreQueryServiceImpl implements VectorStoreQueryService {

    private final VectorStoreMapper vectorStoreMapper;

    @Override
    public PageVO<VectorStoreRecordVO> pageByCurrentUser(Long userId, Long current, Long size, String keyword) {
        long safeCurrent = current == null || current < 1 ? 1 : current;
        long safeSize = size == null || size < 1 ? 10 : size;

        IPage<VectorStoreRowDTO> page = vectorStoreMapper.selectPageByUserId(
                new Page<>(safeCurrent, safeSize),
                userId.toString(),
                StringUtils.hasText(keyword) ? keyword.trim() : null
        );

        List<VectorStoreRecordVO> records = page.getRecords().stream()
                .filter(Objects::nonNull)
                .map(this::toVO)
                .collect(Collectors.toList());

        return new PageVO<>(safeCurrent, safeSize, page.getTotal(), records);
    }

    @Override
    public VectorStoreRecordVO getDetailById(String id) {
        if (!StringUtils.hasText(id)) {
            return null;
        }
        VectorStoreRowDTO row = vectorStoreMapper.selectDetailById(id.trim());
        return row == null ? null : toVO(row);
    }

    private VectorStoreRecordVO toVO(VectorStoreRowDTO row) {
        return VectorStoreRecordVO.builder()
                .id(row.getId())
                .content(row.getContent())
                .metaData(MetaData.builder()
                        .userId(row.getUserId())
                        .source(row.getSource())
                        .category(row.getCategory())
                        .lineNumber(parseIntValue(row.getLineNumber()))
                        .totalLines(parseIntValue(row.getTotalLines()))
                        .uploadTime(row.getUploadTime())
                        .build())
                .build();
    }

    private int parseIntValue(String value) {
        if (!StringUtils.hasText(value)) {
            return 0;
        }
        return Integer.parseInt(value);
    }
}

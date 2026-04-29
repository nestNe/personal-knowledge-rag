package com.seehold.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import com.seehold.entity.ChatMemoryEntity;

@Mapper
public interface ChatMemoryMapper extends BaseMapper<ChatMemoryEntity> {
}

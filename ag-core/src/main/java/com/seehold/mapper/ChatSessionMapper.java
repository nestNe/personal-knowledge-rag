package com.seehold.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seehold.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    @Select("""
            select * from spring_ai_chat_session where session_id = #{sessionId}
            """)
    ChatSession selectBySessionId(String sessionId);

    @Update("update spring_ai_chat_session set " +
            "message_count = message_count + 1, last_message_time = current_timestamp " +
            "where session_id = #{sessionId}")
    void incrementMessageCount(String sessionId);
}

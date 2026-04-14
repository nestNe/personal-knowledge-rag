CREATE TABLE chat_session
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL,
    title          VARCHAR(200),
    prompt_type    VARCHAR(20),
    summary        TEXT,
    summary_vector VECTOR(1536),
    message_count  INT              DEFAULT 0,
    total_tokens   BIGINT           DEFAULT 0,
    created_at     TIMESTAMP        DEFAULT NOW(),
    updated_at     TIMESTAMP        DEFAULT NOW(),
    is_deleted     BOOLEAN          DEFAULT FALSE
);

COMMENT ON TABLE chat_session IS '对话会话表，存储用户与AI的对话上下文';
COMMENT ON COLUMN chat_session.id IS '主键ID，会话唯一标识';
COMMENT ON COLUMN chat_session.user_id IS '用户ID，关联用户表';
COMMENT ON COLUMN chat_session.title IS '会话标题，AI自动生成';
COMMENT ON COLUMN chat_session.prompt_type IS '使用的系统提示词类型';
COMMENT ON COLUMN chat_session.summary IS '会话核心摘要，动态更新';
COMMENT ON COLUMN chat_session.summary_vector IS '摘要向量化，用于相似会话推荐';
COMMENT ON COLUMN chat_session.message_count IS '消息数量统计';
COMMENT ON COLUMN chat_session.total_tokens IS '累计token消耗';
COMMENT ON COLUMN chat_session.created_at IS '创建时间';
COMMENT ON COLUMN chat_session.updated_at IS '最后更新时间';
COMMENT ON COLUMN chat_session.is_deleted IS '是否删除（软删除）';

CREATE TABLE chat_message
(
    id            BIGSERIAL PRIMARY KEY,
    session_id    UUID        NOT NULL,
    role          VARCHAR(20) NOT NULL,
    content       TEXT,
    content_type  VARCHAR(20) DEFAULT 'text',
    extra_data    JSONB,
    tool_calls    JSONB,
    tool_results  JSONB,
    tokens_used   INT,
    latency_ms    INT,
    is_summarized BOOLEAN     DEFAULT FALSE,
    created_at    TIMESTAMP   DEFAULT NOW()
);

COMMENT ON TABLE chat_message IS '消息记录表，存储用户与AI的对话消息';
COMMENT ON COLUMN chat_message.id IS '主键ID，自增';
COMMENT ON COLUMN chat_message.session_id IS '所属会话ID，关联chat_session表';
COMMENT ON COLUMN chat_message.role IS '消息角色：user(用户)/assistant(AI)/system(系统)/tool(工具)';
COMMENT ON COLUMN chat_message.content IS '消息内容';
COMMENT ON COLUMN chat_message.content_type IS '内容类型：text(文本)/image_url(图片链接)/file_ref(文件引用)';
COMMENT ON COLUMN chat_message.extra_data IS '扩展数据，JSON格式存储图片URL、文件元数据等';
COMMENT ON COLUMN chat_message.tool_calls IS '工具调用信息，JSON格式存储调用的工具及参数';
COMMENT ON COLUMN chat_message.tool_results IS '工具返回结果，JSON格式存储执行结果';
COMMENT ON COLUMN chat_message.tokens_used IS '本次消息消耗的token数量';
COMMENT ON COLUMN chat_message.latency_ms IS 'AI响应延迟，单位毫秒';
COMMENT ON COLUMN chat_message.is_summarized IS '是否已计入会话摘要，false表示未摘要';
COMMENT ON COLUMN chat_message.created_at IS '创建时间';

CREATE INDEX idx_message_session ON chat_message (session_id, created_at DESC);
CREATE INDEX idx_message_unsummarized ON chat_message (session_id, is_summarized) WHERE is_summarized = FALSE;
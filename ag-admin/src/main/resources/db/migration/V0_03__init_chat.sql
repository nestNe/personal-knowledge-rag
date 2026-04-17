CREATE TABLE spring_ai_chat_session
(
    id                BIGSERIAL PRIMARY KEY,
    session_id        VARCHAR(64) NOT NULL UNIQUE,
    user_id           BIGINT      NOT NULL,
    title             VARCHAR(255) DEFAULT '新对话',
    summary           TEXT, -- AI 自动生成的会话摘要
    model_type        VARCHAR(50)  DEFAULT 'kb',
    is_active         BOOLEAN      DEFAULT TRUE,
    message_count     INTEGER      DEFAULT 0,
    last_message_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    created_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_session_id UNIQUE (session_id)
);

-- 添加字段注释
COMMENT ON TABLE spring_ai_chat_session IS 'AI对话会话表';
COMMENT ON COLUMN spring_ai_chat_session.id IS '主键，自增';
COMMENT ON COLUMN spring_ai_chat_session.session_id IS 'Spring AI 使用的会话ID，全局唯一';
COMMENT ON COLUMN spring_ai_chat_session.user_id IS '用户ID，关联用户表';
COMMENT ON COLUMN spring_ai_chat_session.title IS '会话标题，可手动修改或由首条消息生成';
COMMENT ON COLUMN spring_ai_chat_session.summary IS 'AI 自动生成的会话摘要，记录核心讨论内容';
COMMENT ON COLUMN spring_ai_chat_session.model_type IS '模型类型：kb(知识库)/qa(问答)等';
COMMENT ON COLUMN spring_ai_chat_session.is_active IS '是否活跃：true正常 false已删除';
COMMENT ON COLUMN spring_ai_chat_session.message_count IS '消息条数统计，冗余字段用于快速展示';
COMMENT ON COLUMN spring_ai_chat_session.last_message_time IS '最后消息时间，用于排序';
COMMENT ON COLUMN spring_ai_chat_session.created_at IS '创建时间';
COMMENT ON COLUMN spring_ai_chat_session.updated_at IS '更新时间';

-- 创建索引
CREATE INDEX idx_chat_session_user_id ON spring_ai_chat_session (user_id);
CREATE INDEX idx_chat_session_last_time ON spring_ai_chat_session (last_message_time);
CREATE INDEX idx_chat_session_user_active ON spring_ai_chat_session (user_id, is_active, last_message_time DESC);

-- 自动更新 updated_at 的触发器
CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_chat_session_updated_at
    BEFORE UPDATE
    ON spring_ai_chat_session
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
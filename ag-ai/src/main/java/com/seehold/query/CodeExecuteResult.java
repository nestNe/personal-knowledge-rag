package com.seehold.query;

import lombok.Builder;
import lombok.Data;

/**
 * 代码执行结果，返回给 AI 模型进行解读
 */
@Data
@Builder
public class CodeExecuteResult {

    /** 是否执行成功 */
    private boolean success;

    /** 标准输出内容（代码的实际运行结果） */
    private String stdout;

    /** 标准错误输出（执行失败时的错误信息） */
    private String stderr;

    /** 退出码，0 表示正常 */
    private int exitCode;

    /** 实际执行耗时（毫秒） */
    private long costTimeMs;

    /** 执行的语言 */
    private String language;

    /** 失败原因描述（超时/不支持的语言/安全拦截等） */
    private String failReason;

}

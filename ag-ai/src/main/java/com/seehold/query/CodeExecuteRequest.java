package com.seehold.query;

import lombok.Data;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * AI 代码执行工具的请求参数
 * 由模型根据用户问题自动生成并填充
 */
@Data
public class CodeExecuteRequest {

    @ToolParam(description = """
            要执行的编程语言，支持：python、javascript（nodejs）、shell（bash/sh）。
            根据任务特点选择最合适的语言：
            - 数学计算、数据处理 → python
            - 时间/日期/系统信息 → python 或 shell
            - 字符串处理、JSON → python 或 javascript
            - 系统命令、文件操作 → shell
            """)
    private String language;

    @ToolParam(description = """
            要执行的完整代码字符串。注意：
            1. 代码必须能够独立运行，不依赖外部文件或未安装的第三方库
            2. 将最终结果用 print() / console.log() 输出到标准输出
            3. 代码应简洁，只完成目标任务
            4. 禁止执行危险操作（删除文件、网络请求等）
            """)
    private String code;

    @ToolParam(description = "本次代码执行的任务描述，用于日志记录，例如：'查询当前系统时间'", required = false)
    private String taskDescription;

    @ToolParam(description = "代码执行超时秒数，默认10秒，最大30秒", required = false)
    private Integer timeoutSeconds;

}

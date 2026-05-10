package com.seehold.tools;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 经理模型的委派工具 — 作为 Manager ChatClient 的 @Tool，
 * 根据任务类型将工作分发给不同的 Worker ChatClient。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrchestratorTools {

    private final ChatClient userManageClient;
    private final ChatClient kbClient;
    private final ChatClient codeClient;

    @Tool(description = """
            委派给「用户管理」Worker：处理用户/角色/权限等数据查询。

            适用场景举例：
            - 查询用户列表、用户信息、角色、权限
            - 统计用户数量
            - 用户状态查询
            """)
    public String delegateToUserManage(
            @ToolParam(description = "当前用户ID，从系统上下文获取") Long userId,
            @ToolParam(description = "用自然语言描述的工作任务，Worker 会根据任务内容自主决定调用哪些工具") String task) {
        log.info("[Orchestrator] 委派用户管理任务 (userId={}): {}", userId, task);
        return userManageClient.prompt()
                .system("当前用户ID: " + userId)
                .user(task)
                .call()
                .content();
    }

    @Tool(description = """
            委派给「知识库检索」Worker：搜索用户个人知识库中保存的内容。

            当问及"我/我的/之前/记过/存过/项目/配置"等个人相关内容时，优先使用此工具。
            Worker 会通过向量相似度搜索，返回匹配度最高的知识条目。
            """)
    public String delegateToKb(
            @ToolParam(description = "当前用户ID，从系统上下文获取") Long userId,
            @ToolParam(description = "用自然语言描述要检索的内容，关键词形式（如 'Steam 账号'、'Redis 配置'）") String task) {
        log.info("[Orchestrator] 委派知识库任务 (userId={}): {}", userId, task);
        return kbClient.prompt()
                .system("当前用户ID: " + userId)
                .user(task)
                .call()
                .content();
    }

    @Tool(description = """
            委派给「代码执行」Worker：通过编写并运行代码来精确回答需要计算或实时信息的问题。

            适用场景举例：
            - 时间/日期查询（当前时间、星期几、时区换算）
            - 数学计算（圆周率、大数阶乘、复杂公式）
            - 系统信息（操作系统、环境变量）
            - 编码解码、单位换算、字符串处理

            Worker 自动选择最佳编程语言（Python/JavaScript/Shell），生成并执行代码。
            Worker 有安全黑名单，会拒绝执行任何危险操作。
            """)
    public String delegateToCode(
            @ToolParam(description = "用自然语言描述的计算任务，Worker 会自行生成代码完成") String task) {
        log.info("[Orchestrator] 委派代码执行任务: {}", task);
        return codeClient.prompt()
                .user(task)
                .call()
                .content();
    }
}

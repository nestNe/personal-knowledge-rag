package com.seehold.tools;

import com.seehold.query.CodeExecuteRequest;
import com.seehold.query.CodeExecuteResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;

/**
 * 通用代码执行工具（AI Tool）
 *
 * <p>让 AI 模型能够自主生成代码并在服务端沙箱中执行，
 * 将 stdout 结果返回给模型，由模型将执行结果转换为自然语言回答用户。</p>
 *
 * <h3>支持的语言</h3>
 * <ul>
 *   <li>python  — 适合数学计算、时间处理、数据转换</li>
 *   <li>javascript — 通过 Node.js 运行</li>
 *   <li>shell   — bash/sh，适合系统信息查询</li>
 * </ul>
 *
 * <h3>安全策略</h3>
 * <ul>
 *   <li>关键词黑名单拦截（rm、del、format、os.system 等危险调用）</li>
 *   <li>超时强制终止进程（默认 10 秒）</li>
 *   <li>代码写入临时文件，执行后立即删除</li>
 *   <li>stdout/stderr 输出截断（最大 4096 字符）</li>
 * </ul>
 */
@Slf4j
@Component
public class CodeExecutorTools {

    /** 默认超时秒数 */
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;

    /** 最大允许超时秒数 */
    private static final int MAX_TIMEOUT_SECONDS = 30;

    /** 输出最大字符数，防止模型 token 爆炸 */
    private static final int MAX_OUTPUT_LENGTH = 4096;

    /**
     * 危险关键词黑名单（粗粒度安全防护）
     * 生产环境建议结合沙箱容器（如 Docker）做更严格的隔离
     */
    private static final List<String> BLACKLIST_KEYWORDS = List.of(
            // 文件删除
            "rm ", "rmdir", "del ", "shutil.rmtree", "os.remove", "os.unlink",
            "Files.delete", "file.delete",
            // 格式化/系统破坏
            "format ", "mkfs", "dd if=",
            // 网络请求（可按需开放）
            "urllib", "requests.get", "requests.post", "fetch(", "axios",
            "http.get(", "http.post(",
            // 进程/命令注入
            "subprocess", "os.system", "exec(", "eval(",
            "Runtime.getRuntime", "ProcessBuilder",
            // Shell 危险命令
            "shutdown", "reboot", "halt", "init 0",
            // 写入系统目录
            "/etc/", "/sys/", "/proc/", "C:\\Windows", "C:\\System"
    );

    @Tool(description = """
            通用代码执行器：根据用户的问题，自动选择合适的编程语言，生成并执行代码，
            将代码的标准输出结果返回，用于回答需要实时计算或系统查询的问题。
            
            适用场景举例：
            - 查询当前时间、日期、星期
            - 数学计算（圆周率、斐波那契数列、质数判断等）
            - 进制转换、编码解码（Base64、URL编码）
            - 字符串处理（统计字数、正则匹配）
            - 系统信息查询（操作系统、CPU架构）
            - 单位换算（时间、距离、温度）
            
            注意：不会执行涉及文件删除、网络请求、系统破坏的代码。
            """)
    public CodeExecuteResult executeCode(CodeExecuteRequest request) {
        String language = request.getLanguage();
        String code = request.getCode();
        String taskDesc = request.getTaskDescription() != null ? request.getTaskDescription() : "未描述";
        int timeout = resolveTimeout(request.getTimeoutSeconds());

        log.info("[CodeExecutor] 任务: {}, 语言: {}, 超时: {}s", taskDesc, language, timeout);
        log.debug("[CodeExecutor] 代码内容:\n{}", code);

        // 1. 安全检查
        String blockReason = checkSecurity(code, language);
        if (blockReason != null) {
            log.warn("[CodeExecutor] 安全拦截: {}", blockReason);
            return CodeExecuteResult.builder()
                    .success(false)
                    .exitCode(-1)
                    .failReason("安全拦截: " + blockReason)
                    .language(language)
                    .costTimeMs(0)
                    .build();
        }

        // 2. 获取运行命令
        String[] command = buildCommand(language, code);
        if (command == null) {
            return CodeExecuteResult.builder()
                    .success(false)
                    .exitCode(-1)
                    .failReason("不支持的语言: " + language + "，目前支持 python / javascript / shell")
                    .language(language)
                    .costTimeMs(0)
                    .build();
        }

        // 3. 执行代码
        return doExecute(command, language, timeout);
    }

    // ───────────────────── 内部方法 ─────────────────────

    /**
     * 根据语言类型构建执行命令数组
     *
     * <p>Python / JavaScript 将代码写入临时文件后执行；
     * Shell 直接以 -c 参数传入（避免临时文件权限问题）。</p>
     */
    private String[] buildCommand(String language, String code) {
        if (language == null) return null;

        return switch (language.toLowerCase().trim()) {
            case "python", "python3" -> buildTempFileCommand("py", code, "python3", "python");
            case "javascript", "js", "node", "nodejs" -> buildTempFileCommand("js", code, "node");
            case "shell", "bash", "sh" -> new String[]{"sh", "-c", code};
            default -> null;
        };
    }

    /**
     * 将代码写入临时文件，返回 [interpreter, tempFilePath] 形式的命令
     * 支持多个解释器名称备选（如 python3 / python）
     */
    private String[] buildTempFileCommand(String suffix, String code, String... interpreters) {
        try {
            Path tempFile = Files.createTempFile("ag_exec_", "." + suffix);
            Files.writeString(tempFile, code, StandardCharsets.UTF_8);
            // 找到第一个可用的解释器
            String interpreter = resolveInterpreter(interpreters);
            // 注册 JVM 退出时删除临时文件（双保险）
            tempFile.toFile().deleteOnExit();
            return new String[]{interpreter, tempFile.toAbsolutePath().toString()};
        } catch (IOException e) {
            log.error("[CodeExecutor] 创建临时文件失败", e);
            return null;
        }
    }

    /**
     * 从候选解释器列表中返回第一个（简单处理，实际环境按需调整）
     */
    private String resolveInterpreter(String... candidates) {
        // 尝试找到 PATH 中存在的解释器
        for (String candidate : candidates) {
            try {
                Process p = new ProcessBuilder(candidate, "--version")
                        .redirectErrorStream(true)
                        .start();
                p.waitFor(2, TimeUnit.SECONDS);
                if (p.exitValue() == 0 || p.exitValue() == 1) {
                    return candidate;
                }
            } catch (Exception ignored) {
                // 该解释器不存在，尝试下一个
            }
        }
        return candidates[0]; // 都失败时返回第一个，让执行阶段报告真实错误
    }

    /**
     * 真正执行进程，收集 stdout/stderr，强制超时终止
     */
    private CodeExecuteResult doExecute(String[] command, String language, int timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        Path tempFilePath = command.length == 2 ? Path.of(command[1]) : null;

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false); // 分别读取 stdout 和 stderr
            Process process = pb.start();

            // 异步读取输出，防止缓冲区死锁
            ExecutorService ioPool = Executors.newFixedThreadPool(2);
            Future<String> stdoutFuture = ioPool.submit(
                    () -> readStream(process.getInputStream()));
            Future<String> stderrFuture = ioPool.submit(
                    () -> readStream(process.getErrorStream()));

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                // 超时，强制杀进程
                process.destroyForcibly();
                ioPool.shutdownNow();
                long cost = System.currentTimeMillis() - startTime;
                log.warn("[CodeExecutor] 执行超时 ({}s), 语言: {}", timeoutSeconds, language);
                return CodeExecuteResult.builder()
                        .success(false)
                        .exitCode(-1)
                        .failReason("执行超时（超过 " + timeoutSeconds + " 秒），请简化代码")
                        .language(language)
                        .costTimeMs(cost)
                        .build();
            }

            ioPool.shutdown();
            int exitCode = process.exitValue();
            String stdout = truncate(stdoutFuture.get());
            String stderr = truncate(stderrFuture.get());
            long cost = System.currentTimeMillis() - startTime;

            log.info("[CodeExecutor] 执行完成, exitCode={}, 耗时={}ms", exitCode, cost);
            if (!stderr.isBlank()) {
                log.debug("[CodeExecutor] stderr: {}", stderr);
            }

            return CodeExecuteResult.builder()
                    .success(exitCode == 0)
                    .stdout(stdout)
                    .stderr(stderr.isBlank() ? null : stderr)
                    .exitCode(exitCode)
                    .costTimeMs(cost)
                    .language(language)
                    .build();

        } catch (Exception e) {
            long cost = System.currentTimeMillis() - startTime;
            log.error("[CodeExecutor] 执行异常", e);
            return CodeExecuteResult.builder()
                    .success(false)
                    .exitCode(-1)
                    .stderr(e.getMessage())
                    .failReason("执行异常: " + e.getMessage())
                    .language(language)
                    .costTimeMs(cost)
                    .build();
        } finally {
            // 清理临时文件
            if (tempFilePath != null) {
                try {
                    Files.deleteIfExists(tempFilePath);
                } catch (IOException ignored) {}
            }
        }
    }

    /**
     * 安全检测：遍历黑名单关键词
     *
     * @return 若触发拦截返回原因字符串，安全则返回 null
     */
    private String checkSecurity(String code, String language) {
        if (code == null || code.isBlank()) {
            return "代码内容为空";
        }
        String lowerCode = code.toLowerCase();
        for (String keyword : BLACKLIST_KEYWORDS) {
            if (lowerCode.contains(keyword.toLowerCase())) {
                return "代码包含不允许的操作: [" + keyword.trim() + "]";
            }
        }
        return null;
    }

    /** 解析超时时间，限制在合理范围内 */
    private int resolveTimeout(Integer requested) {
        if (requested == null || requested <= 0) return DEFAULT_TIMEOUT_SECONDS;
        return Math.min(requested, MAX_TIMEOUT_SECONDS);
    }

    /** 读取输入流为字符串 */
    private String readStream(InputStream is) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString().stripTrailing();
        } catch (IOException e) {
            return "";
        }
    }

    /** 截断过长输出，防止 token 过多 */
    private String truncate(String text) {
        if (text == null) return "";
        if (text.length() <= MAX_OUTPUT_LENGTH) return text;
        return text.substring(0, MAX_OUTPUT_LENGTH) + "\n... [输出过长，已截断]";
    }
}

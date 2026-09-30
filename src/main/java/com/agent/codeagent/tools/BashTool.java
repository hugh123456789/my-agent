package com.agent.codeagent.tools;

import com.agent.codeagent.agents.tool.ToolDispatcher;
import com.agent.codeagent.agents.tool.ToolRegistrar;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.List;

/** Executes local shell commands with bounded lifetime and complete output capture. */
@Component
public class BashTool implements ToolRegistrar {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Duration timeout;

    public BashTool() {
        this(DEFAULT_TIMEOUT);
    }

    public BashTool(Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        this.timeout = timeout;
    }

    public String execute(String arguments) throws Exception {
        String command = parseCommand(arguments);
        Process process = start(command);
        ExecutorService readerExecutor = Executors.newSingleThreadExecutor();
        Future<String> output = readerExecutor.submit(() -> readOutput(process));
        try {
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                process.waitFor(1, TimeUnit.SECONDS);
                return output.get(1, TimeUnit.SECONDS)
                        + "\n[命令超时，已强制终止]";
            }
            return output.get(1, TimeUnit.SECONDS);
        } catch (ExecutionException error) {
            throw new IOException("读取命令输出失败", error.getCause());
        } finally {
            if (process.isAlive()) process.destroyForcibly();
            readerExecutor.shutdownNow();
        }
    }

    @Override
    public void register(ToolDispatcher dispatcher) {
        dispatcher.register(
                ToolSpecification.builder()
                        .name("bash")
                        .description(isWindows()
                                ? "在 Windows PowerShell 中执行一条本地命令，返回 stdout + stderr。优先使用只读命令。"
                                : "在本地 bash 终端执行一条命令，返回 stdout + stderr。优先使用只读命令。")
                        .parameters(JsonObjectSchema.builder()
                                .addProperty("command", JsonStringSchema.builder()
                                        .description("要执行的命令")
                                        .build())
                                .required(List.of("command"))
                                .build())
                        .build(),
                (sessionId, toolName, arguments) -> execute(arguments));
    }

    String parseCommand(String arguments) throws IOException {
        if (arguments == null || arguments.isBlank()) {
            throw new IllegalArgumentException("bash 工具需要 command 参数");
        }
        String trimmed = arguments.trim();
        if (!trimmed.startsWith("{")) return arguments;
        JsonNode command = objectMapper.readTree(trimmed).get("command");
        if (command == null || !command.isTextual() || command.asText().isBlank()) {
            throw new IllegalArgumentException("bash 工具需要字符串参数 command");
        }
        return command.asText();
    }

    private Process start(String command) throws IOException {
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return new ProcessBuilder("powershell.exe", "-NoLogo", "-NoProfile",
                    "-NonInteractive", "-Command",
                    "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; " + command)
                    .redirectErrorStream(true)
                    .start();
        }
        return new ProcessBuilder("bash", "-lc", command)
                .redirectErrorStream(true)
                .start();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private String readOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        }
        return output.toString();
    }
}

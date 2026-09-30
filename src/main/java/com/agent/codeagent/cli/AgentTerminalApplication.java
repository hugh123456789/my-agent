package com.agent.codeagent.cli;

import com.agent.codeagent.agents.api.AgentEvent;
import com.agent.codeagent.agents.api.AgentFacade;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import reactor.core.Disposable;

/** Interactive terminal client running in the same JVM as the Agent. */
public final class AgentTerminalApplication {

    private static final String EXIT_COMMAND = "/exit";

    private final AgentFacade agentFacade;
    private final BufferedReader input;
    private final PrintWriter output;
    private String sessionId;
    private boolean thinkingOpen;

    public AgentTerminalApplication(AgentFacade agentFacade,
                                    BufferedReader input,
                                    PrintWriter output) {
        this.agentFacade = agentFacade;
        this.input = input;
        this.output = output;
    }

    public static BufferedReader systemInput() {
        return new BufferedReader(new InputStreamReader(System.in, consoleCharset()));
    }

    public static PrintWriter systemOutput() {
        return new PrintWriter(System.out, true, consoleCharset());
    }

    private static Charset consoleCharset() {
        if (System.console() != null) return System.console().charset();
        return Charset.forName(System.getProperty("native.encoding", StandardCharsets.UTF_8.name()));
    }

    public void run() throws IOException, InterruptedException {
        output.println("Agent CLI 已启动（本地模式）");
        output.println("输入 /exit 退出。");
        while (true) {
            output.print("Agent> ");
            output.flush();
            String message = input.readLine();
            if (message == null || isExitCommand(message)) {
                output.println("已退出。");
                return;
            }
            if (message.isBlank()) continue;
            stream(message);
        }
    }

    static boolean isExitCommand(String input) {
        return EXIT_COMMAND.equals(input == null ? null : input.trim());
    }

    private void stream(String message) throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Disposable> current = new AtomicReference<>();
        current.set(agentFacade.agentLoop(sessionId, message).subscribe(
                event -> handle(event, done, current),
                error -> {
                    output.println("\n[错误] " + error.getMessage());
                    done.countDown();
                }));
        done.await();
    }

    private void handle(AgentEvent event,
                        CountDownLatch done,
                        AtomicReference<Disposable> current) {
        if (event instanceof AgentEvent.PermissionRequired required) {
            approve(required, done, current);
            return;
        }
        render(event);
        if (event instanceof AgentEvent.Complete || event instanceof AgentEvent.Error) {
            done.countDown();
        }
    }

    private void approve(AgentEvent.PermissionRequired event,
                         CountDownLatch done,
                         AtomicReference<Disposable> current) {
        finishThinkingBlock();
        output.println("\n[需要审批] " + event.reason());
        output.println("工具: " + event.toolName());
        output.print("允许执行？(y/N) ");
        output.flush();
        try {
            String answer = input.readLine();
            boolean approved = answer != null && ("y".equalsIgnoreCase(answer.trim())
                    || "yes".equalsIgnoreCase(answer.trim()));
            current.set(agentFacade.approveTool(event.requestId(), approved).subscribe(
                    approvalEvent -> handle(approvalEvent, done, current),
                    error -> {
                        output.println("\n[错误] " + error.getMessage());
                        done.countDown();
                    }));
        } catch (IOException error) {
            output.println("\n[输入错误] " + error.getMessage());
            done.countDown();
        }
    }

    private void render(AgentEvent event) {
        switch (event.type()) {
            case "session" -> sessionId = ((AgentEvent.SessionStarted) event).sessionId();
            case "text" -> {
                finishThinkingBlock();
                output.print(((AgentEvent.TextDelta) event).text());
                output.flush();
            }
            case "thinking" -> {
                if (!thinkingOpen) {
                    output.print("\n<thinking>");
                    thinkingOpen = true;
                }
                output.print(((AgentEvent.ThinkingDelta) event).text());
                output.flush();
            }
            case "tool_call" -> {
                finishThinkingBlock();
                AgentEvent.ToolCallDelta call = (AgentEvent.ToolCallDelta) event;
                output.println("\n[工具] " + call.name() + " " + call.arguments());
            }
            case "tool_result" -> {
                finishThinkingBlock();
                AgentEvent.ToolResultDelta result = (AgentEvent.ToolResultDelta) event;
                output.println("\n[工具结果] " + result.content());
            }
            case "permission_denied" -> {
                finishThinkingBlock();
                output.println("\n[已拒绝] " + ((AgentEvent.PermissionDenied) event).reason());
            }
            case "error" -> {
                finishThinkingBlock();
                output.println("\n[错误] " + ((AgentEvent.Error) event).error().getMessage());
            }
            case "complete" -> {
                finishThinkingBlock();
                output.println();
            }
            case "reminder" -> {
                finishThinkingBlock();
                output.println("\n[提醒] " + ((AgentEvent.Reminder) event).text());
            }
            default -> { }
        }
    }

    private void finishThinkingBlock() {
        if (!thinkingOpen) return;
        output.print("</thinking>\n");
        output.flush();
        thinkingOpen = false;
    }
}

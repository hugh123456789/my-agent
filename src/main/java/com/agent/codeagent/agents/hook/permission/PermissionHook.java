package com.agent.codeagent.agents.hook.permission;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import com.agent.codeagent.agents.hook.core.HookContext;
import com.agent.codeagent.agents.hook.core.HookEvent;
import com.agent.codeagent.agents.hook.core.HookRegistrar;
import com.agent.codeagent.agents.hook.core.HookRegistry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

/** 工具执行前的拒绝、审批和放行策略。 */
@Component
public class PermissionHook implements HookRegistrar {

    private static final List<String> DENY_LIST = List.of(
            "rm -rf /", "sudo", "shutdown", "reboot", "mkfs", "dd if=", "> /dev/sda");

    private static final List<ApprovalRule> APPROVAL_RULES = List.of(
            new ApprovalRule("bash", args -> {
                String value = args.toLowerCase();
                return (value.contains(" > ") || value.contains(">>")
                        || value.contains("mkdir") || value.contains("touch"))
                        && !value.startsWith("ls ")
                        && !value.startsWith("cat ")
                        && !value.startsWith("grep ");
            }, "写文件或修改文件系统操作"),
            new ApprovalRule("bash", args -> {
                String value = args.toLowerCase();
                return value.startsWith("rm ") || value.startsWith("mv ") || value.startsWith("cp -r");
            }, "删除或移动文件操作"),
            new ApprovalRule("bash", args -> {
                String value = args.toLowerCase();
                return value.contains("apt ") || value.contains("yum ")
                        || value.contains("pip install") || value.contains("npm install -g")
                        || value.contains("brew ");
            }, "安装软件包操作"));

    @Override
    public String name() {
        return "permission_hook";
    }

    @Override
    public void register(HookRegistry registry) {
        registry.registerHook(HookEvent.PRE_TOOL_USE, this::check);
    }

    private Object check(HookContext context) {
        if (context.output() instanceof PermissionResult result
                && !(result instanceof PermissionResult.Allowed)) {
            return result;
        }
        if (!(context.block() instanceof ToolExecutionRequest request)) {
            return context.output();
        }

        String toolName = request.name();
        String arguments = request.arguments();
        if ("bash".equals(toolName) && arguments != null) {
            for (String pattern : DENY_LIST) {
                if (arguments.contains(pattern)) {
                    return new PermissionResult.Denied("危险命令已被拒绝: " + pattern);
                }
            }
            for (ApprovalRule rule : APPROVAL_RULES) {
                if (rule.toolName.equals(toolName) && rule.matcher.test(arguments)) {
                    return new PermissionResult.AskUser(rule.reason);
                }
            }
        }
        return new PermissionResult.Allowed();
    }

    private record ApprovalRule(String toolName, Predicate<String> matcher, String reason) {}
}

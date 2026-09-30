package com.agent.codeagent.common;

import com.agent.codeagent.agents.approval.ApprovalManager;
import com.agent.codeagent.agents.conversation.ConversationRepository;
import com.agent.codeagent.agents.conversation.JsonlConversationStore;
import com.agent.codeagent.agents.execution.AgentLoopExecutor;
import com.agent.codeagent.agents.execution.SubAgentRunner;
import com.agent.codeagent.agents.tools.ToolCallProcessor;
import com.agent.codeagent.agents.hook.core.HookRegistry;
import com.agent.codeagent.agents.tool.ToolDispatcher;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.nio.file.Paths;

/** Wires Agent runtime collaborators independently from tool definitions. */
@Configuration
public class AgentRuntimeConfig {

    private static final long APPROVAL_TIMEOUT_MILLIS = 5 * 60 * 1000L;

    @Bean
    public ConversationRepository conversationRepository(
            @Value("${agent.sessions.directory:./sessions}") String directory) {
        return new JsonlConversationStore(Paths.get(directory));
    }

    @Bean
    public SubAgentRunner subAgentRunner(
            @Lazy @Qualifier("mainChatModel") ChatModel model,
            @Qualifier("subAgentToolDispatcher") ToolDispatcher childToolDispatcher,
            HookRegistry hookRegistry) {
        return new SubAgentRunner(
                model,
                childToolDispatcher,
                new ToolCallProcessor(childToolDispatcher, hookRegistry),
                hookRegistry);
    }

    @Bean
    public ToolCallProcessor mainToolCallProcessor(
            @Qualifier("mainToolDispatcher") ToolDispatcher dispatcher,
            HookRegistry hookRegistry) {
        return new ToolCallProcessor(dispatcher, hookRegistry);
    }

    @Bean
    public ApprovalManager approvalManager(@Lazy AgentLoopExecutor executor) {
        return new ApprovalManager(APPROVAL_TIMEOUT_MILLIS, executor::expireApproval);
    }
}

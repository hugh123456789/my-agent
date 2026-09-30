package com.agent.codeagent.agents.approval;

import com.agent.codeagent.agents.api.AgentEvent;
import com.agent.codeagent.agents.conversation.ConversationContext;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ChatMessage;
import reactor.core.publisher.FluxSink;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Owns suspended tool approvals and their lifecycle. */
public final class ApprovalManager {

    public record PendingApproval(
            ConversationContext context,
            List<ToolExecutionRequest> remaining,
            Object continuationState,
            FluxSink<AgentEvent> sink,
            long expiresAt) {
    }

    private final long timeoutMillis;
    private final Consumer<PendingApproval> onExpired;
    private final Map<String, PendingApproval> pending = new ConcurrentHashMap<>();

    public ApprovalManager(long timeoutMillis) {
        this(timeoutMillis, ignored -> { });
    }

    public ApprovalManager(long timeoutMillis, Consumer<PendingApproval> onExpired) {
        if (timeoutMillis <= 0) throw new IllegalArgumentException("timeoutMillis must be positive");
        this.timeoutMillis = timeoutMillis;
        this.onExpired = onExpired;
    }

    public String suspend(ConversationContext context,
                          List<ToolExecutionRequest> remaining,
                          Object continuationState,
                          FluxSink<AgentEvent> sink) {
        expirePending();
        String requestId = UUID.randomUUID().toString();
        pending.put(requestId, new PendingApproval(
                context,
                new ArrayList<>(remaining),
                continuationState,
                sink,
                System.currentTimeMillis() + timeoutMillis));
        return requestId;
    }

    public PendingApproval take(String requestId) {
        expirePending();
        PendingApproval approval = pending.remove(requestId);
        if (approval == null || approval.expiresAt() < System.currentTimeMillis()) {
            return null;
        }
        return approval;
    }

    public void expirePending() {
        long now = System.currentTimeMillis();
        pending.entrySet().removeIf(entry -> {
            if (entry.getValue().expiresAt() >= now) return false;
            onExpired.accept(entry.getValue());
            return true;
        });
    }

    public void removeForSink(FluxSink<AgentEvent> sink) {
        pending.entrySet().removeIf(entry -> entry.getValue().sink() == sink);
    }
}

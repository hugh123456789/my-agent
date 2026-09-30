package com.agent.codeagent.tools;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/** Owns Todo state and its stable text representations. */
@Component
public final class TodoStore {

    private static final Set<String> VALID_STATUSES =
            Set.of("pending", "in_progress", "completed");
    private static final String DEFAULT_SESSION_ID = "default";
    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

    public SessionState session(String sessionId) {
        String key = sessionId == null || sessionId.isBlank() ? DEFAULT_SESSION_ID : sessionId;
        return sessions.computeIfAbsent(key, ignored -> new SessionState());
    }

    public void removeSession(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            sessions.remove(sessionId);
        }
    }

    public void upsert(String title, String status) {
        session(DEFAULT_SESSION_ID).upsert(title, status);
    }

    public boolean isEmpty() {
        return session(DEFAULT_SESSION_ID).isEmpty();
    }

    public String formatTaskList() {
        return session(DEFAULT_SESSION_ID).formatTaskList();
    }

    public String formatReminder() {
        return session(DEFAULT_SESSION_ID).formatReminder();
    }

    public static final class SessionState {
        private final Map<String, Task> tasks = new ConcurrentHashMap<>();
        private final AtomicLong sequence = new AtomicLong();
        private final AtomicInteger toolCallsSinceWrite = new AtomicInteger();

        public void recordToolCall(boolean todoWrite) {
            if (todoWrite) toolCallsSinceWrite.set(0);
            else toolCallsSinceWrite.incrementAndGet();
        }

        public boolean consumeReminderIfDue(int threshold) {
            if (toolCallsSinceWrite.get() < threshold || isEmpty()) return false;
            toolCallsSinceWrite.set(0);
            return true;
        }

        public void resetReminder() {
            toolCallsSinceWrite.set(0);
        }

        public void upsert(String title, String status) {
        if (title == null || title.isBlank()) return;
        if (status == null || !VALID_STATUSES.contains(status)) {
            throw new IllegalArgumentException("非法状态 '" + status
                    + "'，仅允许 pending / in_progress / completed");
        }
        tasks.compute(title, (key, existing) -> existing == null
                ? new Task(sequence.incrementAndGet(), title, status)
                : new Task(existing.sequence(), title, status));
    }

        public boolean isEmpty() {
            return tasks.isEmpty();
        }

        public String formatTaskList() {
        if (tasks.isEmpty()) return "任务清单为空。";
        StringBuilder output = new StringBuilder("当前任务清单（共 ")
                .append(tasks.size()).append(" 项）：");
        for (Task task : sorted()) {
            String mark = switch (task.status()) {
                case "pending" -> "[ ]";
                case "in_progress" -> "[>]";
                case "completed" -> "[x]";
                default -> "[?]";
            };
            output.append("\n  ").append(mark).append(" ")
                    .append(task.title()).append(" (").append(task.status()).append(")");
        }
        return output.toString();
    }

        public String formatReminder() {
        List<Task> sorted = sorted();
        if (sorted.isEmpty()) return null;
        String totalGoal = sorted.getFirst().title();
        String currentTodo = sorted.stream()
                .filter(task -> "in_progress".equals(task.status()))
                .map(Task::title)
                .findFirst()
                .orElse("暂无，请从待完成任务中选择一个开始");

        StringBuilder output = new StringBuilder()
                .append("总目标：").append(totalGoal)
                .append("\n\n当前 Todo：").append(currentTodo)
                .append("\n\n已完成：\n\n");
        appendTasks(output, sorted, "completed", "- 暂无");
        output.append("\n还没完成：\n\n");
        appendTasks(output, sorted, "pending", "- 暂无");
        return output.append("\n要求：\n继续当前 Todo。").toString();
    }

        private List<Task> sorted() {
        return tasks.values().stream()
                .sorted(Comparator.comparingLong(Task::sequence))
                .toList();
    }

        private void appendTasks(StringBuilder output, List<Task> tasks,
                             String status, String emptyText) {
        boolean found = false;
        for (Task task : tasks) {
            if (status.equals(task.status())) {
                output.append("- ").append(task.title()).append("\n");
                found = true;
            }
        }
        if (!found) output.append(emptyText).append("\n");
    }

    }

    public record Task(long sequence, String title, String status) {}
}

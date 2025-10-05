package com.github.jiangwangyang.ai.common.a2a;

import com.github.jiangwangyang.ai.common.agent.BaseAgent;
import io.a2a.A2A;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DefaultAgentExecutor implements AgentExecutor {

    public static final String STREAMING_METADATA_KEY = "isStreaming";

    private static final Logger log = LoggerFactory.getLogger(DefaultAgentExecutor.class);

    private final BaseAgent agent;

    public DefaultAgentExecutor(BaseAgent agent) {
        this.agent = agent;
    }

    @Override
    public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        try {
            Message message = context.getParams().message();
            StringBuilder sb = new StringBuilder();
            for (Part<?> each : message.getParts()) {
                if (Part.Kind.TEXT.equals(each.getKind())) {
                    sb.append(((TextPart) each).getText()).append("\n");
                }
            }
            String messageContent = sb.toString().trim();
            executeTask(messageContent, context, eventQueue, isStreamRequest(context));
        } catch (Exception e) {
            log.error("Agent execution failed", e);
            eventQueue.enqueueEvent(A2A.toAgentMessage("Agent execution failed: " + e.getMessage()));
        }
    }

    @Override
    public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
    }

    private void executeTask(String messageContent, RequestContext context, EventQueue eventQueue, boolean isStreaming) {
        Task task = context.getTask();
        if (task == null) {
            task = newTask(context.getMessage());
            eventQueue.enqueueEvent(task);
        }
        TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
        taskUpdater.submit();
        Consumer<String> sendUpdateEvent = msg -> eventQueue.enqueueEvent(
                new TaskStatusUpdateEvent(
                        context.getTaskId(),
                        new TaskStatus(TaskState.WORKING, A2A.toAgentMessage(msg), LocalDateTime.now()),
                        context.getContextId(),
                        false,
                        Collections.emptyMap())
        );
        Consumer<String> doNothing = msg -> {
        };
        Consumer<String> doOnNext = isStreaming ? sendUpdateEvent : doNothing;
        String result = agent.run(task.getContextId(), "remoteAgent", messageContent)
                .doOnNext(doOnNext)
                .toStream()
                .collect(Collectors.joining());
        taskUpdater.addArtifact(List.of(new TextPart(result)), UUID.randomUUID().toString(),
                "conversation_result", Collections.emptyMap());
        taskUpdater.complete();
    }

    private boolean isStreamRequest(RequestContext context) {
        MessageSendParams params = context.getParams();
        if (null == params.metadata()) {
            return false;
        }
        if (!params.metadata().containsKey(STREAMING_METADATA_KEY)) {
            return false;
        }
        return (boolean) params.metadata().get(STREAMING_METADATA_KEY);
    }

    private Task newTask(Message request) {
        String contextId = request.getContextId();
        if (contextId == null || contextId.isEmpty()) {
            contextId = UUID.randomUUID().toString();
        }
        String id = UUID.randomUUID().toString();
        if (request.getTaskId() != null && !request.getTaskId().isEmpty()) {
            id = request.getTaskId();
        }
        return new Task(id, contextId, new TaskStatus(TaskState.SUBMITTED), null, List.of(request), null);
    }

}

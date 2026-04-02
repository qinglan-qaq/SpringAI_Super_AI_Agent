package com.lx.aisuperagent.agent;

import com.lx.aisuperagent.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import opennlp.tools.util.StringUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 使用抽象类作为基础的代理类
 * 用于管理代理状态和执行流程
 * 提供基础功能:
 * 状态转换, 内存管理, 执行循环
 *
 */

@Data
@Slf4j
public abstract class BaseAgent {

    //    核心属性
    private String name;

    //    系统提示词和下一步提示词
    private String systemPrompt;
    private String nextStepPrompt;

    //    状态默认为空闲
    private AgentState state = AgentState.IDLE;

    //    最大循环步骤, 当前步骤
    private int maxSteps = 10;
    private int currentStep = 0;

    //    LLM
    private ChatClient chatClient;

    //    自主维护的上下文对话记忆
    private List<Message> messageList = new ArrayList<>();

    /**
     * 执行单个步骤
     *
     * @return
     */
    public abstract String step();

    /**
     * 完成步骤后清理资源
     * 子类重写该方法
     */
    protected void cleanup() {

    }

    public String run(String userPrompt) {

        //    合法性判断
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("cannot run agent from state:" + state);
        }
        if (StringUtil.isEmpty(userPrompt)) {
            throw new RuntimeException("cannot run agent with empty user prompt");
        }

        //       更改状态
        state = AgentState.RUNNING;
        //      保存上下文
        messageList.add(new UserMessage(userPrompt));
        //        保存结果
        List<String> results = new ArrayList<>();

        try {
            //      逐步循环执行步骤,代码完整性
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Executing step" + stepNumber + "\n");
                String stepResult = step();
                //      保存当前第几步和对应的执行结果
                String result = "Step " + stepNumber + ": " + stepResult;
                results.add(result);
            }
            if (currentStep > maxSteps) {
                state = AgentState.FINISHED;
                results.add("Terminated: Reached max steps (" + maxSteps + ")");
            }
            return String.join("\n", results);
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("Error executing agent", e);
            return "执行错误" + e.getMessage();
        } finally {
            //  清理资源
            this.cleanup();
        }

    }

    /**
     * 使用emitter发送代理(Agent)执行步骤 结果 信息
     * 对Run的emitter改造
     *
     * @param userPrompt
     * @return
     */
    public SseEmitter runStream(String userPrompt) {
        //      设置超时时间
        SseEmitter emitter = new SseEmitter(300000L);

        CompletableFuture.runAsync(() -> {
            try {
                if (this.state != AgentState.IDLE) {
                    emitter.send("错误, 无法从该状态执行代理: " + this.state);
                    emitter.complete();
                    return;
                }
                if (StringUtil.isEmpty(userPrompt)) {
                    emitter.send("错误, 不能使用空提示词执行代理");
                    emitter.complete();
                    return;
                }
                //  更改状态
                state = AgentState.RUNNING;
                //  添加用户提示词
                messageList.add(new UserMessage(userPrompt));

                try {
                    for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                        int stepNumber = i + 1;
                        currentStep = stepNumber;
                        log.info("Executing step" + stepNumber + "\n");
                        String stepResult = step();
                        //      保存当前第几步和对应的执行结果
                        String result = "Step " + stepNumber + ": " + stepResult;
                        //      发送每一步的结果
                        emitter.send(result);
                    }
                    //      是否超出最大步骤
                    if (currentStep > maxSteps) {
                        state = AgentState.FINISHED;
                        emitter.send("Terminated: Reached max steps (" + maxSteps + ")");
                    }
                    emitter.complete();
                } catch (Exception e) {
                    state = AgentState.ERROR;
                    log.error("Error executing agent", e);
                    try {
                        emitter.send("执行错误: " + e.getMessage());
                        emitter.complete();
                    } catch (Exception exception) {

                        emitter.completeWithError(exception);
                    }
                } finally {
                    //  清理资源
                    this.cleanup();
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.cleanup();
            log.warn("SSE connection timed out");
        });


        emitter.onCompletion(() -> {
            if (this.state == AgentState.RUNNING) {
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
            log.info("SSE connection completed");
        });

        return emitter;
    }
}

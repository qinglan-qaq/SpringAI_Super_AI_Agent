package com.lx.aisuperagent.agent;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ReAct( Reasoning and Acting )模式代理抽象类
 * 实现了思考 + 行动的循环模式
 *
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class ReActAgent extends BaseAgent {

    /**
     * 执行单个步骤
     * 思考和执行(think and act)
     *
     * @return 步骤执行结果
     */
    @Override
    public String step() {
        try {

            boolean shouldAct = think();

            if (!shouldAct) {

                return "思考完成 - 无需行动";
            }
            return act();

        } catch (Exception e) {

            e.printStackTrace();

            return "步骤执行异常: " + e.getMessage();
        }
    }

    /**
     * 处理当前状态决定是否执行下一步操作
     *
     * @return 是否要执行, true执行 false不需要执行
     */
    public abstract boolean think();

    /**
     * 执行单个操作的决定
     *
     * @return 返回执行结果
     */
    public abstract String act();
}

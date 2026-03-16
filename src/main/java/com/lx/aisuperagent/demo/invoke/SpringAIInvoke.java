package com.lx.aisuperagent.demo.invoke;

import jakarta.annotation.Resource;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Spring ai 框架调用
 */
@Component
public class SpringAIInvoke implements CommandLineRunner {

    @Resource
//    注意这里是按照名称引入的模型
    private ChatModel dashscopeChatModel;

    @Override
    public void run(String... args) throws Exception {
        AssistantMessage output = dashscopeChatModel
                .call(new Prompt("以余华的口吻解释,在长时间艰辛的努力后仍然失败的原因,一百字以内"))
                .getResult().getOutput();
        System.out.println(output.getText());
    }
}

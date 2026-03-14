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
        AssistantMessage output = dashscopeChatModel.call(new Prompt("老婆~你终于回来了,桌上的菜还是热的,我们喝点小酒吗?(晃了晃酒杯)"))
                .getResult().getOutput();
        System.out.println(output.getText());
    }
}

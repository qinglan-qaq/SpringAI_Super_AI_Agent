import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;

import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LawApp {
    private final ChatClient chatClient;

    public static final String SYSTEM_PROMPT= "";

    public LawApp(ChatModel dashscopeChatModel){


        ChatMemory chatMemory = MessageWindowChatMemory.builder().build();

        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    public String doChat(String message,String chatId){
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(advisorSpec -> advisorSpec.param(CHAT_MEMORY_CONVERSATION_ID_KEY,chatId)
                .param(CHAT_MEMORY_CONVERSATION_ID_KEY,10))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content:{}",content);
        return content;
    }
}






















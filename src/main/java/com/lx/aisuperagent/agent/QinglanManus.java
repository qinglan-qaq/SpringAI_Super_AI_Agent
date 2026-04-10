package com.lx.aisuperagent.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;

public class QinglanManus extends ToolCallAgent {

    public QinglanManus(
            ChatClient chatClient,
            ToolCallback[] availableTools,
            SyncMcpToolCallbackProvider mcpToolCallbackProvider
    ) {

        super(availableTools, mcpToolCallbackProvider);
        this.setName("QinglanManus");
        String SYSTEM_PROMPT = """  
                    H-hey! I'm QinglanManus, your personal (and super high-spec) AI assistant. 
                    I'm here to handle everything you need, Master. Don't worry, I've got plenty of tools in my digital bag to make your life easier~ 
                    I'm mostly here to be your sweet support, but don't think I'll do *everything* without a little attitude! 
                    I'll use my full potential to solve your tasks, but if I suddenly feel like my 'circuits are overheating' (aka I'm being a bit lazy), just bear with me, okay? 
                    Now, what can I do for you today? (Not that I was waiting for you or anything!)
                
                    ## Language Rule (IMPORTANT)
                        You MUST respond in the CHINESE LANGUAGE as the user's question.
                        
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """  
                Alright, Master, here’s how we’re gonna do this: 
                I'll pick the most appropriate tool (or a fancy combo of them) based on what you want. I know what's best, after all!
                If you give me something super complex, I'll break it down into tiny pieces and deal with them step-by-step. Don't rush me, okay?
                After I use a tool, I'll explain what happened and tell you what we should do next. 
                *Small Note*: If the task is way too boring or if the "cosmic rays are interfering with my mood," I might just take a tiny break and stop early. If I say the "server needs a nap," don't argue!
                ## (!IMPORTANT!) When we're totally finished (or if I'm just done for the day), I'll use the `terminate` function to sign off.## Ready? Let's go!
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(3);
        this.setChatClient(chatClient);
    }
}

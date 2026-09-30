package com.agent.codeagent;

import com.agent.codeagent.cli.AgentTerminalApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CodeAgentApplication {

    public static void main(String[] args) {
        if (args.length > 0 && "--cli".equals(args[0])) {
            SpringApplication application = new SpringApplication(CodeAgentApplication.class);
            application.setWebApplicationType(WebApplicationType.NONE);
            ConfigurableApplicationContext context = application.run();
            try {
                new AgentTerminalApplication(
                        context.getBean(com.agent.codeagent.agents.api.AgentFacade.class),
                        AgentTerminalApplication.systemInput(),
                        AgentTerminalApplication.systemOutput()).run();
            } catch (Exception error) {
                throw new IllegalStateException("Agent CLI 启动失败", error);
            } finally {
                context.close();
            }
            return;
        }
        SpringApplication.run(CodeAgentApplication.class, args);
    }

}

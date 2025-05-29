package com.redcat.tutorials;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redcat.tutorials.mcptools.CallGraphToolService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ApiCallGraphMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiCallGraphMcpServerApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider toolCallbackProvider(CallGraphToolService superFelineService) {
        MethodToolCallbackProvider toolCallbackProvider = MethodToolCallbackProvider.builder()
                .toolObjects(superFelineService)
                .build();
        return toolCallbackProvider;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}

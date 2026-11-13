package com.example.supermercado;

import com.example.supermercado.service.SupermercadoToolsService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SupermercadoMcpApplication {

	public static void main(String[] args) {
		SpringApplication.run(SupermercadoMcpApplication.class, args);
	}

	/** Registra los metodos @Tool del servicio como herramientas del servidor MCP. */
	@Bean
	public ToolCallbackProvider supermercadoTools(SupermercadoToolsService tools) {
		return MethodToolCallbackProvider.builder()
				.toolObjects(tools)
				.build();
	}
}

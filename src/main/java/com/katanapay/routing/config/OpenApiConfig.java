package com.katanapay.routing.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentRoutingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("KatanaPay Payment Routing API")
                        .description("API for routing payment requests to appropriate payment providers")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("KatanaPay Support")
                                .email("support@katanapay.com")
                                .url("https://katanapay.com/support")))
                .servers(List.of(
                        new Server().url("/").description("Default Server URL")
                ));
    }
}
package com.musinsa.shop.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Musinsa Test Shop API")
                        .description("Musinsa 백엔드 과제용 Swagger 문서")
                        .version("v0.1.0")
                        .contact(new Contact()
                                .name("정지원")
                                .email("micool1030@gmail.com")))
                .servers(List.of(new Server().url("http://localhost:8080/api").description("Local API Server")));
    }
}

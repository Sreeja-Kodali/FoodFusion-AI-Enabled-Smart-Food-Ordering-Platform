package com.jeenu.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class,args);
    }
}

/**
 * From a terminal, enter the following command to start Keycloak:
 * Reference : https://www.keycloak.org/getting-started/getting-started-docker
 * docker run -p 8080:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:24.0.4 start-dev
 * This command starts Keycloak exposed on the local port 8080 and creates an initial admin user with the username admin and password admin.
 *
 * redirection URL : http://localhost:8080/login/oauth2/code/spring-boot-mircroservices-realm
 *
 * start Zipkin via Docker.
 * docker run -d -p 9411:9411 openzipkin/zipkin
 */
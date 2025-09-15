package com.mysillydreams.authservice.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for WebClient used for outbound service-to-service communication.
 * Configures timeouts, connection pooling, and logging for all HTTP clients.
 */
@Configuration
@Slf4j
public class WebClientConfig {

    @Value("${app.webclient.connection-timeout:5000}")
    private int connectionTimeoutMs;

    @Value("${app.webclient.read-timeout:10000}")
    private int readTimeoutMs;

    @Value("${app.webclient.write-timeout:10000}")
    private int writeTimeoutMs;

    @Value("${app.webclient.max-memory-size:1048576}") // 1MB default
    private int maxMemorySize;

    @Value("${auth.user-service.base-url:http://localhost:8083}")
    private String userServiceUrl;

    /**
     * Provides a generic, pre-configured WebClient builder.
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutMs)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(writeTimeoutMs, TimeUnit.MILLISECONDS)))
                .responseTimeout(Duration.ofMillis(readTimeoutMs));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxMemorySize))
                .filter(logRequest())
                .filter(logResponse())
                .filter(errorHandling());
    }

    /**
     * Provides a specialized WebClient for communicating with the User Service.
     */
    @Bean("userServiceWebClient")
    public WebClient userServiceWebClient(WebClient.Builder webClientBuilder) {
        log.info("🔧 Configuring User Service WebClient with base URL: {}", userServiceUrl);
        return webClientBuilder
                .baseUrl(userServiceUrl)
                .defaultHeader("User-Agent", "AuthService-UserClient/1.0")
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Request logging filter
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (log.isDebugEnabled()) {
                log.debug("🌐 Outbound Request: {} {}", clientRequest.method(), clientRequest.url());
                clientRequest.headers().forEach((name, values) -> {
                    if (!isSensitiveHeader(name)) {
                        values.forEach(value -> log.debug("🌐 Request Header: {}={}", name, value));
                    }
                });
            }
            return Mono.just(clientRequest);
        });
    }

    /**
     * Response logging filter
     */
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (log.isDebugEnabled()) {
                log.debug("🌐 Response Status: {}", clientResponse.statusCode());
                clientResponse.headers().asHttpHeaders().forEach((name, values) -> {
                    if (!isSensitiveHeader(name)) {
                        values.forEach(value -> log.debug("🌐 Response Header: {}={}", name, value));
                    }
                });
            }
            return Mono.just(clientResponse);
        });
    }

    /**
     * Error handling filter
     */
    private ExchangeFilterFunction errorHandling() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().isError()) {
                log.warn("🚨 HTTP Error Response: {} from {}",
                    clientResponse.statusCode(),
                    clientResponse.request().getURI());
            }
            return Mono.just(clientResponse);
        });
    }

    /**
     * Check if header contains sensitive information
     */
    private boolean isSensitiveHeader(String headerName) {
        String lowerName = headerName.toLowerCase();
        return lowerName.contains("authorization") ||
               lowerName.contains("api-key") ||
               lowerName.contains("token") ||
               lowerName.contains("password") ||
               lowerName.contains("secret");
    }
}

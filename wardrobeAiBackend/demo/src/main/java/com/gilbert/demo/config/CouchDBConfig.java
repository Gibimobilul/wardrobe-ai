package com.gilbert.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class CouchDBConfig {

    @Value("${couchdb.connect-timeout-ms:3000}")
    private int connectTimeoutMs;

    @Bean
    public RestTemplate couchDbRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(connectTimeoutMs * 5);
        return new RestTemplate(factory);
    }
}

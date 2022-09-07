package com.example.hellopatent.config;

import lombok.Getter;
import lombok.Setter;
import org.apache.http.HttpHost;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Setter
@ConfigurationProperties(prefix = "elasticsearch")
public class EsProperties {
    private String host;
    private int port;
    private Indices indices;

    public HttpHost httpHost() {
        return new HttpHost(host, port, "http");
    }

    public String getPatentIndexName() {
        return Optional.ofNullable(indices).map(Indices::getPatentsIndexName).orElse(null);
    }

    public String getTestIndexName() {
        return Optional.ofNullable(indices).map(Indices::getTestIndexName).orElse(null);
    }


    @Getter
    @Setter
    public static class Indices {
        String patentsIndexName;
        String testIndexName;
    }


}

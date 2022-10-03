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

    public String getkrPatentIndexName() {
        return Optional.ofNullable(indices).map(Indices::getKr_patentsIndexName).orElse(null);
    }

    public String getenPatentIndexName() {
        return Optional.ofNullable(indices).map(Indices::getEn_patentsIndexName).orElse(null);
    }

    public String getnotenPatentIndexName() {
        return Optional.ofNullable(indices).map(Indices::getNoten_patentsIndexName).orElse(null);
    }

    public String getjpPatentIndexName() {
        return Optional.ofNullable(indices).map(Indices::getJp_patentsIndexName).orElse(null);
    }

    public String getTestIndexName() {
        return Optional.ofNullable(indices).map(Indices::getTestIndexName).orElse(null);
    }


    @Getter
    @Setter
    public static class Indices {
        String patentsIndexName;
        String kr_patentsIndexName;
        String jp_patentsIndexName;
        String en_patentsIndexName;
        String noten_patentsIndexName;

        String testIndexName;
    }


}

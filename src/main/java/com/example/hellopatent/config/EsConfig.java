package com.example.hellopatent.config;

import lombok.RequiredArgsConstructor;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;

@Configuration
@RequiredArgsConstructor
public class EsConfig extends AbstractElasticsearchConfiguration {
    private final EsProperties esProperties;

    @Override
    public RestHighLevelClient elasticsearchClient() {
        return new RestHighLevelClient(RestClient.builder(esProperties.httpHost()));
    }
}

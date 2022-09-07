package com.example.hellopatent.service;

import org.elasticsearch.action.index.IndexResponse;

import java.io.IOException;
import java.util.Map;

public interface DocumentService {
//    List<String> getSearch() throws IOException;
    IndexResponse createDocument() throws IOException;

    Map<String, Object> getDocument(String id) throws IOException;
}

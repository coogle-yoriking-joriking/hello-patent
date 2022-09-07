package com.example.hellopatent.service;

import com.example.hellopatent.dto.SearchRequestDto;
import org.elasticsearch.action.index.IndexResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface DocumentService {
//    List<String> getSearch() throws IOException;
    IndexResponse createDocument() throws IOException;

    List<Map<String,Object>> getCustom(SearchRequestDto requestDto) throws IOException;

    Map<String, Object> getDocument(String id) throws IOException;

    List<Map<String,Object>> getSearch() throws IOException;



}

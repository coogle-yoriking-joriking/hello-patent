package com.example.hellopatent.service;

import com.example.hellopatent.dto.EnSearchRequestDto;
import com.example.hellopatent.dto.KrSearchRequestDto;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface DocumentService {
    // 필드별 조회
    List<Map<String, Object>> getEnPatent(EnSearchRequestDto requestDto) throws IOException;

    //    List<String> getSearch() throws IOException;
    List<Map<String,Object>> getKrPatent(KrSearchRequestDto requestDto) throws IOException;




}

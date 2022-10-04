package com.example.hellopatent.service;

import com.example.hellopatent.dto.requestdto.EnSearchRequestDto;
import com.example.hellopatent.dto.requestdto.KrSearchRequestDto;
import com.example.hellopatent.dto.responsedto.EnSearchResponseDto;
import com.example.hellopatent.dto.responsedto.KrSearchResponseDto;

import java.io.IOException;

public interface DocumentService {
    // 필드별 조회
    EnSearchResponseDto getEnPatent(EnSearchRequestDto requestDto) throws IOException;

    //    List<String> getSearch() throws IOException;
    KrSearchResponseDto getKrPatent(KrSearchRequestDto requestDto) throws IOException;




}

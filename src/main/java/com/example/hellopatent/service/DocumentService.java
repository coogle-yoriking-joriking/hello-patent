package com.example.hellopatent.service;

import com.example.hellopatent.dto.requestdto.EnSearchRequestDto;
import com.example.hellopatent.dto.requestdto.KrSearchRequestDto;
import com.example.hellopatent.dto.responsedto.EnSearchResponseDto;
import com.example.hellopatent.dto.responsedto.KrSearchResponseDto;

import java.io.IOException;

public interface DocumentService {
    // 해외권 조회
    EnSearchResponseDto getEnPatent(EnSearchRequestDto requestDto) throws IOException;

    //  국내 조회
    KrSearchResponseDto getKrPatent(KrSearchRequestDto requestDto) throws IOException;




}

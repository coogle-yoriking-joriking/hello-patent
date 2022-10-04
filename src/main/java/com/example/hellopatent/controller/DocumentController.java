package com.example.hellopatent.controller;

import com.example.hellopatent.dto.requestdto.EnSearchRequestDto;
import com.example.hellopatent.dto.requestdto.KrSearchRequestDto;
import com.example.hellopatent.dto.responsedto.EnSearchResponseDto;
import com.example.hellopatent.dto.responsedto.KrSearchResponseDto;
import com.example.hellopatent.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;


    @GetMapping("/patent/korean")
    public KrSearchResponseDto getKrPatent(KrSearchRequestDto requestDto) throws IOException {
            return documentService.getKrPatent(requestDto);
    }

    @GetMapping("/patent/foreign")
    public EnSearchResponseDto getEnPatent(EnSearchRequestDto requestDto) throws IOException {
            return documentService.getEnPatent(requestDto);
    }



}

package com.example.hellopatent.controller;

import com.example.hellopatent.dto.EnSearchRequestDto;
import com.example.hellopatent.dto.KrSearchRequestDto;
import com.example.hellopatent.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;


    @GetMapping("/patent/korean")
    public List<Map<String,Object>> getKrPatent( KrSearchRequestDto requestDto) throws IOException {
            return documentService.getKrPatent(requestDto);
    }

    @GetMapping("/patent/foreign")
    public List<Map<String,Object>> getEnPatent( EnSearchRequestDto requestDto) throws IOException {
            return documentService.getEnPatent(requestDto);
    }



}

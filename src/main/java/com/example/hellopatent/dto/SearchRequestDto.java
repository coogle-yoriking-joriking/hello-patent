package com.example.hellopatent.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchRequestDto {

    private String[] contentType;

    private String[] contentValue;

    private String[] exceptType;

    private String[] exceptValue;

    private String[] statusType;

    private String patentType;
}
package com.example.hellopatent.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchRequestDto {

    private String type;

    private String content;
}

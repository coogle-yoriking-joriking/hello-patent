package com.example.hellopatent.dto.requestdto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnSearchRequestDto {

    private String[] contentType;

    private String[] contentValue;

    private String[] exceptType;

    private String[] exceptValue;

    private String sortType;

    private int page = 0;

    private String[] country;

}

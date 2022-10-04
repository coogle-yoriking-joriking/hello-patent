package com.example.hellopatent.dto.responsedto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EnSearchResponseDto {
    private int pagecount;
    private String totalhits;

    private List<EnResponseDto> response;
}

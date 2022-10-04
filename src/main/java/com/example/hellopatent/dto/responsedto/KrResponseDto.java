package com.example.hellopatent.dto.responsedto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KrResponseDto {
    private String 등록번호;
    private String 공고번호;
    private String 출원번호;
    private String 공개번호;
    private String IPC분류;
    private String CPC분류;
    private String 발명의명칭;
    private String 법적상태;
    private String 출원일자;
    private String 출원인;
    private String 요약;

}

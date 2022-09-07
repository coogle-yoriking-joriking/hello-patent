package com.example.hellopatent.model;

import com.sun.istack.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Setter
@Document(indexName = "patent-data")
public class Patent {

    @Id
    @NotNull
    private Long kor_patent_id;

    @Field(type = FieldType.Keyword)
    @NotNull
    private String application_number; //출원번호

    @Field(type = FieldType.Keyword)
    @NotNull
    private String application_date; //출원일자

    @Field(type = FieldType.Text)
    @NotNull
    private String invention_title; //발명의명칭

    @Field(type = FieldType.Text)
    @NotNull
    private String applicant; //출원인

    @Field(type = FieldType.Text)
    private String ipc_number; //IPC 분류

    @Field(type = FieldType.Text)
    private String cpc_number; // CPC 분류

    @Field(type = FieldType.Keyword)
    private Long notice_number; // 공고번호

    @Field(type = FieldType.Keyword)
    private Long publication_number; // 공개번호

    @Field(type = FieldType.Keyword)
    private Long registration_number; // 등록번호

    @Field(type = FieldType.Keyword)
    private String legal_status; // 법적상태

    @Field(type = FieldType.Text)
    private String summary; // 요약


}

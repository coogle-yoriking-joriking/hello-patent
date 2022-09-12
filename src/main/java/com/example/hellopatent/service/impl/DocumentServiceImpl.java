package com.example.hellopatent.service.impl;

import com.example.hellopatent.dto.SearchRequestDto;
import org.elasticsearch.action.search.SearchRequest;
import com.example.hellopatent.config.EsProperties;
import com.example.hellopatent.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

import static org.elasticsearch.xcontent.XContentFactory.jsonBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {
    private final RestHighLevelClient client;
    private final EsProperties esProperties;

    private static final String APPLICATION_NUMBER = "application_number";
    private static final String APPLICATION_DATE = "application_date";
    private static final String INVENTION_TITLE = "invention_title";
    private static final String APPLICANT = "applicant";
    private static final String IPC_NUMBER = "ipc_number";
    private static final String CPC_NUMBER = "cpc_number";
    private static final String NOTICE_NUMBER = "notice_number";
    private static final String PUBLICATION_NUMBER = "publication_number";
    private static final String REGISTRATION_NUMBER = "registration_number";
    private static final String LEGAL_STATUS = "legal_status";
    private static final String SUMMARY = "summary";

    @Override // 전체 조회
    public List<Map<String, Object>> getSearch() throws IOException {

        SearchRequest searchRequest = new SearchRequest(esProperties.getPatentIndexName());
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(500);


        searchRequest.source(searchSourceBuilder);
        List<Map<String, Object>> list = new ArrayList<>();
        SearchHits searchHits = client.search(searchRequest, RequestOptions.DEFAULT).getHits();
        for (SearchHit hit : searchHits) {
            Map<String, Object> sourceMap = hit.getSourceAsMap();
            list.add(sourceMap);
        }
        return list;

    }


    @Override // 필드별 조회
    public List<Map<String, Object>> getCustom(SearchRequestDto requestDto) throws IOException {

        SearchRequest searchRequest = new SearchRequest(esProperties.getPatentIndexName());
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //제외방식 중첩?
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        for (int i = 0; i < requestDto.getContentType().length; i++) {

            // contentType=전체 contentValue=전기,반도체
            // &exceptType=발명의명칭&exceptValue=전자

            boolean isNum = Objects.equals(requestDto.getContentType()[i], "번호정보");
            boolean isAll = Objects.equals(requestDto.getContentType()[i], "전체");
            boolean isPc = Objects.equals(requestDto.getContentType()[i], "IPC/CPC분류");
            boolean isYear = Objects.equals(requestDto.getContentType()[i], "연도");

            if (isNum) {
                boolQueryBuilder.must(QueryBuilders.multiMatchQuery(
                        requestDto.getContentValue()[i], "출원번호", "공개번호", "공고번호", "등록번호"));
            } else if (isAll) {
                boolQueryBuilder.must(QueryBuilders.multiMatchQuery(
                        "*" + requestDto.getContentValue()[i] + "*", "*").type("cross_fields"));
            }else if (isPc) {
                boolQueryBuilder.must(QueryBuilders.moreLikeThisQuery(new String[]{"IPC분류", "CPC분류"},
                        new String[]{requestDto.getContentValue()[i]}, null).minDocFreq(1).minTermFreq(1));
            }else if (isYear) {
                boolQueryBuilder.must(QueryBuilders.wildcardQuery(
                        "출원일자",requestDto.getContentValue()[i]+ ".*"));
            } else {
                boolQueryBuilder.must(QueryBuilders.wildcardQuery(
                        requestDto.getContentType()[i], "*" + requestDto.getContentValue()[i] + "*"));
            }
        }

        if (requestDto.getExceptType() != null) {
            for (int z = 0; z < requestDto.getExceptType().length; z++) {
                boolean isENum = Objects.equals(requestDto.getExceptType()[z], "번호정보");
                boolean isEAll = Objects.equals(requestDto.getExceptType()[z], "전체");
                boolean isPc = Objects.equals(requestDto.getExceptType()[z], "IPC/CPC분류");
                boolean isYear = Objects.equals(requestDto.getExceptType()[z], "연도");
                if (isENum) {
                    boolQueryBuilder.mustNot(QueryBuilders.multiMatchQuery(
                            requestDto.getExceptValue()[z], "출원번호", "공개번호", "공고번호", "등록번호"));
                } else if (isEAll) {
                    boolQueryBuilder.mustNot(QueryBuilders.multiMatchQuery(
                            requestDto.getExceptValue()[z], "*"));
                }else if (isPc) {
                    boolQueryBuilder.mustNot(QueryBuilders.multiMatchQuery(
                            requestDto.getExceptValue()[z], "IPC분류", "CPC분류"));
                }else if (isYear) {
                    boolQueryBuilder.mustNot(QueryBuilders.wildcardQuery(
                            "출원일자",requestDto.getExceptValue()[z]+ ".*"));
                }else {
                    boolQueryBuilder.mustNot(QueryBuilders.wildcardQuery(requestDto.getExceptType()[z], requestDto.getExceptValue()[z] ));
                }
            }
        }


        // 법적 상태
        if (requestDto.getStatusType() != null) {
            for (int k = 0; k < requestDto.getStatusType().length; k++) {
                boolQueryBuilder.should(QueryBuilders.termQuery("법적상태", requestDto.getStatusType()[k]));
            }
        }

        if(Objects.equals(requestDto.getPatentType(), "특허")) {
            boolQueryBuilder.must(QueryBuilders.wildcardQuery(
                    "출원번호", "10-*"
            ));
        } else if(Objects.equals(requestDto.getPatentType(), "실용")) {
            boolQueryBuilder.must(QueryBuilders.wildcardQuery(
                    "출원번호", "20-*"
            ));
        }

        searchSourceBuilder.size(500);
        searchSourceBuilder.query(boolQueryBuilder);
        searchRequest.source(searchSourceBuilder);
        List<Map<String, Object>> list = new ArrayList<>();
        SearchHits searchHits = client.search(searchRequest, RequestOptions.DEFAULT).getHits();
        for (
                SearchHit hit : searchHits) {
            Map<String, Object> sourceMap = hit.getSourceAsMap();
            list.add(sourceMap);
        }
        return list;

    }

    // queryDSL 로 표현했을 때
    // GET /students/_search
    //{
    //  "query": {
    //    "bool": {
    //      "should": [
    //        {
    //          "match": {
    //            "name": "학생1"
    //          }
    //        },
    //        {
    //          "match": {
    //            "name": "학생22"
    //          }
    //        }
    //      ]
    //    }
    //  }
    //}
//    }

    @Override
    public IndexResponse createDocument() throws IOException {
        IndexRequest request = new IndexRequest(esProperties.getPatentIndexName());
        request.source(jsonBuilder()
                .startObject()
                .field(APPLICATION_NUMBER, "10326546421")
                .field(APPLICATION_DATE, "2022.09.07")
                .field(INVENTION_TITLE, "엘라스틱서치와 스프링부트 연동")
                .field(APPLICANT, "희희")
                .field(IPC_NUMBER, "A307 1022 5456")
                .field(CPC_NUMBER, "G307 1022 5456")
                .field(NOTICE_NUMBER, "65498463513")
                .field(PUBLICATION_NUMBER, "45312154")
                .field(REGISTRATION_NUMBER, "544534543453")
                .field(LEGAL_STATUS, "공개")
                .field(SUMMARY, "룰루랄라")
                .endObject());
        try {
            return client.index(request, RequestOptions.DEFAULT);
        } catch (ElasticsearchException e) {
            if (e.status() == RestStatus.CONFLICT) {
                log.error("문서 생성에 실패하였습니다.");
            }
        }
        return null;
    }

    @Override //id 별 조회
    public Map<String, Object> getDocument(String id) throws IOException {
        GetRequest request = new GetRequest(esProperties.getPatentIndexName(), id);
        GetResponse getResponse = client.get(request, RequestOptions.DEFAULT);
        if (getResponse.isExists()) {
            // 도큐먼트가 있는 경우
            return getResponse.getSourceAsMap();
        }
        return null;
    }


//    @Override
//    public DeleteResponse deleteDocument(String id) throws IOException {
//        DeleteRequest deleteRequest = new DeleteRequest(esProperties.getStudentIndexName(), id);
//        return client.delete(deleteRequest, RequestOptions.DEFAULT);
//    }
//
//    @Override
//    public UpdateResponse updateDocumentByScript(String id) throws IOException {
//        UpdateRequest updateRequest = new UpdateRequest(esProperties.getStudentIndexName(), id);
//
//        // name 을 이름 수정으로 변경
//        Map<String, Object> parameterMap = Collections.singletonMap(NAME, "이름수정");
//        Script inline = new Script(ScriptType.INLINE, "painless", "ctx..name = params.name", parameterMap);
//        // 이미 스크립트가 등록되어 있는 경우에는 ScriptType 을 Stored 로
//        updateRequest.script(inline);
//        // updateRequest.scriptedUpsert(true/false);
//        // updateRequest.upsert(inline);
//
//        try {
//            return client.update(updateRequest, RequestOptions.DEFAULT);
//        } catch (ElasticsearchException e) {
//            if (e.status() == RestStatus.NOT_FOUND) {
//                System.out.println("업데이트 대상이 존재하지 않습니다. ");
//            }
//        }
//        return null;
//    }
//
//    @Override
//    public UpdateResponse updateDocument(String id) throws IOException {
//        // name 을 이름수정으로 변경
//        XContentBuilder builder = jsonBuilder()
//                .startObject()
//                .field(NAME, "이름수정")
//                .endObject();
//
//        UpdateRequest updateRequest = new UpdateRequest(esProperties.getStudentIndexName(), id).doc(builder);
//        return client.update(updateRequest, RequestOptions.DEFAULT);
//    }
//
//    @Override
//    public UpdateResponse upsertDocument(String id) throws IOException {
//        IndexRequest indexRequest = new IndexRequest(esProperties.getStudentIndexName()).source(jsonBuilder().startObject().field(NAME, "소소").endObject());
//
//        XContentBuilder xContentBuilder = jsonBuilder()
//                .startObject()
//                .field(CREATED_AT, new Date())
//                .endObject();
//
//        UpdateRequest updateRequest = new UpdateRequest(esProperties.getStudentIndexName(), id).doc(xContentBuilder).upsert(indexRequest);
//        return client.update(updateRequest, RequestOptions.DEFAULT);
//    }
//
//    @Override
//    public BulkResponse createDocumentBulk() throws IOException {
//        BulkRequest request = new BulkRequest();
//        request.add(new IndexRequest(esProperties.getStudentIndexName()).source(XContentType.JSON, NAME, "테스트1"));
//        request.add(new IndexRequest(esProperties.getStudentIndexName()).source(XContentType.JSON, NAME, "테스트2"));
//        request.add(new IndexRequest(esProperties.getStudentIndexName()).source(XContentType.JSON, NAME, "테스트3"));
//        return client.bulk(request, RequestOptions.DEFAULT);

//    @Override // 필드별 조회
//    public List<Map<String,Object>> getCustom1(SearchRequestDto requestDto) throws IOException {
//        d
//        SearchRequest searchRequest = new SearchRequest(esProperties.getPatentIndexName());
//        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
//        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
//        boolQueryBuilder.should(QueryBuilders.wildcardQuery(requestDto.getType(), "*" + requestDto.getContent() + "*"));
//
//        searchSourceBuilder.size(500);
//
//        searchSourceBuilder.query(boolQueryBuilder);
//        searchRequest.source(searchSourceBuilder);
//        List<Map<String,Object>> list = new ArrayList<>();
//        SearchHits searchHits = client.search(searchRequest, RequestOptions.DEFAULT).getHits();
//        for(SearchHit hit : searchHits) {
//            Map<String, Object> sourceMap = hit.getSourceAsMap();
//            list.add(sourceMap);
//        }
//        return list;
//
//    }


}

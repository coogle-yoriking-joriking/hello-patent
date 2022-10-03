package com.example.hellopatent.service.impl;

import com.example.hellopatent.config.EsProperties;
import com.example.hellopatent.dto.EnSearchRequestDto;
import com.example.hellopatent.dto.KrSearchRequestDto;
import com.example.hellopatent.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {
    private final RestHighLevelClient client;
    private final EsProperties esProperties;




    @Override // 국내 조회
    public List<Map<String, Object>> getKrPatent(KrSearchRequestDto requestDto) throws IOException {

        SearchRequest searchRequest = new SearchRequest(esProperties.getkrPatentIndexName());

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

        for (int i = 0; i < requestDto.getContentType().length; i++) {

            // contentType=전체 contentValue=전기,반도체
            // &exceptType=발명의명칭&exceptValue=전자

            boolean isNum = Objects.equals(requestDto.getContentType()[i], "번호정보");
            boolean isAll = Objects.equals(requestDto.getContentType()[i], "전체");
            boolean isPc = Objects.equals(requestDto.getContentType()[i], "IPC/CPC분류");
            boolean isYear = Objects.equals(requestDto.getContentType()[i], "연도");
            boolean isName = Objects.equals(requestDto.getContentType()[i], "발명의명칭");
            boolean isSummary = Objects.equals(requestDto.getContentType()[i], "요약");


            if (isNum) {
                boolQueryBuilder.must(QueryBuilders.multiMatchQuery(
                        requestDto.getContentValue()[i], "*번호"));
            } else if (isAll) {
                boolQueryBuilder
                        .must(QueryBuilders.multiMatchQuery(requestDto.getContentValue()[i],"*"));

            }else if (isPc) {
                boolQueryBuilder.must(QueryBuilders.moreLikeThisQuery(new String[]{"IPC분류", "CPC분류"},
                        new String[]{requestDto.getContentValue()[i]}, null).minDocFreq(1).minTermFreq(1));
            }else if (isYear) {
                boolQueryBuilder.must(QueryBuilders.matchQuery(
                        "출원일자",requestDto.getContentValue()[i]));
            }
            else if (isName) {
                if(requestDto.getContentType().length == 1) {
                    boolQueryBuilder
                            .must(QueryBuilders.multiMatchQuery(requestDto.getContentValue()[i],"발명의명칭*"));
                }
            }
            else if (isSummary) {
                boolQueryBuilder
                        .must(QueryBuilders.multiMatchQuery(requestDto.getContentValue()[i],"요약*"));
            }
            else {
                boolQueryBuilder.must(QueryBuilders.matchQuery(
                        requestDto.getContentType()[i],  requestDto.getContentValue()[i]));
            }
        }


        if (requestDto.getExceptType() != null) {
            for (int z = 0; z < requestDto.getExceptType().length; z++) {
                boolean isENum = Objects.equals(requestDto.getExceptType()[z], "번호정보");
                boolean isEAll = Objects.equals(requestDto.getExceptType()[z], "전체");
                boolean isPc = Objects.equals(requestDto.getExceptType()[z], "IPC/CPC분류");
                boolean isYear = Objects.equals(requestDto.getExceptType()[z], "연도");
                boolean isName = Objects.equals(requestDto.getExceptType()[z], "발명의명칭");
                boolean isSummary = Objects.equals(requestDto.getExceptType()[z], "요약");

                if (isENum) {
                    boolQueryBuilder.mustNot(QueryBuilders.multiMatchQuery(
                            requestDto.getExceptValue()[z], "*번호"));
                } else if (isEAll) {
                    boolQueryBuilder.mustNot(QueryBuilders.multiMatchQuery(
                            requestDto.getExceptValue()[z], "*"));
                } else if (isName) {
                    boolQueryBuilder.mustNot(QueryBuilders.multiMatchQuery(
                            requestDto.getExceptValue()[z],"발명의명칭*"));
                } else if (isSummary) {
                    boolQueryBuilder.mustNot(QueryBuilders.multiMatchQuery(
                            requestDto.getExceptValue()[z],"요약*"));
                }
                else if (isPc) {
                    boolQueryBuilder.mustNot(QueryBuilders.multiMatchQuery(
                            requestDto.getExceptValue()[z], "*분류"));
                } else if (isYear) {
                    boolQueryBuilder.mustNot(QueryBuilders.matchQuery(
                            "출원일자",requestDto.getExceptValue()[z]));
                }else {
                    boolQueryBuilder.mustNot(QueryBuilders.matchQuery(
                            requestDto.getExceptType()[z], requestDto.getExceptValue()[z] ));
                }
            }
        }


        // 법적 상태
        if (requestDto.getStatusType() != null) {
                boolQueryBuilder.must(QueryBuilders.termsQuery("법적상태", requestDto.getStatusType()));
        }

        if(Objects.equals(requestDto.getPatentType(), "특허")) {
            boolQueryBuilder.must(QueryBuilders.matchQuery(
                    "출원번호", "10"
            ));
        } else if(Objects.equals(requestDto.getPatentType(), "실용")) {
            boolQueryBuilder.must(QueryBuilders.matchQuery(
                    "출원번호", "20"
            ));
        }

        if(Objects.equals(requestDto.getSortType(), "최신순")) {
            searchSourceBuilder.sort("출원일자.keyword", SortOrder.DESC);
        } else if (Objects.equals(requestDto.getSortType(), "오래된순")) {
            searchSourceBuilder.sort("출원일자.keyword",SortOrder.ASC);
        }


        //페이지 네이션
        searchSourceBuilder.size(20);
        searchSourceBuilder.from((requestDto.getPage()-1) * 20);

        searchSourceBuilder.query(boolQueryBuilder);

        searchRequest.source(searchSourceBuilder);

        List<Map<String, Object>> list = new ArrayList<>();
        SearchHits searchHits = client.search(searchRequest, RequestOptions.DEFAULT).getHits();
        System.out.println(searchHits.getTotalHits());
        for (
                SearchHit hit : searchHits) {
            Map<String, Object> sourceMap = hit.getSourceAsMap();
            list.add(sourceMap);
        }
        return list;

    }

    @Override // 해외 조회
    public List<Map<String, Object>> getEnPatent(EnSearchRequestDto requestDto) throws IOException {


        SearchRequest searchRequest = new SearchRequest( esProperties.getenPatentIndexName(),esProperties.getjpPatentIndexName(),esProperties.getnotenPatentIndexName());

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();


        //전체 검색
        for (int i = 0; i < requestDto.getContentType().length; i++) {

            // contentType=전체 contentValue=전기,반도체
            // &exceptType=발명의명칭&exceptValue=전자

            boolean isNum = Objects.equals(requestDto.getContentType()[i], "번호정보");
            boolean isAll = Objects.equals(requestDto.getContentType()[i], "전체");
            boolean isPc = Objects.equals(requestDto.getContentType()[i], "IPC/CPC분류");
            boolean isYear = Objects.equals(requestDto.getContentType()[i], "연도");


            if (isNum) {
                boolQueryBuilder.must(QueryBuilders.multiMatchQuery(
                        requestDto.getContentValue()[i], "*번호"));
            } else if (isAll) {
                boolQueryBuilder
                        .must(QueryBuilders.multiMatchQuery(requestDto.getContentValue()[i],"*"));
            }else if (isPc) {
                boolQueryBuilder.must(QueryBuilders.moreLikeThisQuery(new String[]{"IPC분류", "CPC분류"},
                        new String[]{requestDto.getContentValue()[i]}, null).minDocFreq(1).minTermFreq(1));
            }else if (isYear) {
                boolQueryBuilder.must(QueryBuilders.matchQuery(
                        "출원일자",requestDto.getContentValue()[i]));
            } else {
                boolQueryBuilder.must(QueryBuilders.matchQuery(
                        requestDto.getContentType()[i],requestDto.getContentValue()[i]).operator(Operator.AND).fuzziness(Fuzziness.ONE));
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
                            requestDto.getExceptValue()[z], "*번호"));
                } else if (isEAll) {
                    boolQueryBuilder.mustNot(QueryBuilders.multiMatchQuery(
                            requestDto.getExceptValue()[z], "*"));
                }else if (isPc) {
                    boolQueryBuilder.mustNot(QueryBuilders.multiMatchQuery(
                            requestDto.getExceptValue()[z], "IPC분류", "CPC분류"));
                }else if (isYear) {
                    boolQueryBuilder.mustNot(QueryBuilders.matchQuery(
                            "출원일자",requestDto.getExceptValue()[z]));
                }else {
                    boolQueryBuilder.mustNot(QueryBuilders.matchQuery(requestDto.getExceptType()[z], requestDto.getExceptValue()[z] ));
                }
            }
        }


        //정렬
        if(Objects.equals(requestDto.getSortType(), "최신순")) {
            searchSourceBuilder.sort("출원일자.keyword", SortOrder.DESC);
        } else if (Objects.equals(requestDto.getSortType(), "오래된순")) {
            searchSourceBuilder.sort("출원일자.keyword",SortOrder.ASC);
        }



        //페이지 네이션
        searchSourceBuilder.size(20);
        searchSourceBuilder.from((requestDto.getPage()-1) * 20);

        //1차 쿼리
        searchSourceBuilder.query(boolQueryBuilder);

        if(requestDto.getCountry() != null) {
            //국가 선택
            boolQueryBuilder.must(QueryBuilders.termsQuery("국가",requestDto.getCountry()));
        }

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



}

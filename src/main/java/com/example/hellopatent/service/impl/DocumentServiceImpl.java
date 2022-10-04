package com.example.hellopatent.service.impl;

import com.example.hellopatent.config.EsProperties;
import com.example.hellopatent.dto.responsedto.EnResponseDto;
import com.example.hellopatent.dto.requestdto.EnSearchRequestDto;
import com.example.hellopatent.dto.responsedto.EnSearchResponseDto;
import com.example.hellopatent.dto.requestdto.KrSearchRequestDto;
import com.example.hellopatent.dto.responsedto.KrResponseDto;
import com.example.hellopatent.dto.responsedto.KrSearchResponseDto;
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
import java.util.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {
    private final RestHighLevelClient client;
    private final EsProperties esProperties;
    private final static HashMap<String,String> nation = new HashMap<>() {{//초기값 지정
        put("US", "미국");
        put("EP", "유럽");
        put("WO", "PCT");
        put("JP", "일본");
        put("CN", "중국");
        put("GB", "영국");
        put("DE", "독일");
        put("FR", "프랑스");
        put("AU", "호주");
        put("CA", "캐나다");
        put("RU", "러시아");
        put("TW", "대만");
        put("AT", "오스트리아");
        put("DK", "덴마크");
        put("IL", "이스라엘");
        put("ES", "스페인");
        put("PT", "포르투갈");
        put("PH", "필리핀");
        put("CH", "스위스");
        put("PL", "폴란드");
        put("SE", "스웨덴");
        put("SI", "슬로베니아");
        put("CO", "콜롬비아");
        put("EA", "유라시아");
        put("RS", "세르비아");
    }};


    @Override // 국내 조회
    public KrSearchResponseDto getKrPatent(KrSearchRequestDto requestDto) throws IOException {

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
        searchSourceBuilder.trackTotalHits(true);
        searchRequest.source(searchSourceBuilder);

        List<KrResponseDto> list = new ArrayList<>();
        SearchHits searchHits = client.search(searchRequest, RequestOptions.DEFAULT).getHits();
        KrSearchResponseDto responseDto = new KrSearchResponseDto();

        String totalhits = searchHits.getTotalHits().toString();
        totalhits = totalhits.substring(0,totalhits.length()-5);

        int pagetotal = (int) Math.ceil(Integer.parseInt(totalhits) / 20.0);

        responseDto.setPagecount(Math.min(pagetotal, 500));

        responseDto.setTotalhits(totalhits);


        for (
                SearchHit hit : searchHits) {
            Map<String, Object> sourceMap = hit.getSourceAsMap();
            KrResponseDto krResponseDto = new KrResponseDto();
            krResponseDto.set공개번호((String) sourceMap.get("공개번호"));
            krResponseDto.set출원번호((String) sourceMap.get("출원번호"));
            krResponseDto.set공고번호((String) sourceMap.get("공고번호"));
            krResponseDto.set등록번호((String) sourceMap.get("등록번호"));
            krResponseDto.set출원일자((String) sourceMap.get("출원일자"));
            krResponseDto.setIPC분류((String) sourceMap.get("IPC분류"));
            krResponseDto.setCPC분류((String) sourceMap.get("CPC분류"));
            krResponseDto.set법적상태((String) sourceMap.get("법적상태"));
            krResponseDto.set요약((String) sourceMap.get("요약"));
            krResponseDto.set발명의명칭((String) sourceMap.get("발명의명칭"));
            krResponseDto.set출원인((String) sourceMap.get("출원인"));
            list.add(krResponseDto);
        }
        responseDto.setResponse(list);

        return responseDto;

    }

    @Override // 해외 조회
    public EnSearchResponseDto getEnPatent(EnSearchRequestDto requestDto) throws IOException {


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
        searchSourceBuilder.trackTotalHits(true);
        searchSourceBuilder.from((requestDto.getPage()-1) * 20);

        //1차 쿼리
        searchSourceBuilder.query(boolQueryBuilder);

        if(requestDto.getCountry() != null) {
            //국가 선택
            boolQueryBuilder.must(QueryBuilders.termsQuery("국가",requestDto.getCountry()));
        }
        EnSearchResponseDto enSearchResponseDto = new EnSearchResponseDto();
        List<EnResponseDto> list = new ArrayList<>();

        searchRequest.source(searchSourceBuilder);

        SearchHits searchHits = client.search(searchRequest, RequestOptions.DEFAULT).getHits();
        String totalhits = searchHits.getTotalHits().toString();
        totalhits = totalhits.substring(0,totalhits.length()-5);

        enSearchResponseDto.setTotalhits(totalhits);

        int pagetotal = (int) Math.ceil(Integer.parseInt(totalhits) / 20.0);

        enSearchResponseDto.setPagecount(Math.min(pagetotal, 500));

        for (SearchHit hit : searchHits) {
            Map<String, Object> sourceMap = hit.getSourceAsMap();
            EnResponseDto enResponseDto = new EnResponseDto();
            enResponseDto.set공개번호((String) sourceMap.get("공개번호"));
            enResponseDto.set출원번호((String) sourceMap.get("출원번호"));
            enResponseDto.set공고번호((String) sourceMap.get("공고번호"));
            enResponseDto.set등록번호((String) sourceMap.get("등록번호"));
            enResponseDto.set출원일자((String) sourceMap.get("출원일자"));
            enResponseDto.setIPC분류((String) sourceMap.get("IPC분류"));
            enResponseDto.setCPC분류((String) sourceMap.get("CPC분류"));
            enResponseDto.set국가(nation.get(sourceMap.get("국가")));
            enResponseDto.set요약((String) sourceMap.get("요약"));
            enResponseDto.set발명의명칭((String) sourceMap.get("발명의명칭"));
            enResponseDto.set출원인((String) sourceMap.get("출원인"));
            list.add(enResponseDto);
        }
        enSearchResponseDto.setResponse(list);
        return enSearchResponseDto;

    }



}

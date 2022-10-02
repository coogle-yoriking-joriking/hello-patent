package com.example.hellopatent.service.impl;

import com.example.hellopatent.config.EsProperties;
import com.example.hellopatent.dto.SearchRequestDto;
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
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
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

    private BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();



    @Override // 국내 조회
    public List<Map<String, Object>> getKrPatent(SearchRequestDto requestDto) throws IOException {

        SearchRequest searchRequest = new SearchRequest(esProperties.getkrPatentIndexName());
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //제외방식 중첩?
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

        //should용
        BoolQueryBuilder boolQueryBuilder1 = new BoolQueryBuilder();

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
                        requestDto.getContentValue()[i], "출원번호", "공개번호", "공고번호", "등록번호"));
            } else if (isAll) {
                boolQueryBuilder
                        .should(QueryBuilders.matchQuery("출원인",requestDto.getContentValue()[i]).fuzziness(Fuzziness.ONE))
                        .should(QueryBuilders.matchQuery("요약.lblank",requestDto.getContentValue()[i]).fuzziness(Fuzziness.ONE))
                        .should(QueryBuilders.matchQuery("요약",requestDto.getContentValue()[i]).fuzziness(Fuzziness.ONE))
                        .should(QueryBuilders.matchQuery("발명의명칭.lblank",requestDto.getContentValue()[i]).boost(3.0f).fuzziness(Fuzziness.ONE))
                        .should(QueryBuilders.matchQuery("발명의명칭.nori",requestDto.getContentValue()[i]).boost(3.0f).fuzziness(Fuzziness.ONE))

                        .should(QueryBuilders.matchQuery("발명의명칭.nori",requestDto.getContentValue()[i]))
                        .should(QueryBuilders.matchQuery("발명의명칭.ngram",requestDto.getContentValue()[i]).boost(8.0f))
                        .should(QueryBuilders.matchQuery("요약.nori",requestDto.getContentValue()[i]));

            }else if (isPc) {
                boolQueryBuilder.must(QueryBuilders.moreLikeThisQuery(new String[]{"IPC분류", "CPC분류"},
                        new String[]{requestDto.getContentValue()[i]}, null).minDocFreq(1).minTermFreq(1));
            }else if (isYear) {
                boolQueryBuilder.must(QueryBuilders.matchQuery(
                        "출원일자",requestDto.getContentValue()[i]));
            }
            else if (isName) {
                boolQueryBuilder1
                        .should(QueryBuilders.matchQuery("발명의명칭.lblank",requestDto.getContentValue()[i]).boost(3.0f).fuzziness(Fuzziness.ONE))
                        .should(QueryBuilders.matchQuery("발명의명칭.nori",requestDto.getContentValue()[i]).boost(3.0f).fuzziness(Fuzziness.ONE))
                        .should(QueryBuilders.matchQuery("발명의명칭.nori",requestDto.getContentValue()[i]))
                        .should(QueryBuilders.matchQuery("발명의명칭.ngram",requestDto.getContentValue()[i]).boost(8.0f));
            }

            else if (isSummary) {
                boolQueryBuilder1
                        .should(QueryBuilders.matchQuery("요약",requestDto.getContentValue()[i]).fuzziness(Fuzziness.ONE))
                        .should(QueryBuilders.matchQuery("요약.nori",requestDto.getContentValue()[i]));
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
                    boolQueryBuilder.mustNot(QueryBuilders.matchQuery(
                            "출원일자",requestDto.getExceptValue()[z]));
                }else {
                    boolQueryBuilder.mustNot(QueryBuilders.wildcardQuery(requestDto.getExceptType()[z], requestDto.getExceptValue()[z] ));
                }
            }
        }


        // 법적 상태
        if (requestDto.getStatusType() != null) {
                boolQueryBuilder.should(QueryBuilders.termsQuery("법적상태", requestDto.getStatusType()));
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

        searchSourceBuilder.size(20);
        searchSourceBuilder.from((requestDto.getPage()-1) * 20);

        searchSourceBuilder.highlighter(new HighlightBuilder().field("발명의명칭",230));
        searchSourceBuilder.size(500);
        searchSourceBuilder.query(boolQueryBuilder1);
        searchSourceBuilder.postFilter(boolQueryBuilder1);

        searchRequest.source(searchSourceBuilder);

        List<Map<String, Object>> list = new ArrayList<>();
        SearchHits searchHits = client.search(searchRequest, RequestOptions.DEFAULT).getHits();
        searchHits.getSortFields();
        for (
                SearchHit hit : searchHits) {
            Map<String, Object> sourceMap = hit.getSourceAsMap();
            list.add(sourceMap);
        }
        return list;

    }

    public BoolQueryBuilder nation (SearchRequestDto requestDto) {
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.must(QueryBuilders.termsQuery("국가",requestDto.getCountry()));
        return boolQueryBuilder;
    }

    @Override // 해외 조회
    public List<Map<String, Object>> getEnPatent(SearchRequestDto requestDto) throws IOException {

        SearchRequest searchRequest = new SearchRequest(esProperties.getenPatentIndexName(),esProperties.getjpPatentIndexName(),esProperties.getnotenPatentIndexName());
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

        BoolQueryBuilder boolQueryBuilder1 = new BoolQueryBuilder();


        //전체 검색
        for (int i = 0; i < requestDto.getContentType().length; i++) {

            // contentType=전체 contentValue=전기,반도체
            // &exceptType=발명의명칭&exceptValue=전자

            boolean isNum = Objects.equals(requestDto.getContentType()[i], "번호정보");
            boolean isAll = Objects.equals(requestDto.getContentType()[i], "전체");
            boolean isPc = Objects.equals(requestDto.getContentType()[i], "IPC/CPC분류");
            boolean isYear = Objects.equals(requestDto.getContentType()[i], "연도");


            if (isNum) {
                boolQueryBuilder1.must(QueryBuilders.multiMatchQuery(
                        requestDto.getContentValue()[i], "출원번호", "공개번호", "공고번호", "등록번호"));
            } else if (isAll) {
                boolQueryBuilder.should(QueryBuilders.matchQuery("출원인",requestDto.getContentValue()[i]).operator(Operator.AND).fuzziness(Fuzziness.ONE))
                                .should(QueryBuilders.matchQuery("요약",requestDto.getContentValue()[i]).boost(1.5f).fuzziness(Fuzziness.ONE).operator(Operator.AND))
                                .should(QueryBuilders.matchQuery("발명의명칭",requestDto.getContentValue()[i]).boost(3.0f).operator(Operator.AND).fuzziness(Fuzziness.ONE))
                                .should(QueryBuilders.multiMatchQuery(requestDto.getContentValue()[i],"IPC분류","CPC분류", "공개번호", "공고번호", "등록번호", "출원번호","출원일자").type("cross_fields"));
            }else if (isPc) {
                boolQueryBuilder1.must(QueryBuilders.moreLikeThisQuery(new String[]{"IPC분류", "CPC분류"},
                        new String[]{requestDto.getContentValue()[i]}, null).minDocFreq(1).minTermFreq(1));
            }else if (isYear) {
                boolQueryBuilder1.must(QueryBuilders.matchQuery(
                        "출원일자",requestDto.getContentValue()[i]));
            } else {
                boolQueryBuilder1.must(QueryBuilders.matchQuery(
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
                            requestDto.getExceptValue()[z], "출원번호", "공개번호", "공고번호", "등록번호"));
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
        searchSourceBuilder.explain(true);

        //1차 쿼리
        searchSourceBuilder.query(boolQueryBuilder);

        if(requestDto.getCountry() != null) {
            //국가 선택
            boolQueryBuilder1.must(QueryBuilders.termsQuery("국가",requestDto.getCountry()));
        }

        //1차 쿼리를 토대로한 2차쿼리
        searchSourceBuilder.postFilter(boolQueryBuilder1);

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



}

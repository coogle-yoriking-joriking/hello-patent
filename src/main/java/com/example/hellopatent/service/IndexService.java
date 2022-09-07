package com.example.hellopatent.service;

public interface IndexService {
    boolean createIndexSync();

    void createIndexAsync();

    boolean deleteIndex();

    boolean openIndex();

//    boolean closeIndex();
}

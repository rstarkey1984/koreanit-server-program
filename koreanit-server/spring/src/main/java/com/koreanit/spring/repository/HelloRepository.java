package com.koreanit.spring.repository;

import org.springframework.stereotype.Repository;

@Repository
public class HelloRepository {

    public String ping() {
        return "pong";
    }
}
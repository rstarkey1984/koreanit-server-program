package com.koreanit.spring.service;

import com.koreanit.spring.repository.HelloRepository;
import org.springframework.stereotype.Service;

@Service
public class HelloService {

    private final HelloRepository helloRepository;

    public HelloService(HelloRepository helloRepository) {
        this.helloRepository = helloRepository;
    }

    public String ping() {
        return helloRepository.ping();
    }
}
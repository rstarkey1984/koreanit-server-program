package com.koreanit.spring.controller;

import com.koreanit.spring.service.HelloService;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

  private final HelloService helloService;

  public HelloController(HelloService helloService) {
    this.helloService = helloService;
  }

  @GetMapping("/ping")
  public String ping() {
    return helloService.ping();
  }

  @GetMapping("/hello-json")
  public Map<String, String> helloJson() {
    Map<String, String> result = new HashMap<>();
    result.put("message", "Hello JSON");
    return result;
  }
}
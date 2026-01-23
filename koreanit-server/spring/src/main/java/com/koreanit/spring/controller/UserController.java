package com.koreanit.spring.controller;

import com.koreanit.spring.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/db-check")
    public Integer dbCheck() {
        return userService.checkConnection();
    }
}
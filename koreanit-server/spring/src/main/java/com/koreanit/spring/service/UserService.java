package com.koreanit.spring.service;

import com.koreanit.spring.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Integer checkConnection() {
        return userRepository.findOne();
    }
}
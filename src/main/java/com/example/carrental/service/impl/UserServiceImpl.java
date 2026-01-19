package com.example.carrental.service.impl;

import com.example.carrental.mapper.user.UserMapper;
import com.example.carrental.repository.UserRepository;
import com.example.carrental.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final UserMapper mapper;


}

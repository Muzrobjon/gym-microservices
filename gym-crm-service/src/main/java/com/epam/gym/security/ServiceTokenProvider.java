package com.epam.gym.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ServiceTokenProvider {

    @Value("${service.internal.token}")
    private String internalServiceToken;

    public String getServiceToken() {
        return internalServiceToken;
    }
}
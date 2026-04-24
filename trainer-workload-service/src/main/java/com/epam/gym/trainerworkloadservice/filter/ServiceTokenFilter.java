package com.epam.gym.trainerworkloadservice.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ServiceTokenFilter {

    @Value("${service.internal.token}")
    private String expectedToken;

    public boolean isValidServiceCall(String authHeader) {
        if (authHeader == null) return false;
        String token = authHeader.replace("Bearer ", "");

        return token.equals(expectedToken);
    }
}
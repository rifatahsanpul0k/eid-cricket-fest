package com.eidcricketfest.security;

import com.eidcricketfest.common.web.ApiErrorCode;
import com.eidcricketfest.common.web.ApiProblemFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

@Component
public class RestAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    private final ApiProblemFactory problems;
    private final JsonMapper jsonMapper;

    public RestAuthenticationEntryPoint(
            ApiProblemFactory problems,
            JsonMapper jsonMapper
    ) {
        this.problems = problems;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        var problem =
                problems.create(
                        HttpStatus.UNAUTHORIZED,
                        ApiErrorCode.UNAUTHORIZED,
                        "Unauthorized",
                        "Authentication is required to access this resource.",
                        request
                );

        response.setStatus(
                HttpStatus.UNAUTHORIZED.value()
        );

        response.setContentType(
                MediaType.APPLICATION_PROBLEM_JSON_VALUE
        );

        jsonMapper.writeValue(
                response.getOutputStream(),
                problem
        );
    }
}

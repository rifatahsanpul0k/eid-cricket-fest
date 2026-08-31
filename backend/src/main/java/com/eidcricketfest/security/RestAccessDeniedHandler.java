package com.eidcricketfest.security;

import com.eidcricketfest.common.web.ApiErrorCode;
import com.eidcricketfest.common.web.ApiProblemFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

@Component
public class RestAccessDeniedHandler
        implements AccessDeniedHandler {

    private final ApiProblemFactory problems;
    private final JsonMapper jsonMapper;

    public RestAccessDeniedHandler(
            ApiProblemFactory problems,
            JsonMapper jsonMapper
    ) {
        this.problems = problems;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {

        var problem =
                problems.create(
                        HttpStatus.FORBIDDEN,
                        ApiErrorCode.FORBIDDEN,
                        "Forbidden",
                        "You do not have permission to access this resource.",
                        request
                );

        response.setStatus(
                HttpStatus.FORBIDDEN.value()
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

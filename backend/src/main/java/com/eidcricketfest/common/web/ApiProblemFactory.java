package com.eidcricketfest.common.web;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;

@Component
public class ApiProblemFactory {

    public ProblemDetail create(
            HttpStatus status,
            ApiErrorCode code,
            String title,
            String detail,
            HttpServletRequest request
    ) {

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        status,
                        detail
                );

        problem.setTitle(title);

        problem.setInstance(
                URI.create(
                        request.getRequestURI()
                )
        );

        problem.setProperty(
                "code",
                code.name()
        );

        problem.setProperty(
                "requestId",
                requestId(request)
        );

        problem.setProperty(
                "timestamp",
                Instant.now().toString()
        );

        return problem;
    }

    private String requestId(
            HttpServletRequest request
    ) {

        Object value =
                request.getAttribute(
                        RequestIdFilter.ATTRIBUTE_NAME
                );

        return value == null
                ? "unknown"
                : value.toString();
    }
}

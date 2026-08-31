package com.eidcricketfest.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME =
            "X-Request-Id";

    public static final String ATTRIBUTE_NAME =
            "requestId";

    public static final String MDC_KEY =
            "requestId";

    private static final Logger log =
            LoggerFactory.getLogger(
                    RequestIdFilter.class
            );

    private static final Pattern SAFE_REQUEST_ID =
            Pattern.compile(
                    "^[A-Za-z0-9._-]{1,100}$"
            );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestId =
                resolveRequestId(request);

        request.setAttribute(
                ATTRIBUTE_NAME,
                requestId
        );

        response.setHeader(
                HEADER_NAME,
                requestId
        );

        MDC.put(
                MDC_KEY,
                requestId
        );

        long startedAt =
                System.nanoTime();

        try {

            filterChain.doFilter(
                    request,
                    response
            );

        } finally {

            long durationMs =
                    (System.nanoTime() - startedAt)
                            / 1_000_000;

            log.info(
                    "HTTP {} {} -> {} ({} ms)",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs
            );

            MDC.remove(
                    MDC_KEY
            );
        }
    }

    private String resolveRequestId(
            HttpServletRequest request
    ) {

        String supplied =
                request.getHeader(
                        HEADER_NAME
                );

        if (supplied != null
                && SAFE_REQUEST_ID
                        .matcher(supplied)
                        .matches()) {

            return supplied;
        }

        return UUID.randomUUID()
                .toString();
    }
}

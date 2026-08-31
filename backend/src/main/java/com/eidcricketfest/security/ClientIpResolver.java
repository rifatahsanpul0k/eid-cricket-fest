package com.eidcricketfest.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    public String resolve(
            HttpServletRequest request
    ) {

        return request.getRemoteAddr();
    }
}

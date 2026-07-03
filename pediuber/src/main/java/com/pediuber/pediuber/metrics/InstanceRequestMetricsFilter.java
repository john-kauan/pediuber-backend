package com.pediuber.pediuber.metrics;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InstanceRequestMetricsFilter extends OncePerRequestFilter {

    private final PediUberMetricsService metricsService;

    public InstanceRequestMetricsFilter(PediUberMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String uri = request.getRequestURI();

        if (!uri.startsWith("/actuator")) {
            metricsService.incrementInstanceRequest();
        }

        filterChain.doFilter(request, response);
    }
}
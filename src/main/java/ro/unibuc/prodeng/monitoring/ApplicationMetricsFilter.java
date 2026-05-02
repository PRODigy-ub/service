package ro.unibuc.prodeng.monitoring;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApplicationMetricsFilter extends OncePerRequestFilter {

    private final MonitoringMetricsService monitoringMetricsService;

    public ApplicationMetricsFilter(MonitoringMetricsService monitoringMetricsService) {
        this.monitoringMetricsService = monitoringMetricsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startTime = System.nanoTime();
        monitoringMetricsService.incrementRequestsInFlight();
        try {
            filterChain.doFilter(request, response);
        } finally {
            monitoringMetricsService.decrementRequestsInFlight();
            Object bestMatchingPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            monitoringMetricsService.recordRequest(
                    request.getMethod(),
                    bestMatchingPattern instanceof String pattern ? pattern : request.getRequestURI(),
                    response.getStatus(),
                    System.nanoTime() - startTime);
        }
    }
}

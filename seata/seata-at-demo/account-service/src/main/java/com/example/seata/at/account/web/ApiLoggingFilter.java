package com.example.seata.at.account.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ApiLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(ApiLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api")) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrapped = WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
        if (wrapped == null) {
            wrapped = new ContentCachingRequestWrapper(request);
        }

        boolean logged = false;
        try {
            filterChain.doFilter(wrapped, response);
        } catch (Exception ex) {
            logRequest(wrapped, ex);
            logged = true;
            throw ex;
        } finally {
            if (!logged) {
                logRequest(wrapped, null);
            }
        }
    }

    private void logRequest(ContentCachingRequestWrapper request, Exception ex) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String paramStr = stringifyParams(request.getParameterMap());
        String body = extractBody(request);
        if (ex == null) {
            log.info("API Request -> {} {}{} params={} body={}", method, uri, query == null ? "" : ("?" + query), paramStr, body);
        } else {
            log.info("API Request (with exception) -> {} {}{} params={} body={} ex={}", method, uri, query == null ? "" : ("?" + query), paramStr, body, ex.toString());
        }
    }

    private String stringifyParams(Map<String, String[]> params) {
        if (params == null || params.isEmpty()) return "{}";
        return params.entrySet().stream()
                .map(e -> e.getKey() + "=" + Arrays.toString(e.getValue()))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private String extractBody(ContentCachingRequestWrapper request) {
        String contentType = request.getContentType();
        if (contentType == null) return "";
        if (!contentType.contains("application/json") && !contentType.contains("application/x-www-form-urlencoded")) {
            return "";
        }
        byte[] buf = request.getContentAsByteArray();
        if (buf.length == 0) return "";
        Charset cs = request.getCharacterEncoding() != null ? Charset.forName(request.getCharacterEncoding()) : StandardCharsets.UTF_8;
        return new String(buf, cs);
    }
}
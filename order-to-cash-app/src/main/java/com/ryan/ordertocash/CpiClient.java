package com.ryan.ordertocash;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

/**
 * Talks to SAP Integration Suite (CPI).
 *
 *  1. Fetches an OAuth token from the Process Integration Runtime token URL
 *     using client_credentials (clientid/clientsecret from the service key).
 *  2. POSTs the order XML to the deployed iFlow endpoint with that Bearer token.
 *
 * Token is cached until shortly before it expires.
 */
@Service
public class CpiClient {

    @Value("${cpi.token-url}")     private String tokenUrl;
    @Value("${cpi.client-id}")     private String clientId;
    @Value("${cpi.client-secret}") private String clientSecret;
    @Value("${cpi.iflow-url}")     private String iflowUrl;

    private final RestTemplate rest;
    private String cachedToken;
    private Instant expiresAt = Instant.EPOCH;

    public CpiClient() {
        // a bare RestTemplate has no timeouts, so a hung CPI call would hang us too
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        this.rest = new RestTemplate(factory);
    }

    /**
     * BTP service keys give the token URL both with and without the
     * /oauth/token suffix, and a trailing slash would otherwise produce
     * .../oauth/token//oauth/token. Package-private for the unit tests.
     */
    static String normalizeTokenUrl(String configured) {
        String base = configured;
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base.endsWith("/oauth/token") ? base : base + "/oauth/token";
    }

    private synchronized String getToken() {
        if (cachedToken != null && Instant.now().isBefore(expiresAt)) {
            return cachedToken;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");

        String url = normalizeTokenUrl(tokenUrl);

        ResponseEntity<Map<String, Object>> resp = rest.exchange(
                url, HttpMethod.POST, new HttpEntity<>(form, headers),
                new ParameterizedTypeReference<Map<String, Object>>() {});

        Map<String, Object> body = resp.getBody();
        if (body == null || body.get("access_token") == null) {
            throw new IllegalStateException("No access_token returned from " + url);
        }
        cachedToken = (String) body.get("access_token");
        int expiresIn = body.get("expires_in") != null
                ? ((Number) body.get("expires_in")).intValue() : 3600;
        // refresh a minute early
        expiresAt = Instant.now().plusSeconds(Math.max(60, expiresIn - 60));
        return cachedToken;
    }

    public String sendOrder(String orderXml) {
        try {
            return postOrder(orderXml);
        } catch (HttpClientErrorException.Unauthorized e) {
            // cached token may have been revoked early, drop it and retry once
            clearToken();
            return postOrder(orderXml);
        }
    }

    private String postOrder(String orderXml) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getToken());
        headers.setContentType(MediaType.APPLICATION_XML);

        ResponseEntity<String> resp =
                rest.exchange(iflowUrl, HttpMethod.POST, new HttpEntity<>(orderXml, headers), String.class);
        return resp.getBody();
    }

    private synchronized void clearToken() {
        cachedToken = null;
        expiresAt = Instant.EPOCH;
    }
}

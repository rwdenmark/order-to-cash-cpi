package com.ryan.ordertocash;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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

    private final RestTemplate rest = new RestTemplate();
    private String cachedToken;
    private Instant expiresAt = Instant.EPOCH;

    private synchronized String getToken() {
        if (cachedToken != null && Instant.now().isBefore(expiresAt)) {
            return cachedToken;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");

        String url = tokenUrl.endsWith("/oauth/token") ? tokenUrl : tokenUrl + "/oauth/token";

        ResponseEntity<Map> resp =
                rest.exchange(url, HttpMethod.POST, new HttpEntity<>(form, headers), Map.class);

        Map body = resp.getBody();
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
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getToken());
        headers.setContentType(MediaType.APPLICATION_XML);

        ResponseEntity<String> resp =
                rest.exchange(iflowUrl, HttpMethod.POST, new HttpEntity<>(orderXml, headers), String.class);
        return resp.getBody();
    }
}

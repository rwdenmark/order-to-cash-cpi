package com.ryan.ordertocash;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CpiClientTest {

    @Test
    void appendsPathWhenMissing() {
        assertEquals("https://x.authentication.eu10.hana.ondemand.com/oauth/token",
                CpiClient.normalizeTokenUrl("https://x.authentication.eu10.hana.ondemand.com"));
    }

    @Test
    void trailingSlashDoesNotDoubleThePath() {
        assertEquals("https://x.authentication.eu10.hana.ondemand.com/oauth/token",
                CpiClient.normalizeTokenUrl("https://x.authentication.eu10.hana.ondemand.com/"));
    }

    @Test
    void fullUrlPassesThrough() {
        assertEquals("https://x.authentication.eu10.hana.ondemand.com/oauth/token",
                CpiClient.normalizeTokenUrl("https://x.authentication.eu10.hana.ondemand.com/oauth/token"));
    }

    @Test
    void fullUrlWithTrailingSlashIsTrimmed() {
        assertEquals("https://x.authentication.eu10.hana.ondemand.com/oauth/token",
                CpiClient.normalizeTokenUrl("https://x.authentication.eu10.hana.ondemand.com/oauth/token/"));
    }
}

package com.reminder.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCheckServiceTest {

    @Test
    void normalizeStripsVAndSuffix() {
        assertEquals("1.2.3", UpdateCheckService.normalize("v1.2.3"));
        assertEquals("1.2.3", UpdateCheckService.normalize("V1.2.3"));
        assertEquals("1.2.3", UpdateCheckService.normalize("1.2.3-4-gabc"));
        assertEquals("", UpdateCheckService.normalize(null));
    }

    @Test
    void comparesSemanticVersions() {
        assertTrue(UpdateCheckService.compareVersions("1.2.0", "1.1.9") > 0);
        assertTrue(UpdateCheckService.compareVersions("1.1.9", "1.2.0") < 0);
        assertEquals(0, UpdateCheckService.compareVersions("1.2.0", "1.2"));
        assertTrue(UpdateCheckService.compareVersions("1.2.10", "1.2.9") > 0);
    }
}
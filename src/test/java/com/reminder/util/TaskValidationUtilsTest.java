package com.reminder.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskValidationUtilsTest {

    @Test
    void safeTaskNameReplacesBlankWithVisualEmpty() {
        assertEquals(TaskValidationUtils.VISUAL_EMPTY, TaskValidationUtils.getSafeTaskName(null));
        assertEquals(TaskValidationUtils.VISUAL_EMPTY, TaskValidationUtils.getSafeTaskName("   "));
        assertEquals("Задача", TaskValidationUtils.getSafeTaskName("  Задача  "));
    }

    @Test
    void filterTaskNamesDropsNullAndBlank() {
        assertTrue(TaskValidationUtils.filterTaskNames(null).isEmpty());
        List<String> input = Arrays.asList("A", "", "   ", null, "B");
        assertEquals(List.of("A", "B"), TaskValidationUtils.filterTaskNames(input));
    }
}
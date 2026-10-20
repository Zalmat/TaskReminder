package com.reminder.suggest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextNormalizerTest {

    @Test
    void matchFormLowercasesAndNormalizesYo() {
        assertEquals("елка", TextNormalizer.matchForm("Ёлка"));
        assertEquals("проект альфа", TextNormalizer.matchForm("Проект Альфа"));
    }

    @Test
    void matchFormPreservesLengthForHighlighting() {
        assertEquals("ёлка".length(), TextNormalizer.matchForm("Ёлка").length());
    }

    @Test
    void keyCollapsesWhitespace() {
        assertEquals("проект альфа", TextNormalizer.key("  Проект   Альфа  "));
    }
}

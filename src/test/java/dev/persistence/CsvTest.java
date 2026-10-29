package dev.persistence;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvTest {
    @Test
    void parsesQuotesEscapedQuotesAndEmbeddedNewlines() {
        String csv = "a,b,c\r\n\"x,1\",\"he said \"\"hi\"\"\",\"line1\nline2\"\r\n";
        List<List<String>> rows = Csv.parse(csv);
        assertEquals(2, rows.size());
        assertEquals(List.of("x,1", "he said \"hi\"", "line1\nline2"), rows.get(1));
    }

    @Test
    void stripsBomIgnoresBlankLinesAndKeepsEmptyFields() {
        List<List<String>> rows = Csv.parse("﻿a,b,c\n\n1,,3");
        assertEquals(2, rows.size());
        assertEquals("a", rows.get(0).get(0));
        assertEquals(List.of("1", "", "3"), rows.get(1));
    }

    @Test
    void writeThenParseRoundTrips() {
        List<List<String>> rows = List.of(List.of("n", "p"), List.of("a,b", "\"q\""), List.of("multi\nline", ""));
        assertEquals(rows, Csv.parse(Csv.write(rows)));
    }
}

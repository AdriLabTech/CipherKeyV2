package dev.persistence;

import java.util.ArrayList;
import java.util.List;

/** Lector y escritor CSV (RFC 4180): comillas, comillas escapadas y saltos de línea dentro de campos. */
public final class Csv {
    private Csv() {}

    public static List<List<String>> parse(String text) {
        if (text.startsWith("﻿")) text = text.substring(1);
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false, fieldStarted = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    field.append(c);
                }
                continue;
            }
            switch (c) {
                case '"' -> {
                    quoted = true;
                    fieldStarted = true;
                }
                case ',' -> {
                    row.add(field.toString());
                    field.setLength(0);
                    fieldStarted = true;
                }
                case '\r' -> { /* se trata con el \n siguiente */ }
                case '\n' -> {
                    endRow(rows, row, field, fieldStarted);
                    row = new ArrayList<>();
                    fieldStarted = false;
                }
                default -> {
                    field.append(c);
                    fieldStarted = true;
                }
            }
        }
        endRow(rows, row, field, fieldStarted);
        return rows;
    }

    private static void endRow(List<List<String>> rows, List<String> row, StringBuilder field, boolean fieldStarted) {
        if (!fieldStarted && row.isEmpty()) return; // línea vacía
        row.add(field.toString());
        field.setLength(0);
        rows.add(row);
    }

    public static String write(List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(escape(row.get(i)));
            }
            sb.append("\r\n");
        }
        return sb.toString();
    }

    private static String escape(String value) {
        if (value == null) return "";
        boolean needsQuotes = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
        return needsQuotes ? '"' + value.replace("\"", "\"\"") + '"' : value;
    }
}

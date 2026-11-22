package com.pulseiot.desktop;

import java.util.*;

/** Bộ đọc JSON nhỏ gọn (không cần thư viện ngoài) với API gần giống JsonNode. */
final class Jn {
    static final Jn NULL = new Jn(null);
    private final Object v;

    private Jn(Object v) { this.v = v; }

    static Jn parse(String s) {
        if (s == null || s.isBlank()) return NULL;
        try {
            return new Jn(new Parser(s).parse());
        } catch (RuntimeException e) {
            return NULL;
        }
    }

    Jn path(String key) {
        if (v instanceof Map<?, ?> m) {
            Object o = m.get(key);
            return o == null ? NULL : new Jn(o);
        }
        return NULL;
    }

    List<Jn> list() {
        List<Jn> out = new ArrayList<>();
        if (v instanceof List<?> l) for (Object o : l) out.add(o == null ? NULL : new Jn(o));
        return out;
    }

    List<Jn> list(String key) { return path(key).list(); }

    boolean isNull() { return v == null; }

    String text(String def) {
        if (v == null) return def;
        if (v instanceof Double d) {
            if (d == Math.rint(d) && Math.abs(d) < 1e15) return String.valueOf((long) (double) d);
            return String.valueOf(d);
        }
        return String.valueOf(v);
    }

    String text() { return text(""); }

    double dbl(double def) {
        if (v instanceof Double d) return d;
        if (v instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) { }
        }
        return def;
    }

    int intv(int def) { return v instanceof Double d ? (int) Math.round(d) : def; }

    long lng(long def) { return v instanceof Double d ? Math.round(d) : def; }

    boolean bool() { return v instanceof Boolean b && b; }

    static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : (s == null ? "" : s).toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> { if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c); }
            }
        }
        return sb.append('"').toString();
    }

    private static final class Parser {
        private final String s;
        private int i;

        Parser(String s) { this.s = s; }

        Object parse() {
            Object o = value();
            ws();
            return o;
        }

        private void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

        private Object value() {
            ws();
            if (i >= s.length()) throw new IllegalStateException("JSON rỗng");
            char c = s.charAt(i);
            return switch (c) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> { i += 4; yield Boolean.TRUE; }
                case 'f' -> { i += 5; yield Boolean.FALSE; }
                case 'n' -> { i += 4; yield null; }
                default -> number();
            };
        }

        private Map<String, Object> object() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++;
            ws();
            if (s.charAt(i) == '}') { i++; return m; }
            while (true) {
                ws();
                String k = string();
                ws();
                i++; // ':'
                m.put(k, value());
                ws();
                char c = s.charAt(i++);
                if (c == '}') return m;
            }
        }

        private List<Object> array() {
            List<Object> l = new ArrayList<>();
            i++;
            ws();
            if (s.charAt(i) == ']') { i++; return l; }
            while (true) {
                l.add(value());
                ws();
                char c = s.charAt(i++);
                if (c == ']') return l;
            }
        }

        private String string() {
            StringBuilder sb = new StringBuilder();
            i++;
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> { sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                        default -> sb.append(e);
                    }
                } else sb.append(c);
            }
        }

        private Double number() {
            int st = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            return Double.parseDouble(s.substring(st, i));
        }
    }
}

package com.notes.shared.json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonUtil {
    private JsonUtil() {
    }

    public static Object parse(String json) {
        return new Parser(json).parseValue();
    }

    public static String stringify(Object value) {
        StringBuilder builder = new StringBuilder();
        writeValue(builder, value);
        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asObject(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> asList(Object value) {
        if (value instanceof List<?> list) {
            return (List<Object>) list;
        }
        return new ArrayList<>();
    }

    public static String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public static boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(asString(value));
    }

    public static long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(asString(value));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static void writeValue(StringBuilder builder, Object value) {
        if (value == null) {
            builder.append("null");
            return;
        }
        if (value instanceof String string) {
            builder.append('"').append(escape(string)).append('"');
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
            return;
        }
        if (value instanceof Map<?, ?> map) {
            builder.append('{');
            Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<?, ?> entry = iterator.next();
                builder.append('"').append(escape(String.valueOf(entry.getKey()))).append('"').append(':');
                writeValue(builder, entry.getValue());
                if (iterator.hasNext()) {
                    builder.append(',');
                }
            }
            builder.append('}');
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            builder.append('[');
            Iterator<?> iterator = iterable.iterator();
            while (iterator.hasNext()) {
                writeValue(builder, iterator.next());
                if (iterator.hasNext()) {
                    builder.append(',');
                }
            }
            builder.append(']');
            return;
        }
        builder.append('"').append(escape(String.valueOf(value))).append('"');
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private static final class Parser {
        private final String input;
        private int index;

        private Parser(String input) {
            this.input = input == null ? "" : input.trim();
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= input.length()) {
                return null;
            }
            char current = input.charAt(index);
            if (current == '{') {
                return parseObject();
            }
            if (current == '[') {
                return parseArray();
            }
            if (current == '"') {
                return parseString();
            }
            if (current == 't' || current == 'f') {
                return parseBoolean();
            }
            if (current == 'n') {
                index += 4;
                return null;
            }
            return parseNumber();
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            index++;
            skipWhitespace();
            while (index < input.length() && input.charAt(index) != '}') {
                String key = parseString();
                skipWhitespace();
                index++;
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (index < input.length() && input.charAt(index) == ',') {
                    index++;
                    skipWhitespace();
                }
            }
            if (index < input.length()) {
                index++;
            }
            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            index++;
            skipWhitespace();
            while (index < input.length() && input.charAt(index) != ']') {
                list.add(parseValue());
                skipWhitespace();
                if (index < input.length() && input.charAt(index) == ',') {
                    index++;
                    skipWhitespace();
                }
            }
            if (index < input.length()) {
                index++;
            }
            return list;
        }

        private String parseString() {
            StringBuilder builder = new StringBuilder();
            index++;
            while (index < input.length()) {
                char current = input.charAt(index++);
                if (current == '"') {
                    break;
                }
                if (current == '\\' && index < input.length()) {
                    char escaped = input.charAt(index++);
                    switch (escaped) {
                        case '"' -> builder.append('"');
                        case '\\' -> builder.append('\\');
                        case '/' -> builder.append('/');
                        case 'b' -> builder.append('\b');
                        case 'f' -> builder.append('\f');
                        case 'n' -> builder.append('\n');
                        case 'r' -> builder.append('\r');
                        case 't' -> builder.append('\t');
                        case 'u' -> {
                            String hex = input.substring(index, Math.min(index + 4, input.length()));
                            builder.append((char) Integer.parseInt(hex, 16));
                            index += 4;
                        }
                        default -> builder.append(escaped);
                    }
                    continue;
                }
                builder.append(current);
            }
            return builder.toString();
        }

        private Boolean parseBoolean() {
            if (input.startsWith("true", index)) {
                index += 4;
                return true;
            }
            index += 5;
            return false;
        }

        private Number parseNumber() {
            int start = index;
            while (index < input.length()) {
                char current = input.charAt(index);
                if ((current >= '0' && current <= '9') || current == '-' || current == '+' || current == '.') {
                    index++;
                    continue;
                }
                break;
            }
            String value = input.substring(start, index);
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }
            return Long.parseLong(value);
        }

        private void skipWhitespace() {
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
        }
    }
}

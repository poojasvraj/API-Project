package com.example.menu;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Request-body and query-string validation. */
final class Validation {
    static final List<String> CATEGORIES = List.of("Starter", "MainCourse", "Dessert", "Beverage", "Snack");
    static final List<String> SORT_FIELDS = List.of("dishName", "price", "calories");
    static final List<String> ORDERS = List.of("asc", "desc");
    static final List<String> FIELDS =
            List.of("dishName", "restaurant", "category", "price", "calories", "isVegetarian");
    static final int MAX_LIMIT = 50;

    private static final Pattern POS_INT = Pattern.compile("^[0-9]+$");
    private static final Pattern NUMBER = Pattern.compile("^[+-]?([0-9]+\\.?[0-9]*|\\.[0-9]+)([eE][+-]?[0-9]+)?$");
    private static final BigInteger MAX_SAFE = BigInteger.valueOf((1L << 53) - 1);

    private Validation() {}

    record Result<T>(T value, String error) {
        static <T> Result<T> ok(T v) { return new Result<>(v, null); }
        static <T> Result<T> fail(String e) { return new Result<>(null, e); }
        boolean failed() { return error != null; }
    }

    /** Parsed GET /api/dishes query. Null fields mean "not supplied". */
    static final class ListQuery {
        int page = 1;
        int limit = 10;
        String order = "asc";
        String category;
        Boolean isVegetarian;
        Double minPrice;
        Double maxPrice;
        String search;
        String sort;
    }

    // ---- body ----

    private static Result<Object> validateField(String field, JsonNode v) {
        switch (field) {
            case "dishName": {
                if (!v.isTextual()) return Result.fail("dishName must be a string");
                String s = v.asText().trim();
                if (s.length() < 3 || s.length() > 60) return Result.fail("dishName must be 3-60 characters");
                return Result.ok(s);
            }
            case "restaurant": {
                if (!v.isTextual()) return Result.fail("restaurant must be a string");
                String s = v.asText().trim();
                if (s.length() < 2 || s.length() > 40) return Result.fail("restaurant must be 2-40 characters");
                return Result.ok(s);
            }
            case "category": {
                if (!v.isTextual() || !CATEGORIES.contains(v.asText())) {
                    return Result.fail("category must be one of " + String.join(", ", CATEGORIES));
                }
                return Result.ok(v.asText());
            }
            case "price": {
                if (!v.isNumber() || !Double.isFinite(v.asDouble())) return Result.fail("price must be a number");
                double p = v.asDouble();
                if (p <= 0) return Result.fail("Price must be greater than 0");
                if (p > 5000) return Result.fail("Price must be at most 5000");
                return Result.ok(v.numberValue());
            }
            case "calories": {
                if (!v.isNumber()) return Result.fail("calories must be an integer");
                double c = v.asDouble();
                if (!Double.isFinite(c) || c != Math.rint(c)) return Result.fail("calories must be an integer");
                if (c < 0 || c > 5000) return Result.fail("calories must be between 0 and 5000");
                return Result.ok((long) c);
            }
            default: { // isVegetarian
                if (!v.isBoolean()) return Result.fail("isVegetarian must be a boolean");
                return Result.ok(v.asBoolean());
            }
        }
    }

    /**
     * partial=false (POST/PUT): every field required.
     * partial=true  (PATCH): at least one recognized field, each supplied one validated.
     * Unknown fields (including id) are dropped.
     */
    static Result<Map<String, Object>> validateBody(JsonNode body, boolean partial) {
        if (body == null || !body.isObject()) return Result.fail("Request body must be a JSON object");
        Map<String, Object> data = new LinkedHashMap<>();
        int found = 0;
        for (String f : FIELDS) {
            if (!body.has(f)) {
                if (!partial) return Result.fail(f + " is required");
                continue;
            }
            found++;
            Result<Object> r = validateField(f, body.get(f));
            if (r.failed()) return Result.fail(r.error());
            data.put(f, r.value());
        }
        if (partial && found == 0) return Result.fail("At least one recognized field must be provided");
        return Result.ok(data);
    }

    // ---- query ----

    private static Double parseNumber(String s) {
        String t = s.trim();
        if (t.isEmpty() || !NUMBER.matcher(t).matches()) return null;
        double d = Double.parseDouble(t);
        return Double.isFinite(d) ? d : null;
    }

    static Result<ListQuery> parseListQuery(Map<String, List<String>> params) {
        Map<String, String> q = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : params.entrySet()) {
            if (e.getValue().size() != 1) return Result.fail("Invalid value for " + e.getKey());
            q.put(e.getKey(), e.getValue().get(0));
        }
        ListQuery out = new ListQuery();

        if (q.containsKey("page")) {
            String s = q.get("page");
            if (!POS_INT.matcher(s).matches()) return Result.fail("page must be a positive integer");
            BigInteger n = new BigInteger(s);
            if (n.signum() < 1 || n.compareTo(MAX_SAFE) > 0) return Result.fail("page must be a positive integer");
            out.page = n.min(BigInteger.valueOf(Integer.MAX_VALUE)).intValue();
        }
        if (q.containsKey("limit")) {
            String s = q.get("limit");
            if (!POS_INT.matcher(s).matches()) return Result.fail("limit must be a positive integer");
            BigInteger n = new BigInteger(s);
            if (n.signum() < 1) return Result.fail("limit must be a positive integer");
            if (n.compareTo(BigInteger.valueOf(MAX_LIMIT)) > 0) return Result.fail("limit must not exceed " + MAX_LIMIT);
            out.limit = n.intValue();
        }
        if (q.containsKey("category")) {
            if (!CATEGORIES.contains(q.get("category"))) {
                return Result.fail("category must be one of " + String.join(", ", CATEGORIES));
            }
            out.category = q.get("category");
        }
        if (q.containsKey("isVegetarian")) {
            String s = q.get("isVegetarian");
            if (!s.equals("true") && !s.equals("false")) return Result.fail("isVegetarian must be \"true\" or \"false\"");
            out.isVegetarian = s.equals("true");
        }
        if (q.containsKey("minPrice")) {
            Double n = parseNumber(q.get("minPrice"));
            if (n == null) return Result.fail("minPrice must be a valid number");
            out.minPrice = n;
        }
        if (q.containsKey("maxPrice")) {
            Double n = parseNumber(q.get("maxPrice"));
            if (n == null) return Result.fail("maxPrice must be a valid number");
            out.maxPrice = n;
        }
        if (q.containsKey("search")) {
            String s = q.get("search").trim().toLowerCase();
            if (!s.isEmpty()) out.search = s;
        }
        if (q.containsKey("sort")) {
            if (!SORT_FIELDS.contains(q.get("sort"))) {
                return Result.fail("sort must be one of " + String.join(", ", SORT_FIELDS));
            }
            out.sort = q.get("sort");
        }
        if (q.containsKey("order")) {
            if (!ORDERS.contains(q.get("order"))) return Result.fail("order must be \"asc\" or \"desc\"");
            out.order = q.get("order");
        }
        return Result.ok(out);
    }
}

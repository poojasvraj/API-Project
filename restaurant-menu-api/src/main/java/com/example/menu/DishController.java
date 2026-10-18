package com.example.menu;

import com.example.menu.Validation.ListQuery;
import com.example.menu.Validation.Result;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Dish CRUD plus list/filter/search/sort/paginate, backed by an in-memory list (empty on every start). */
@RestController
@RequestMapping("/api/dishes")
public class DishController {

    private final ObjectMapper mapper = new ObjectMapper();
    private final List<Map<String, Object>> dishes = new ArrayList<>(); // creation order
    private long nextId = 1;

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }

    private static ResponseEntity<Map<String, Object>> notFound() {
        return error(HttpStatus.NOT_FOUND, "Record not found");
    }

    /** Parse the raw body ourselves so malformed JSON yields our JSON 400. Empty body counts as {}. */
    private JsonNode parse(String raw) throws Exception {
        if (raw == null || raw.isBlank()) return mapper.createObjectNode();
        return mapper.readTree(raw);
    }

    private int find(String id) {
        for (int i = 0; i < dishes.size(); i++) {
            if (String.valueOf(dishes.get(i).get("id")).equals(id)) return i;
        }
        return -1;
    }

    private static Map<String, Object> withId(Object id, Map<String, Object> data) {
        Map<String, Object> dish = new LinkedHashMap<>();
        dish.put("id", id);
        dish.putAll(data);
        return dish;
    }

    // A. Create
    @PostMapping
    public synchronized ResponseEntity<?> create(@RequestBody(required = false) String raw) throws Exception {
        Result<Map<String, Object>> r = Validation.validateBody(parse(raw), false);
        if (r.failed()) return error(HttpStatus.BAD_REQUEST, r.error());
        Map<String, Object> dish = withId(nextId++, r.value());
        dishes.add(dish);
        return ResponseEntity.status(HttpStatus.CREATED).body(dish);
    }

    // C. List / filter / search / sort / paginate
    @GetMapping
    public synchronized ResponseEntity<?> list(HttpServletRequest request) {
        Map<String, List<String>> params = new LinkedHashMap<>();
        request.getParameterMap().forEach((k, v) -> params.put(k, List.of(v)));
        Result<ListQuery> p = Validation.parseListQuery(params);
        if (p.failed()) return error(HttpStatus.BAD_REQUEST, p.error());
        ListQuery q = p.value();

        // 1. filters
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> d : dishes) {
            double price = ((Number) d.get("price")).doubleValue();
            if (q.category != null && !d.get("category").equals(q.category)) continue;
            if (q.isVegetarian != null && !d.get("isVegetarian").equals(q.isVegetarian)) continue;
            if (q.minPrice != null && price < q.minPrice) continue;
            if (q.maxPrice != null && price > q.maxPrice) continue;
            result.add(d);
        }

        // 2. search (dishName OR restaurant, case-insensitive partial)
        if (q.search != null) {
            result.removeIf(d -> !((String) d.get("dishName")).toLowerCase().contains(q.search)
                    && !((String) d.get("restaurant")).toLowerCase().contains(q.search));
        }

        // 3. sort (stable; ties keep creation order)
        if (q.sort != null) {
            Comparator<Map<String, Object>> cmp = q.sort.equals("dishName")
                    ? Comparator.comparing(d -> ((String) d.get("dishName")).toLowerCase())
                    : Comparator.comparingDouble(d -> ((Number) d.get(q.sort)).doubleValue());
            result.sort(q.order.equals("desc") ? cmp.reversed() : cmp);
        }

        // 4. paginate
        int total = result.size();
        long totalPages = total == 0 ? 0 : (total + q.limit - 1) / q.limit;
        long start = (long) (q.page - 1) * q.limit;
        List<Map<String, Object>> data = start >= total
                ? List.of()
                : result.subList((int) start, (int) Math.min(total, start + q.limit));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data", data);
        body.put("page", q.page);
        body.put("limit", q.limit);
        body.put("total", total);
        body.put("totalPages", totalPages);
        return ResponseEntity.ok(body);
    }

    // B. Get one
    @GetMapping("/{id}")
    public synchronized ResponseEntity<?> getOne(@PathVariable String id) {
        int i = find(id);
        if (i == -1) return notFound();
        return ResponseEntity.ok(dishes.get(i));
    }

    // D. Replace
    @PutMapping("/{id}")
    public synchronized ResponseEntity<?> replace(@PathVariable String id,
                                                  @RequestBody(required = false) String raw) throws Exception {
        int i = find(id);
        if (i == -1) return notFound();
        Result<Map<String, Object>> r = Validation.validateBody(parse(raw), false);
        if (r.failed()) return error(HttpStatus.BAD_REQUEST, r.error());
        Map<String, Object> dish = withId(dishes.get(i).get("id"), r.value());
        dishes.set(i, dish);
        return ResponseEntity.ok(dish);
    }

    // E. Partial update
    @PatchMapping("/{id}")
    public synchronized ResponseEntity<?> update(@PathVariable String id,
                                                 @RequestBody(required = false) String raw) throws Exception {
        int i = find(id);
        if (i == -1) return notFound();
        Result<Map<String, Object>> r = Validation.validateBody(parse(raw), true);
        if (r.failed()) return error(HttpStatus.BAD_REQUEST, r.error());
        Map<String, Object> dish = new LinkedHashMap<>(dishes.get(i));
        dish.putAll(r.value());
        dishes.set(i, dish);
        return ResponseEntity.ok(dish);
    }

    // F. Delete
    @DeleteMapping("/{id}")
    public synchronized ResponseEntity<?> delete(@PathVariable String id) {
        int i = find(id);
        if (i == -1) return notFound();
        dishes.remove(i);
        return ResponseEntity.noContent().build();
    }
}

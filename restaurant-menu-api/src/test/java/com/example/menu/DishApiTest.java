package com.example.menu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class DishApiTest {

    @Autowired
    MockMvc mvc;

    static final String MARGHERITA =
            "{\"dishName\":\"Margherita\",\"restaurant\":\"Luigi\",\"category\":\"MainCourse\","
                    + "\"price\":12.5,\"calories\":800,\"isVegetarian\":true}";

    private void create(String json) throws Exception {
        mvc.perform(post("/api/dishes").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
    }

    @Test
    void createAndGetOne() throws Exception {
        mvc.perform(post("/api/dishes").contentType(MediaType.APPLICATION_JSON).content(MARGHERITA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.dishName").value("Margherita"));
        mvc.perform(get("/api/dishes/1")).andExpect(status().isOk()).andExpect(jsonPath("$.restaurant").value("Luigi"));
        mvc.perform(get("/api/dishes/99")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Record not found"));
    }

    @Test
    void validationErrors() throws Exception {
        mvc.perform(post("/api/dishes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("dishName is required"));
        mvc.perform(post("/api/dishes").contentType(MediaType.APPLICATION_JSON).content("{bad"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Malformed JSON"));
        mvc.perform(post("/api/dishes").contentType(MediaType.APPLICATION_JSON)
                        .content(MARGHERITA.replace("12.5", "0")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Price must be greater than 0"));
    }

    @Test
    void putPatchDelete() throws Exception {
        create(MARGHERITA);
        mvc.perform(patch("/api/dishes/1").contentType(MediaType.APPLICATION_JSON).content("{\"price\":9.99}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.price").value(9.99))
                .andExpect(jsonPath("$.dishName").value("Margherita"));
        mvc.perform(patch("/api/dishes/1").contentType(MediaType.APPLICATION_JSON).content("{\"foo\":1}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/dishes/1").contentType(MediaType.APPLICATION_JSON)
                        .content(MARGHERITA.replace("Margherita", "Pasta Alfredo")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.dishName").value("Pasta Alfredo"));
        mvc.perform(delete("/api/dishes/1")).andExpect(status().isNoContent());
        mvc.perform(delete("/api/dishes/1")).andExpect(status().isNotFound());
    }

    @Test
    void listFilterSortPaginate() throws Exception {
        create(MARGHERITA);
        create(MARGHERITA.replace("Margherita", "Brownie").replace("MainCourse", "Dessert")
                .replace("12.5", "5").replace("true", "false"));
        create(MARGHERITA.replace("Margherita", "Cola").replace("MainCourse", "Beverage").replace("12.5", "2"));

        mvc.perform(get("/api/dishes")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3)).andExpect(jsonPath("$.totalPages").value(1));
        mvc.perform(get("/api/dishes?isVegetarian=false")).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/dishes?search=LUIGI")).andExpect(jsonPath("$.total").value(3));
        mvc.perform(get("/api/dishes?minPrice=3&maxPrice=6")).andExpect(jsonPath("$.data[0].dishName").value("Brownie"));
        mvc.perform(get("/api/dishes?sort=price&order=desc")).andExpect(jsonPath("$.data[0].dishName").value("Margherita"));
        mvc.perform(get("/api/dishes?sort=price")).andExpect(jsonPath("$.data[0].dishName").value("Cola"));
        mvc.perform(get("/api/dishes?limit=2&page=2")).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/api/dishes?limit=51")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/dishes?page=1&page=2")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/dishes?sort=nope")).andExpect(status().isBadRequest());
    }

    @Test
    void unknownRouteIsJson404() throws Exception {
        mvc.perform(get("/nope")).andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("Route not found"));
        mvc.perform(delete("/api/dishes")).andExpect(status().isNotFound());
    }
}

package com.rjproj.reserba.controller;

import com.rjproj.reserba.config.ItemConfig;
import com.rjproj.reserba.dto.ItemDto;
import com.rjproj.reserba.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @WebMvcTest loads ONLY the web layer (controllers, filters, MockMvc).
// It does NOT load services, repositories, or the database.
// This makes it fast and focused purely on testing the controller behavior.
@WebMvcTest(ItemController.class)
class ItemControllerTest {

    // MockMvc lets us simulate real HTTP requests (GET, POST, PUT, DELETE)
    // without actually starting a server — it's like a fake browser for tests
    @Autowired
    private MockMvc mockMvc;

    // @MockitoBean replaces the real ItemService with a fake (mock) version
    // so we control exactly what it returns without touching any real business logic
    @MockitoBean
    private ItemService itemService;

    // @MockitoBean also replaces ItemConfig with a fake so we can control
    // what getName() and getValue() return in the hello endpoint test
    @MockitoBean
    private ItemConfig itemConfig;

    // Shared test data — created fresh before each test via @BeforeEach
    private UUID itemId;
    private ItemDto itemDto;

    // @BeforeEach runs before every test method
    // We set up a reusable itemId and itemDto so we don't repeat this in every test
    @BeforeEach
    void setUp() {
        itemId = UUID.randomUUID();
        itemDto = new ItemDto(
                itemId.toString(),
                "Test Item",
                "Test Description",
                new BigDecimal("9.99"),
                "http://example.com/image.png",
                "Test Category"
        );
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/items/hello
    // -------------------------------------------------------------------------

    @Test
    void helloWorld_shouldReturnGreetingWithConfigValues() throws Exception {
        // Arrange: tell the mock config what values to return when the controller calls them
        when(itemConfig.getName()).thenReturn("Reserba");
        when(itemConfig.getValue()).thenReturn("42");

        // Act & Assert: perform a GET request and verify the response string
        // The controller builds "Hello World Reserba " + name + " " + value
        mockMvc.perform(get("/api/v1/items/hello"))
                .andExpect(status().isOk()) // Expect HTTP 200
                .andExpect(content().string("Hello World Reserba Reserba 42"));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/items/{id}
    // -------------------------------------------------------------------------

    @Test
    void getItem_whenItemExists_shouldReturnOkWithItem() throws Exception {
        // Arrange: tell the mock service to return our test itemDto when getItem is called
        when(itemService.getItem(itemId)).thenReturn(itemDto);

        // Act & Assert: perform GET with the itemId in the URL path
        mockMvc.perform(get("/api/v1/items/{id}", itemId))
                .andExpect(status().isOk()) // Expect HTTP 200
                // jsonPath checks specific fields inside the JSON response body
                .andExpect(jsonPath("$.id").value(itemId.toString()))
                .andExpect(jsonPath("$.name").value("Test Item"))
                .andExpect(jsonPath("$.description").value("Test Description"))
                .andExpect(jsonPath("$.price").value(9.99))
                .andExpect(jsonPath("$.category").value("Test Category"));

        // Verify the controller actually delegated to the service with the correct id
        verify(itemService).getItem(itemId);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/items
    // -------------------------------------------------------------------------

    @Test
    void getItems_shouldReturnOkWithListOfItems() throws Exception {
        // Arrange: create a second item so we can verify a list is returned
        ItemDto secondItem = new ItemDto(
                UUID.randomUUID().toString(),
                "Second Item",
                "Second Description",
                new BigDecimal("19.99"),
                "http://example.com/image2.png",
                "Second Category"
        );

        // Tell the mock service to return both items as a list
        when(itemService.getItems()).thenReturn(List.of(itemDto, secondItem));

        // Act & Assert: perform GET and verify the JSON array contains 2 items
        mockMvc.perform(get("/api/v1/items"))
                .andExpect(status().isOk()) // Expect HTTP 200
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Test Item"))
                .andExpect(jsonPath("$[1].name").value("Second Item"));

        verify(itemService).getItems();
    }

    @Test
    void getItems_whenNoItems_shouldReturnEmptyList() throws Exception {
        // Arrange: simulate the service returning an empty list (no items in the database)
        when(itemService.getItems()).thenReturn(List.of());

        // Act & Assert: the response should still be 200 OK but with an empty JSON array
        mockMvc.perform(get("/api/v1/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/items
    // -------------------------------------------------------------------------

    @Test
    void saveItem_whenValidRequest_shouldReturnCreatedWithItem() throws Exception {
        // Arrange: the JSON body the client sends in the POST request
        // id is null because the item hasn't been saved yet — the database assigns the id
        String requestBody = """
                {
                    "id": null,
                    "name": "New Item",
                    "description": "New Description",
                    "price": 5.99,
                    "imageUrl": "http://example.com/new.png",
                    "category": "New Category"
                }
                """;

        // Tell the mock service: whatever ItemDto it receives, return our pre-built itemDto
        // any(ItemDto.class) means "I don't care about the exact input, just match any ItemDto"
        when(itemService.saveItem(any(ItemDto.class))).thenReturn(itemDto);

        // Act & Assert: perform POST with the JSON body
        mockMvc.perform(post("/api/v1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated()) // Expect HTTP 201 Created
                .andExpect(jsonPath("$.id").value(itemId.toString()))
                .andExpect(jsonPath("$.name").value("Test Item"));

        verify(itemService).saveItem(any(ItemDto.class));
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/items/{id}
    // -------------------------------------------------------------------------

    @Test
    void updateItem_whenItemExists_shouldReturnOkWithUpdatedItem() throws Exception {
        // Arrange: the JSON body the client sends — includes the id and updated values
        // .formatted(itemId) injects the actual UUID into the JSON string
        String requestBody = """
                {
                    "id": "%s",
                    "name": "Updated Item",
                    "description": "Updated Description",
                    "price": 14.99,
                    "imageUrl": "http://example.com/updated.png",
                    "category": "Updated Category"
                }
                """.formatted(itemId);

        ItemDto updatedDto = new ItemDto(
                itemId.toString(),
                "Updated Item",
                "Updated Description",
                new BigDecimal("14.99"),
                "http://example.com/updated.png",
                "Updated Category"
        );

        // eq(itemId) means the id in the URL must EXACTLY match itemId
        // any(ItemDto.class) means any ItemDto body is acceptable
        when(itemService.updateItem(eq(itemId), any(ItemDto.class))).thenReturn(updatedDto);

        // Act & Assert
        mockMvc.perform(put("/api/v1/items/{id}", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk()) // Expect HTTP 200
                .andExpect(jsonPath("$.id").value(itemId.toString()))
                .andExpect(jsonPath("$.name").value("Updated Item"));

        verify(itemService).updateItem(eq(itemId), any(ItemDto.class));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/items/{id}
    // -------------------------------------------------------------------------

    @Test
    void deleteItem_whenItemExists_shouldReturnNoContent() throws Exception {
        // Arrange: deleteItem returns void, so we use doNothing() to stub it
        // This tells Mockito: when deleteItem is called, do nothing (don't throw an exception)
        doNothing().when(itemService).deleteItem(itemId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/items/{id}", itemId))
                .andExpect(status().isNoContent()); // Expect HTTP 204 No Content

        verify(itemService).deleteItem(itemId);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/items/filter?category=Electronics&category=Clothing
    // Filters items by one or more categories passed as repeated query params
    // -------------------------------------------------------------------------

    @Test
    void filterByCategories_whenMatchExists_shouldReturnFilteredItems() throws Exception {
        ItemDto electronicsItem = new ItemDto(
                UUID.randomUUID().toString(), "Wireless Mouse", "Ergonomic", new BigDecimal("29.99"), "url", "Electronics"
        );

        // Tell the mock service to return items matching the given categories
        when(itemService.filterItemsByCategories(any())).thenReturn(List.of(electronicsItem));

        // Act & Assert: pass categories as repeated query params — ?category=Electronics&category=Clothing
        mockMvc.perform(get("/api/v1/items/filter")
                        .param("category", "Electronics")
                        .param("category", "Clothing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Wireless Mouse"))
                .andExpect(jsonPath("$[0].category").value("Electronics"));

        verify(itemService).filterItemsByCategories(any());
    }

    @Test
    void filterByCategories_whenNoMatch_shouldReturnEmptyList() throws Exception {
        // Arrange: send a category that has no matching items
        when(itemService.filterItemsByCategories(any())).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/v1/items/filter")
                        .param("category", "Furniture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(itemService).filterItemsByCategories(any());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/items/categories
    // Returns a distinct list of all category names
    // -------------------------------------------------------------------------

    @Test
    void getCategories_shouldReturnListOfCategoryNames() throws Exception {
        // Arrange: tell the mock service to return a list of category names
        when(itemService.getCategories()).thenReturn(List.of("Electronics", "Clothing", "Appliances"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/items/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value("Electronics"))
                .andExpect(jsonPath("$[1]").value("Clothing"))
                .andExpect(jsonPath("$[2]").value("Appliances"));

        verify(itemService).getCategories();
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/items/categories/{category}/price-sum
    // Returns the total price sum for a single category as a path variable
    // -------------------------------------------------------------------------

    @Test
    void getSumPriceOfCategory_shouldReturnCorrectSum() throws Exception {
        // Arrange: tell the mock service to return a total price for "Electronics"
        when(itemService.getSumPriceOfCategory("Electronics")).thenReturn(new BigDecimal("119.98"));

        // Act & Assert: category is now a path variable, not a query param
        mockMvc.perform(get("/api/v1/items/categories/{category}/price-sum", "Electronics"))
                .andExpect(status().isOk())
                .andExpect(content().string("119.98"));

        verify(itemService).getSumPriceOfCategory("Electronics");
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/items/categories/price-summary
    // Returns a map of every category with its total price sum
    // -------------------------------------------------------------------------

    @Test
    void getSumPriceOfCategories_shouldReturnMapWithTotals() throws Exception {
        // Arrange: tell the mock service to return a map of category totals
        when(itemService.getSumPriceOfCategories()).thenReturn(Map.of(
                "Electronics", new BigDecimal("119.98"),
                "Clothing", new BigDecimal("19.99")
        ));

        // Act & Assert: jsonPath("$.Electronics") reads the "Electronics" key from the JSON object
        mockMvc.perform(get("/api/v1/items/categories/price-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Electronics").value(119.98))
                .andExpect(jsonPath("$.Clothing").value(19.99));

        verify(itemService).getSumPriceOfCategories();
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/items/search
    // Unified search endpoint — handles name search and price+category search
    // ?name=mouse  OR  ?minPrice=50&category=Electronics  OR both
    // -------------------------------------------------------------------------

    @Test
    void searchItems_whenNameProvided_shouldDelegateToNameSearch() throws Exception {
        // Arrange: when name is provided, the controller delegates to searchItemByName
        when(itemService.searchItemByName("mouse")).thenReturn(List.of(itemDto));

        // Act & Assert: pass name as a query param
        mockMvc.perform(get("/api/v1/items/search")
                        .param("name", "mouse")) // ?name=mouse
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Item"));

        verify(itemService).searchItemByName("mouse");
    }

    @Test
    void searchItems_whenNameNotProvided_shouldDelegateToPriceAndCategorySearch() throws Exception {
        // Arrange: when name is absent, the controller delegates to searchItems(price, category)
        when(itemService.searchItems(any(BigDecimal.class), eq("Electronics")))
                .thenReturn(List.of(itemDto));

        // Act & Assert: pass minPrice and category only — no name param
        mockMvc.perform(get("/api/v1/items/search")
                        .param("minPrice", "50.00")
                        .param("category", "Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Item"));

        verify(itemService).searchItems(any(BigDecimal.class), eq("Electronics"));
    }

    @Test
    void searchItems_whenNoParamsProvided_shouldUseDefaults() throws Exception {
        // Arrange: no params — minPrice defaults to 0, category is null, name is null
        // The controller routes to searchItems(0, null)
        when(itemService.searchItems(any(BigDecimal.class), eq(null)))
                .thenReturn(List.of(itemDto));

        // Act & Assert: no query params at all
        mockMvc.perform(get("/api/v1/items/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(itemService).searchItems(any(BigDecimal.class), eq(null));
    }

    @Test
    void searchItems_whenNoMatch_shouldReturnEmptyList() throws Exception {
        // Arrange: simulate no items matching the filters
        when(itemService.searchItems(any(BigDecimal.class), eq("Furniture")))
                .thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/v1/items/search")
                        .param("minPrice", "0")
                        .param("category", "Furniture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(itemService).searchItems(any(BigDecimal.class), eq("Furniture"));
    }
}
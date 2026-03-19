package com.rjproj.reserba.controller;

import com.rjproj.reserba.config.ItemConfig;
import com.rjproj.reserba.dto.ItemDto;
import com.rjproj.reserba.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;


    private final ItemConfig itemConfig;

    @GetMapping(path="/hello")
    public String helloWorld() {
        return "Hello World Reserba "  + itemConfig.getName() + " " +  itemConfig.getValue();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemDto> getItem(@PathVariable(value="id") UUID id) {
        return ResponseEntity.ok(itemService.getItem(id));
    }

    @GetMapping
    public ResponseEntity<List<ItemDto>> getItems() {
        return ResponseEntity.ok(itemService.getItems());
    }

    @PostMapping
    public ResponseEntity<ItemDto> saveItem(@RequestBody ItemDto itemDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itemService.saveItem(itemDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItemDto> updateItem(@PathVariable(value="id") UUID id, @RequestBody ItemDto itemDto) {
        return ResponseEntity.ok(itemService.updateItem(id, itemDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable(value="id") UUID id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    // --------------------- NON CRUD -------------------------
    // Returns items filtered by one or more categories
    // Uses GET with repeated query params: ?category=Electronics&category=Clothing
    // Changed from POST /filterItemsByCategories — no verbs in URLs, GET is correct for filtering
    @GetMapping("/filter")
    public ResponseEntity<List<ItemDto>> filterByCategories(
            @RequestParam(name = "category") List<String> categories
    ) {
        return ResponseEntity.ok(itemService.filterItemsByCategories(categories));
    }

    // Returns a distinct list of all category names
    // GET /api/v1/items/categories
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(itemService.getCategories());
    }

    // Returns the total price sum for a single category
    // Changed from /getSumPriceOfCategory — no verbs, kebab-case, resource-style URL
    // GET /api/v1/items/categories/{category}/price-sum
    @GetMapping("/categories/{category}/price-sum")
    public ResponseEntity<BigDecimal> getSumPriceOfCategory(
            @PathVariable(name = "category") String category
    ) {
        return ResponseEntity.ok(itemService.getSumPriceOfCategory(category));
    }

    // Returns a map of every category and its total price sum
    // Changed from /getSumPriceOfCategories — no verbs, kebab-case, resource-style URL
    // GET /api/v1/items/categories/price-summary
    @GetMapping("/categories/price-summary")
    public ResponseEntity<Map<String, BigDecimal>> getSumPriceOfCategories() {
        return ResponseEntity.ok(itemService.getSumPriceOfCategories());
    }

    // Search items by name keyword (case-insensitive, partial match)
    // Merged into /search — having a separate /searchItemByName endpoint is redundant
    // GET /api/v1/items/search?name=mouse
    // GET /api/v1/items/search?name=mouse&category=Electronics&minPrice=10.00
    @GetMapping("/search")
    public ResponseEntity<List<ItemDto>> searchItems(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "minPrice", defaultValue = "0") BigDecimal minPrice,
            @RequestParam(name = "category", required = false) String category
    ) {
        // If a name is provided, delegate to name search
        // Otherwise delegate to price+category search
        if (name != null && !name.isEmpty()) {
            return ResponseEntity.ok(itemService.searchItemByName(name));
        }
        return ResponseEntity.ok(itemService.searchItems(minPrice, category));
    }




}

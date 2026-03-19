package com.rjproj.reserba.repository;

import com.rjproj.reserba.model.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// @DataJpaTest spins up a lightweight Spring context that loads ONLY the JPA layer
// (repositories, entities). It does NOT load controllers or services.
// It automatically uses an in-memory H2 database instead of your real PostgreSQL database.
@DataJpaTest
@TestPropertySource(properties = {
        // Disable Flyway because our migration scripts use PostgreSQL-specific functions
        // (like gen_random_uuid()) that H2 does not understand
        "spring.flyway.enabled=false",
        // Instead, let Hibernate build the schema directly from our @Entity classes
        // "create-drop" means: create tables before tests, drop them after tests finish
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ItemRepositoryTest {

    // Spring injects the real ItemRepository backed by the in-memory H2 database
    @Autowired
    private ItemRepository itemRepository;

    // These fields hold our test data so every test method can access them
    private Item electronics1;
    private Item electronics2;
    private Item clothing1;

    // @BeforeEach runs before EVERY single test method
    // This ensures each test starts with a clean, known set of data
    @BeforeEach
    void setUp() {
        // Wipe the database clean so previous test data doesn't bleed into this test
        itemRepository.deleteAll();

        // Create the first Electronics item
        electronics1 = new Item();
        electronics1.setName("Wireless Mouse");
        electronics1.setDescription("Ergonomic wireless mouse");
        electronics1.setPrice(new BigDecimal("29.99"));
        electronics1.setImageUrl("http://example.com/mouse.png");
        electronics1.setCategory("Electronics");

        // Create the second Electronics item
        electronics2 = new Item();
        electronics2.setName("Mechanical Keyboard");
        electronics2.setDescription("RGB mechanical keyboard");
        electronics2.setPrice(new BigDecimal("89.99"));
        electronics2.setImageUrl("http://example.com/keyboard.png");
        electronics2.setCategory("Electronics");

        // Create a Clothing item — different category to test category filtering
        clothing1 = new Item();
        clothing1.setName("Blue T-Shirt");
        clothing1.setDescription("Comfortable cotton t-shirt");
        clothing1.setPrice(new BigDecimal("19.99"));
        clothing1.setImageUrl("http://example.com/tshirt.png");
        clothing1.setCategory("Clothing");

        // Save all three items to the in-memory H2 database at once
        itemRepository.saveAll(List.of(electronics1, electronics2, clothing1));
    }

    // -------------------------------------------------------------------------
    // findByCategoryAndPriceLessThanEqual
    // This method finds items that belong to a category AND cost up to a given price
    // -------------------------------------------------------------------------

    @Test
    void findByCategoryAndPriceLessThanEqual_whenMatchExists_shouldReturnItems() {
        // Search for Electronics items priced at $50 or less
        // Only "Wireless Mouse" ($29.99) qualifies — "Mechanical Keyboard" ($89.99) does not
        List<Item> result = itemRepository.findByCategoryAndPriceLessThanEqual(
                "Electronics", new BigDecimal("50.00")
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Wireless Mouse");
    }

    @Test
    void findByCategoryAndPriceLessThanEqual_whenNoMatch_shouldReturnEmpty() {
        // Search for Electronics items priced at $10 or less
        // Neither item is that cheap, so we expect an empty list
        List<Item> result = itemRepository.findByCategoryAndPriceLessThanEqual(
                "Electronics", new BigDecimal("10.00")
        );

        assertThat(result).isEmpty();
    }

    @Test
    void findByCategoryAndPriceLessThanEqual_whenExactPrice_shouldIncludeItem() {
        // Search using the EXACT price of "Wireless Mouse" ($29.99)
        // "LessThanEqual" means it should still be included at the exact boundary
        List<Item> result = itemRepository.findByCategoryAndPriceLessThanEqual(
                "Electronics", new BigDecimal("29.99")
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Wireless Mouse");
    }

    // -------------------------------------------------------------------------
    // findByNameContainingIgnoreCase
    // This method searches for items whose name contains a keyword (case-insensitive)
    // -------------------------------------------------------------------------

    @Test
    void findByNameContainingIgnoreCase_whenMatchExists_shouldReturnItems() {
        // Search with lowercase "mouse" — should match "Wireless Mouse"
        List<Item> result = itemRepository.findByNameContainingIgnoreCase("mouse");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Wireless Mouse");
    }

    @Test
    void findByNameContainingIgnoreCase_whenUpperCase_shouldReturnItems() {
        // Search with uppercase "MOUSE" — should still match "Wireless Mouse"
        // This proves the search is truly case-insensitive
        List<Item> result = itemRepository.findByNameContainingIgnoreCase("MOUSE");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Wireless Mouse");
    }

    @Test
    void findByNameContainingIgnoreCase_whenPartialMatch_shouldReturnMultiple() {
        // Search for "keyboard" — should match only "Mechanical Keyboard"
        List<Item> result = itemRepository.findByNameContainingIgnoreCase("keyboard");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Mechanical Keyboard");
    }

    @Test
    void findByNameContainingIgnoreCase_whenNoMatch_shouldReturnEmpty() {
        // Search for a keyword that no item name contains — expects empty result
        List<Item> result = itemRepository.findByNameContainingIgnoreCase("xyz");

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // findAllCategories
    // This method returns a distinct list of all category names in the database
    // -------------------------------------------------------------------------

    @Test
    void findAllCategories_shouldReturnDistinctCategories() {
        // We have 2 Electronics and 1 Clothing item, but DISTINCT means
        // "Electronics" should only appear once in the result
        List<String> categories = itemRepository.findAllCategories();

        assertThat(categories)
                .hasSize(2)
                .containsExactlyInAnyOrder("Electronics", "Clothing");
    }

    @Test
    void findAllCategories_whenNoCategoryIsNull_shouldNotIncludeNull() {
        // Save an extra item with a NULL category
        Item nullCategoryItem = new Item();
        nullCategoryItem.setName("Unknown");
        nullCategoryItem.setPrice(new BigDecimal("5.00"));
        nullCategoryItem.setCategory(null); // Intentionally no category
        itemRepository.save(nullCategoryItem);

        List<String> categories = itemRepository.findAllCategories();

        // The query uses WHERE category IS NOT NULL, so null should be excluded
        assertThat(categories).doesNotContainNull();
    }

    // -------------------------------------------------------------------------
    // findByNameAndCategory
    // This method finds items matching BOTH a name keyword AND an exact category
    // -------------------------------------------------------------------------

    @Test
    void findByNameAndCategory_whenMatchExists_shouldReturnItems() {
        // "mouse" is in "Wireless Mouse" AND it belongs to "Electronics" — match!
        List<Item> result = itemRepository.findByNameAndCategory("mouse", "Electronics");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Wireless Mouse");
    }

    @Test
    void findByNameAndCategory_whenCategoryMismatch_shouldReturnEmpty() {
        // "mouse" matches the name but "Clothing" is the wrong category — no match
        List<Item> result = itemRepository.findByNameAndCategory("mouse", "Clothing");

        assertThat(result).isEmpty();
    }

    @Test
    void findByNameAndCategory_whenNameMismatch_shouldReturnEmpty() {
        // "laptop" doesn't match any item name, even though "Electronics" is valid
        List<Item> result = itemRepository.findByNameAndCategory("laptop", "Electronics");

        assertThat(result).isEmpty();
    }

    @Test
    void findByNameAndCategory_isCaseInsensitiveForName() {
        // Searching with uppercase "MOUSE" should still find "Wireless Mouse"
        // because the query uses LOWER() on both sides
        List<Item> result = itemRepository.findByNameAndCategory("MOUSE", "Electronics");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Wireless Mouse");
    }

    // -------------------------------------------------------------------------
    // searchItems
    // This method is a flexible search — both name and category are optional (nullable)
    // If a param is null, that filter is skipped entirely
    // -------------------------------------------------------------------------

    @Test
    void searchItems_whenBothParamsProvided_shouldFilterByBoth() {
        // Both filters active: name contains "mouse" AND category is "Electronics"
        List<Item> result = itemRepository.searchItems("mouse", "Electronics");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Wireless Mouse");
    }

    @Test
    void searchItems_whenOnlyNameProvided_shouldFilterByNameOnly() {
        // Only name filter active: category is null so it is ignored
        // "shirt" matches "Blue T-Shirt" regardless of its category
        List<Item> result = itemRepository.searchItems("shirt", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Blue T-Shirt");
    }

    @Test
    void searchItems_whenOnlyCategoryProvided_shouldFilterByCategoryOnly() {
        // Only category filter active: name is null so it is ignored
        // All "Electronics" items should be returned
        List<Item> result = itemRepository.searchItems(null, "Electronics");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Item::getName)
                .containsExactlyInAnyOrder("Wireless Mouse", "Mechanical Keyboard");
    }

    @Test
    void searchItems_whenBothParamsNull_shouldReturnAllItems() {
        // Both filters are null — no filtering at all, so all 3 items are returned
        List<Item> result = itemRepository.searchItems(null, null);

        assertThat(result).hasSize(3);
    }

    @Test
    void searchItems_whenNoMatchFound_shouldReturnEmpty() {
        // "laptop" doesn't exist in any name, and no item is in "Clothing" with that name
        List<Item> result = itemRepository.searchItems("laptop", "Clothing");

        assertThat(result).isEmpty();
    }
}
package com.rjproj.reserba.service;

import com.rjproj.reserba.dto.ItemDto;
import com.rjproj.reserba.exception.ItemNotFoundException;
import com.rjproj.reserba.mapper.ItemMapper;
import com.rjproj.reserba.model.Item;
import com.rjproj.reserba.repository.ItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class) activates Mockito for this test class
// It processes @Mock and @InjectMocks annotations automatically
@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    // Mockito creates a fake ItemRepository — no real database is used
    @Mock
    private ItemRepository itemRepository;

    // Mockito creates a fake ItemMapper — no real mapping logic runs
    @Mock
    private ItemMapper itemMapper;

    // Mockito injects the mocks above into the real ItemService we are testing
    @InjectMocks
    private ItemService itemService;

    // -------------------------------------------------------------------------
    // getItem
    // -------------------------------------------------------------------------

    @Test
    void getItem_whenIdExists_shouldReturnItemDto() {
        // Arrange
        UUID id = UUID.randomUUID();
        Item itemEntity = new Item(); // Represents the entity returned by the database

        // Since ItemDto is a RECORD, we must use the full constructor
        ItemDto expectedDto = new ItemDto(
                id.toString(),
                "Espresso",
                "Strong coffee",
                new BigDecimal("3.50"),
                "url",
                "Drinks"
        );

        // Tell the mock repository: when findById is called with this id, return the entity
        when(itemRepository.findById(id)).thenReturn(Optional.of(itemEntity));

        // Tell the mock mapper: when mapToDto is called with the entity, return our expected DTO
        when(itemMapper.mapToDto(itemEntity)).thenReturn(expectedDto);

        // Act
        ItemDto result = itemService.getItem(id);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(id.toString());
        assertThat(result.name()).isEqualTo("Espresso");

        // Verify the chain of command — did the service call the right methods?
        verify(itemRepository).findById(id);
        verify(itemMapper).mapToDto(itemEntity);
    }

    @Test
    void getItem_whenIdDoesNotExist_shouldThrowItemNotFoundException() {
        // Arrange
        UUID id = UUID.randomUUID();

        // Simulate the database finding nothing for this id
        when(itemRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        // Verify the service throws our custom exception when the item is not found
        assertThatThrownBy(() -> itemService.getItem(id))
                .isInstanceOf(ItemNotFoundException.class);

        // IMPORTANT: Verify the service stopped early
        // Since no item was found, the mapper should NEVER be called
        verify(itemRepository).findById(id);
        verifyNoInteractions(itemMapper);
    }

    // -------------------------------------------------------------------------
    // getItems
    // -------------------------------------------------------------------------

    @Test
    void getItems_whenItemsExist_shouldReturnListOfItemDtos() {
        // Arrange
        // Use mock(Item.class) instead of new Item() so Mockito can distinguish
        // between the two instances — plain new Item() objects with no fields set
        // look identical to Mockito and cause the wrong stub to be matched
        Item item1 = mock(Item.class); // First entity from the database
        Item item2 = mock(Item.class); // Second entity from the database

        // The DTOs we expect the mapper to produce for each entity
        ItemDto dto1 = new ItemDto(UUID.randomUUID().toString(), "Item 1", "Desc 1", new BigDecimal("10.00"), "url1", "Cat1");
        ItemDto dto2 = new ItemDto(UUID.randomUUID().toString(), "Item 2", "Desc 2", new BigDecimal("20.00"), "url2", "Cat2");

        // Tell the mock repository to return both entities when findAll is called
        when(itemRepository.findAll()).thenReturn(List.of(item1, item2));

        // Tell the mock mapper what to return for each individual entity
        when(itemMapper.mapToDto(item1)).thenReturn(dto1);
        when(itemMapper.mapToDto(item2)).thenReturn(dto2);

        // Act
        List<ItemDto> result = itemService.getItems();

        // Assert
        // Use containsExactlyInAnyOrder because the list order is not guaranteed
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ItemDto::name)
                .containsExactlyInAnyOrder("Item 1", "Item 2");

        // Verify that findAll was called once and the mapper was called for each item
        verify(itemRepository).findAll();
        verify(itemMapper).mapToDto(item1);
        verify(itemMapper).mapToDto(item2);
    }

    @Test
    void getItems_whenNoItems_shouldReturnEmptyList() {
        // Arrange
        // Simulate the database having no items
        when(itemRepository.findAll()).thenReturn(List.of());

        // Act
        List<ItemDto> result = itemService.getItems();

        // Assert
        assertThat(result).isEmpty();

        // Verify findAll was called but the mapper was never touched (nothing to map)
        verify(itemRepository).findAll();
        verifyNoInteractions(itemMapper);
    }

    // -------------------------------------------------------------------------
    // saveItem
    // -------------------------------------------------------------------------

    @Test
    void saveItem_shouldReturnSavedItemDto() {
        // Arrange
        // The incoming DTO from the user — no id yet since it hasn't been saved
        ItemDto inputDto = new ItemDto(
                null,
                "Espresso Machine",
                "Professional grade",
                new BigDecimal("599.99"),
                "http://image.url",
                "Appliances"
        );

        Item itemEntity = new Item();  // The entity the mapper produces from the input DTO
        Item savedEntity = new Item(); // The entity returned by the repository after saving

        // The DTO we expect back — now has an id assigned by the database
        ItemDto expectedDto = new ItemDto(
                UUID.randomUUID().toString(),
                "Espresso Machine",
                "Professional grade",
                new BigDecimal("599.99"),
                "http://image.url",
                "Appliances"
        );

        // Tell the mock mapper: convert the input DTO into an entity
        when(itemMapper.mapToEntity(inputDto)).thenReturn(itemEntity);

        // Tell the mock repository: save the entity and return the saved version
        when(itemRepository.save(itemEntity)).thenReturn(savedEntity);

        // Tell the mock mapper: convert the saved entity back into a DTO
        when(itemMapper.mapToDto(savedEntity)).thenReturn(expectedDto);

        // Act
        ItemDto result = itemService.saveItem(inputDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isNotNull(); // The saved item should now have an id
        assertThat(result.name()).isEqualTo("Espresso Machine");

        // Verify the three-step save process was followed
        verify(itemMapper).mapToEntity(inputDto);   // Step 1: Convert DTO to entity
        verify(itemRepository).save(itemEntity);    // Step 2: Persist the entity
        verify(itemMapper).mapToDto(savedEntity);   // Step 3: Convert saved entity back to DTO
    }

    // -------------------------------------------------------------------------
    // updateItem
    // -------------------------------------------------------------------------

    @Test
    void updateItem_whenIdExists_shouldReturnUpdatedItemDto() {
        // Arrange
        UUID id = UUID.randomUUID();

        // The new data sent by the user
        ItemDto inputDto = new ItemDto(
                id.toString(),
                "Updated Name",
                "New Desc",
                new BigDecimal("99.99"),
                "new-url",
                "New Cat"
        );

        Item existingItem = new Item(); // The current entity in the database
        Item savedItem = new Item();    // The entity after being saved with new values

        // The DTO the user gets back after a successful update — same values as inputDto
        ItemDto expectedDto = inputDto;

        // Tell the mock repository: find the existing item by id
        when(itemRepository.findById(id)).thenReturn(Optional.of(existingItem));

        // Tell the mock repository: save the updated entity and return the saved version
        when(itemRepository.save(existingItem)).thenReturn(savedItem);

        // Tell the mock mapper: convert the saved entity to a DTO
        when(itemMapper.mapToDto(savedItem)).thenReturn(expectedDto);

        // Act
        ItemDto result = itemService.updateItem(id, inputDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Updated Name");
        assertThat(result.id()).isEqualTo(id.toString());

        // Verify the four-step update process was followed in order
        verify(itemRepository).findById(id);                            // Step 1: Find existing item
        verify(itemMapper).updateEntityFromDto(inputDto, existingItem); // Step 2: Merge new values into entity
        verify(itemRepository).save(existingItem);                      // Step 3: Persist updated entity
        verify(itemMapper).mapToDto(savedItem);                         // Step 4: Convert result to DTO
    }

    @Test
    void updateItem_whenIdDoesNotExist_shouldThrowItemNotFoundException() {
        // Arrange
        UUID id = UUID.randomUUID();
        ItemDto inputDto = new ItemDto(id.toString(), "Name", "Desc", BigDecimal.ONE, "url", "Cat");

        // Simulate the database finding nothing for this id
        when(itemRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> itemService.updateItem(id, inputDto))
                .isInstanceOf(ItemNotFoundException.class);

        // Verify the service stopped — it should NOT attempt to save or map anything
        verify(itemRepository).findById(id);
        verifyNoMoreInteractions(itemMapper, itemRepository);
    }

    // -------------------------------------------------------------------------
    // deleteItem
    // -------------------------------------------------------------------------

    @Test
    void deleteItem_whenIdExists_shouldDeleteItem() {
        // Arrange
        UUID id = UUID.randomUUID();
        Item existingItem = new Item(); // The entity we expect to be deleted

        // Tell the mock repository: return the item when searched by id
        when(itemRepository.findById(id)).thenReturn(Optional.of(existingItem));

        // Act
        // deleteItem returns void so we just call it and verify behavior
        itemService.deleteItem(id);

        // Assert & Verify
        verify(itemRepository).findById(id);         // Step 1: Did it check the item exists?
        verify(itemRepository).delete(existingItem); // Step 2: Did it actually call delete?
    }

    @Test
    void deleteItem_whenIdDoesNotExist_shouldThrowItemNotFoundException() {
        // Arrange
        UUID id = UUID.randomUUID();

        // Simulate the database finding nothing for this id
        when(itemRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> itemService.deleteItem(id))
                .isInstanceOf(ItemNotFoundException.class);

        // Crucial safety check: verify delete was NEVER called
        // We must not delete something that doesn't exist
        verify(itemRepository).findById(id);
        verify(itemRepository, never()).delete(any(Item.class));
    }

    // -------------------------------------------------------------------------
    // filterItemsByCategories
    // The service loads ALL items and then filters in memory by the given categories
    // -------------------------------------------------------------------------

    @Test
    void filterItemsByCategories_whenMatchExists_shouldReturnFilteredItems() {
        // Arrange
        // Create two items in different categories
        Item electronicsItem = new Item();
        electronicsItem.setCategory("Electronics");

        Item clothingItem = new Item();
        clothingItem.setCategory("Clothing");

        ItemDto electronicsDto = new ItemDto(UUID.randomUUID().toString(), "Mouse", "Wireless", new BigDecimal("29.99"), "url", "Electronics");
        ItemDto clothingDto = new ItemDto(UUID.randomUUID().toString(), "T-Shirt", "Cotton", new BigDecimal("19.99"), "url", "Clothing");

        // Tell the mock repository to return both items
        when(itemRepository.findAll()).thenReturn(List.of(electronicsItem, clothingItem));

        // Tell the mock mapper what to return for the Electronics item only
        // (Clothing item should be filtered out before mapping)
        when(itemMapper.mapToDto(electronicsItem)).thenReturn(electronicsDto);

        // Act: filter by "Electronics" only
        List<ItemDto> result = itemService.filterItemsByCategories(List.of("Electronics"));

        // Assert: only the Electronics item should be returned
        assertThat(result).hasSize(1);
        assertThat(result.get(0).category()).isEqualTo("Electronics");

        verify(itemRepository).findAll();
    }

    @Test
    void filterItemsByCategories_whenNoMatch_shouldReturnEmptyList() {
        // Arrange
        Item electronicsItem = new Item();
        electronicsItem.setCategory("Electronics");

        // Tell the mock repository to return one item
        when(itemRepository.findAll()).thenReturn(List.of(electronicsItem));

        // Act: filter by a category that doesn't exist in the data
        List<ItemDto> result = itemService.filterItemsByCategories(List.of("Furniture"));

        // Assert: no items match so the result should be empty
        assertThat(result).isEmpty();

        verify(itemRepository).findAll();
        // The mapper should never be called since no items passed the filter
        verifyNoInteractions(itemMapper);
    }

    // -------------------------------------------------------------------------
    // getCategories
    // Delegates directly to the repository's findAllCategories query
    // -------------------------------------------------------------------------

    @Test
    void getCategories_shouldReturnListOfCategoryNames() {
        // Arrange
        // Tell the mock repository to return a list of distinct category names
        when(itemRepository.findAllCategories()).thenReturn(List.of("Electronics", "Clothing", "Appliances"));

        // Act
        List<String> result = itemService.getCategories();

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result).containsExactlyInAnyOrder("Electronics", "Clothing", "Appliances");

        // Verify the service delegated to the repository
        verify(itemRepository).findAllCategories();
    }

    @Test
    void getCategories_whenNoCategories_shouldReturnEmptyList() {
        // Arrange
        when(itemRepository.findAllCategories()).thenReturn(List.of());

        // Act
        List<String> result = itemService.getCategories();

        // Assert
        assertThat(result).isEmpty();
        verify(itemRepository).findAllCategories();
    }

    // -------------------------------------------------------------------------
    // getSumPriceOfCategory
    // Loads ALL items and sums the price of items that match the given category
    // -------------------------------------------------------------------------

    @Test
    void getSumPriceOfCategory_whenItemsExist_shouldReturnCorrectSum() {
        // Arrange
        // Create two Electronics items and one Clothing item
        Item electronics1 = new Item();
        electronics1.setCategory("Electronics");
        electronics1.setPrice(new BigDecimal("29.99"));

        Item electronics2 = new Item();
        electronics2.setCategory("Electronics");
        electronics2.setPrice(new BigDecimal("89.99"));

        Item clothing1 = new Item();
        clothing1.setCategory("Clothing");
        clothing1.setPrice(new BigDecimal("19.99"));

        // Tell the mock repository to return all three items
        when(itemRepository.findAll()).thenReturn(List.of(electronics1, electronics2, clothing1));

        // Act: get the sum for "Electronics" only
        BigDecimal result = itemService.getSumPriceOfCategory("Electronics");

        // Assert: 29.99 + 89.99 = 119.98
        assertThat(result).isEqualByComparingTo(new BigDecimal("119.98"));

        verify(itemRepository).findAll();
    }

    @Test
    void getSumPriceOfCategory_whenNoItemsInCategory_shouldReturnZero() {
        // Arrange
        Item clothing1 = new Item();
        clothing1.setCategory("Clothing");
        clothing1.setPrice(new BigDecimal("19.99"));

        when(itemRepository.findAll()).thenReturn(List.of(clothing1));

        // Act: search for a category that has no items
        BigDecimal result = itemService.getSumPriceOfCategory("Electronics");

        // Assert: no items matched so the sum should be ZERO (the reduce starting value)
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);

        verify(itemRepository).findAll();
    }

    // -------------------------------------------------------------------------
    // getSumPriceOfCategories
    // Returns a Map where each key is a category name and the value is the total price
    // -------------------------------------------------------------------------

    @Test
    void getSumPriceOfCategories_shouldReturnMapWithCorrectTotalsPerCategory() {
        // Arrange
        Item electronics1 = new Item();
        electronics1.setCategory("Electronics");
        electronics1.setPrice(new BigDecimal("29.99"));

        Item electronics2 = new Item();
        electronics2.setCategory("Electronics");
        electronics2.setPrice(new BigDecimal("89.99"));

        Item clothing1 = new Item();
        clothing1.setCategory("Clothing");
        clothing1.setPrice(new BigDecimal("19.99"));

        when(itemRepository.findAll()).thenReturn(List.of(electronics1, electronics2, clothing1));

        // Act
        Map<String, BigDecimal> result = itemService.getSumPriceOfCategories();

        // Assert: two categories in the map
        assertThat(result).hasSize(2);

        // Electronics: 29.99 + 89.99 = 119.98
        assertThat(result.get("Electronics")).isEqualByComparingTo(new BigDecimal("119.98"));

        // Clothing: 19.99
        assertThat(result.get("Clothing")).isEqualByComparingTo(new BigDecimal("19.99"));

        verify(itemRepository).findAll();
    }

    @Test
    void getSumPriceOfCategories_whenNoItems_shouldReturnEmptyMap() {
        // Arrange
        when(itemRepository.findAll()).thenReturn(List.of());

        // Act
        Map<String, BigDecimal> result = itemService.getSumPriceOfCategories();

        // Assert: no items means no categories in the map
        assertThat(result).isEmpty();

        verify(itemRepository).findAll();
    }

    // -------------------------------------------------------------------------
    // searchItemByName
    // Delegates to the repository's case-insensitive name search
    // -------------------------------------------------------------------------

    @Test
    void searchItemByName_whenMatchExists_shouldReturnMatchingItems() {
        // Arrange
        Item item = new Item();
        ItemDto dto = new ItemDto(UUID.randomUUID().toString(), "Wireless Mouse", "Ergonomic", new BigDecimal("29.99"), "url", "Electronics");

        // Tell the mock repository: return the item when searching by name keyword
        when(itemRepository.findByNameContainingIgnoreCase("mouse")).thenReturn(List.of(item));

        // Tell the mock mapper: convert the entity to our expected DTO
        when(itemMapper.mapToDto(item)).thenReturn(dto);

        // Act
        List<ItemDto> result = itemService.searchItemByName("mouse");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Wireless Mouse");

        verify(itemRepository).findByNameContainingIgnoreCase("mouse");
        verify(itemMapper).mapToDto(item);
    }

    @Test
    void searchItemByName_whenNoMatch_shouldReturnEmptyList() {
        // Arrange
        // Simulate no items matching the search keyword
        when(itemRepository.findByNameContainingIgnoreCase("laptop")).thenReturn(List.of());

        // Act
        List<ItemDto> result = itemService.searchItemByName("laptop");

        // Assert
        assertThat(result).isEmpty();

        verify(itemRepository).findByNameContainingIgnoreCase("laptop");
        // Mapper should never be called since there are no items to convert
        verifyNoInteractions(itemMapper);
    }

    // -------------------------------------------------------------------------
    // searchItems
    // Searches items by category AND maximum price using the repository
    // -------------------------------------------------------------------------

    @Test
    void searchItems_whenMatchExists_shouldReturnFilteredItems() {
        // Arrange
        Item item = new Item();
        ItemDto dto = new ItemDto(UUID.randomUUID().toString(), "Wireless Mouse", "Ergonomic", new BigDecimal("29.99"), "url", "Electronics");

        // Tell the mock repository: return the item when filtering by category and price
        // Note: the service passes (category, price) to the repository method
        when(itemRepository.findByCategoryAndPriceLessThanEqual("Electronics", new BigDecimal("50.00")))
                .thenReturn(List.of(item));

        // Tell the mock mapper: convert the entity to our expected DTO
        when(itemMapper.mapToDto(item)).thenReturn(dto);

        // Act: the service method signature is searchItems(price, category)
        List<ItemDto> result = itemService.searchItems(new BigDecimal("50.00"), "Electronics");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Wireless Mouse");

        verify(itemRepository).findByCategoryAndPriceLessThanEqual("Electronics", new BigDecimal("50.00"));
        verify(itemMapper).mapToDto(item);
    }

    @Test
    void searchItems_whenNoMatch_shouldReturnEmptyList() {
        // Arrange
        // Simulate no items matching the category and price filter
        when(itemRepository.findByCategoryAndPriceLessThanEqual("Electronics", new BigDecimal("5.00")))
                .thenReturn(List.of());

        // Act
        List<ItemDto> result = itemService.searchItems(new BigDecimal("5.00"), "Electronics");

        // Assert
        assertThat(result).isEmpty();

        verify(itemRepository).findByCategoryAndPriceLessThanEqual("Electronics", new BigDecimal("5.00"));
        // Mapper should never be called since there are no items to convert
        verifyNoInteractions(itemMapper);
    }
}
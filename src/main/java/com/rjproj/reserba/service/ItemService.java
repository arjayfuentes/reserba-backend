package com.rjproj.reserba.service;

import com.rjproj.reserba.dto.ItemDto;
import com.rjproj.reserba.exception.ItemNotFoundException;
import com.rjproj.reserba.mapper.ItemMapper;
import com.rjproj.reserba.model.Item;
import com.rjproj.reserba.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    private final ItemMapper itemMapper;

    public ItemDto getItem(UUID id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException(id));

        return itemMapper.mapToDto(item);
    }

    public List<ItemDto> getItems() {
        List<Item> items = itemRepository.findAll();

        return items.stream()
                .map(itemMapper::mapToDto)
                .toList();

//        return items.stream()
//                .map(item -> itemMapper.mapToDto(item))
//                .toList();
    }

    public ItemDto saveItem(ItemDto itemDto) {
        Item newItem = itemMapper.mapToEntity(itemDto);

        Item savedItem = itemRepository.save(newItem);

        return itemMapper.mapToDto(savedItem);
    }

    public ItemDto updateItem(UUID id, ItemDto itemDto) {
        Item existingItem = itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException(id));

        itemMapper.updateEntityFromDto(itemDto, existingItem);

        Item updatedItem = itemRepository.save(existingItem);

        return itemMapper.mapToDto(updatedItem);
    }

    public void deleteItem(UUID id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException(id));

        itemRepository.delete(item);
    }


    // --------------------- NON CRUD -------------------------
    public List<ItemDto> filterItemsByCategories(List<String> categories) {
        List<Item> items = itemRepository.findAll();

        return items.stream()
                .filter(item -> categories.contains(item.getCategory()))
                .map(itemMapper::mapToDto).toList();
    }

    public List<String> getCategories() {
        return itemRepository.findAllCategories();
    }

    public BigDecimal getSumPriceOfCategory(String category) {
        List<Item> items = itemRepository.findAll();
        return items.stream()
                .filter(item -> category.equals(item.getCategory()))
                .map(Item::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
//        return items.stream()
//                .filter(item -> category.equals(item.getCategory()))
//                .mapToDouble(Item::getPrice)
//                .sum();
    }

    public Map<String, BigDecimal> getSumPriceOfCategories() {
        List<Item> items = itemRepository.findAll();
        Map<String, BigDecimal> totals = items.stream()
                .collect(Collectors.groupingBy(
                        Item::getCategory,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Item::getPrice,
                                BigDecimal::add
                        )
                        // Collectors.summingDouble(Product::getPrice)  // if double
                ));
        return totals;
    }

    // ---------------------
    public List<ItemDto> searchItemByName(String name) {
        List<Item> items = itemRepository.findByNameContainingIgnoreCase(name);

        return items.stream()
                .map(itemMapper::mapToDto).toList();

    }

    public List<ItemDto> searchItems(BigDecimal price, String category) {
        List<Item> items = itemRepository.findByCategoryAndPriceLessThanEqual(category,price);

        return items.stream()
                .map(itemMapper::mapToDto).toList();
    }


}

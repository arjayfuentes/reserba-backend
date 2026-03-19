package com.rjproj.reserba.mapper;

import com.rjproj.reserba.dto.ItemDto;
import com.rjproj.reserba.model.Item;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ItemMapper {

    public ItemDto mapToDto(Item item) {
        return new ItemDto(
                item.getId().toString(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.getImageUrl(),
                item.getCategory()
        );
    }

    public Item mapToEntity(ItemDto dto) {
        Item item = new Item();

        // Convert String ID back to UUID if it exists
        if (dto.id() != null && !dto.id().isEmpty()) {
            item.setId(UUID.fromString(dto.id()));
        }

        item.setName(dto.name());
        item.setDescription(dto.description());
        item.setPrice(dto.price());
        item.setImageUrl(dto.imageUrl());
        item.setCategory(dto.category());

        return item;
    }

    public void updateEntityFromDto(ItemDto dto, Item entity) {
        if (dto == null || entity == null) return;

        // Map ID only if the entity doesn't have one yet
        if (entity.getId() == null && dto.id() != null && !dto.id().isEmpty()) {
            entity.setId(UUID.fromString(dto.id()));
        }

        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setPrice(dto.price());
        entity.setImageUrl(dto.imageUrl());
        entity.setCategory(dto.category());
    }
}

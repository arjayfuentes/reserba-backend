package com.rjproj.reserba.dto;

import java.math.BigDecimal;

public record ItemDto(
        String id,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        String category
) {
}

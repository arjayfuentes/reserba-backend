package com.rjproj.reserba.repository;

import com.rjproj.reserba.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface ItemRepository extends JpaRepository<Item, UUID> {

    List<Item> findByCategoryAndPriceLessThanEqual(String category, BigDecimal price);

    List<Item> findByNameContainingIgnoreCase(String name);

    @Query("SELECT DISTINCT i.category FROM Item i WHERE i.category IS NOT NULL")
    List<String> findAllCategories();

    @Query("SELECT i FROM Item i WHERE " +
            "LOWER(i.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
            "AND i.category = :category")
    List<Item> findByNameAndCategory(@Param("name") String name, @Param("category") String category);

    @Query("SELECT i FROM Item i WHERE " +
            "(:name IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND (:category IS NULL OR i.category = :category)")
    List<Item> searchItems(@Param("name") String name, @Param("category") String category);
}

package com.healthdiet.repository;

import com.healthdiet.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Integer> {

    List<Recipe> findByCategory(String category);

    List<Recipe> findByNameContaining(String keyword);
}
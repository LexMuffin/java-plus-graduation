package ru.yandex.practicum.category.service;

import org.springframework.data.domain.Pageable;
import ru.yandex.practicum.category.dto.CategoryDto;
import ru.yandex.practicum.category.dto.NewCategoryDto;

import java.util.List;

public interface CategoryService {

    CategoryDto addCategory(NewCategoryDto newCategoryDto);

    CategoryDto updateCategory(Long catId, CategoryDto categoryDto);

    void deleteCategory(Long catId);

    List<CategoryDto> getCategories(Pageable pageable);

    CategoryDto getCategory(Long catId);
}

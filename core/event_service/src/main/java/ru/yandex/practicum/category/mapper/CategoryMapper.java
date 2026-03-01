package ru.yandex.practicum.category.mapper;

import org.mapstruct.Mapper;
import ru.yandex.practicum.dto.category.CategoryDto;
import ru.yandex.practicum.dto.category.NewCategoryDto;
import ru.yandex.practicum.category.model.Category;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryDto toDto(Category category);

    List<CategoryDto> toDto(List<Category> categories);

    Category toEntity(NewCategoryDto newCategoryDto);

    Category toEntity(Long id, CategoryDto categoryDto);
}

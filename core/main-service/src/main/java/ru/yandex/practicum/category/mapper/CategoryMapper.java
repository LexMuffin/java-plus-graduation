package ru.yandex.practicum.category.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import ru.yandex.practicum.category.dto.CategoryDto;
import ru.yandex.practicum.category.dto.NewCategoryDto;
import ru.yandex.practicum.category.model.Category;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryMapper INSTANCE = Mappers.getMapper(CategoryMapper.class);

    CategoryDto toDto(Category category);

    List<CategoryDto> toDto(List<Category> categories);

    Category toEntity(NewCategoryDto newCategoryDto);

    Category toEntity(Long id, CategoryDto categoryDto);
}

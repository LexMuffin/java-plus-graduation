package ru.yandex.practicum.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.client.event.EventServiceClient;
import ru.yandex.practicum.dto.category.CategoryDto;
import ru.yandex.practicum.dto.category.NewCategoryDto;
import ru.yandex.practicum.mapper.CategoryMapper;
import ru.yandex.practicum.model.Category;
import ru.yandex.practicum.repository.CategoryRepository;
import ru.yandex.practicum.exception.ConflictException;
import ru.yandex.practicum.exception.NotFoundException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryServiceImpl implements CategoryService {

    CategoryRepository categoryRepository;
    CategoryMapper categoryMapper;
    EventServiceClient eventServiceClient;

    @Override
    @Transactional
    public CategoryDto addCategory(NewCategoryDto newCategoryDto) {
        log.info("Добавление: {}", newCategoryDto.getName());

        Category category = categoryMapper.toEntity(newCategoryDto);

        try {
            Category savedCategory = categoryRepository.save(category);
            log.info("Добавлено, id: {}", savedCategory.getId());
            return categoryMapper.toDto(savedCategory);
        } catch (DataIntegrityViolationException e) {
            log.error("Уже существует: {}", newCategoryDto.getName());
            throw new ConflictException("Имя категории должно быть уникальным");
        }
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, CategoryDto categoryDto) {
        log.info("Обновление id: {}", catId);

        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> {
                    log.error("Не найдена id: {}", catId);
                    return new NotFoundException("Категория не найдена");
                });

        category.setName(categoryDto.getName());

        try {
            Category updatedCategory = categoryRepository.save(category);
            log.info("Обновлено id: {}", updatedCategory.getId());
            return categoryMapper.toDto(updatedCategory);
        } catch (DataIntegrityViolationException e) {
            log.error("Уже существует: {}", categoryDto.getName());
            throw new ConflictException("Имя категории должно быть уникальным");
        }
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        log.info("Удаление id: {}", catId);

        if (!categoryRepository.existsById(catId)) {
            log.error("Не найдена id: {}", catId);
            throw new NotFoundException("Категория не найдена");
        }

        if (eventServiceClient.existsEventsByCategoryId(catId)) {
            log.error("Категория с id {} используется в событиях", catId);
            throw new ConflictException("Категория не может быть удалена, так как с ней связаны события");
        }

        categoryRepository.deleteById(catId);
        log.info("Удалено id: {}", catId);
    }

    @Override
    public List<CategoryDto> getCategories(Pageable pageable) {
        int from = pageable.getPageNumber() * pageable.getPageSize();
        log.info("Запрос: from={}, size={}", from, pageable.getPageSize());

        return categoryMapper.toDto(categoryRepository.findAll(pageable).getContent());
    }

    @Override
    public CategoryDto getCategory(Long catId) {
        log.info("Запрос id: {}", catId);

        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> {
                    log.error("Не найдена id: {}", catId);
                    return new NotFoundException("Категория не найдена");
                });

        return categoryMapper.toDto(category);
    }

    @Override
    public Map<Long, CategoryDto> getCategoriesBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyMap();

        log.info("Пакетный запрос категорий для {} id", ids.size());

        return categoryRepository.findAllByIdIn(ids.stream().filter(Objects::nonNull).distinct().toList())
                .stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toMap(CategoryDto::getId, Function.identity()));
    }
}

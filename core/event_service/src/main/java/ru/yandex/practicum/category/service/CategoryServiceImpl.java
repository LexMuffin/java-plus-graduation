package ru.yandex.practicum.category.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.category.CategoryDto;
import ru.yandex.practicum.dto.category.NewCategoryDto;
import ru.yandex.practicum.category.mapper.CategoryMapper;
import ru.yandex.practicum.category.model.Category;
import ru.yandex.practicum.category.repository.CategoryRepository;
import ru.yandex.practicum.exception.ConflictException;
import ru.yandex.practicum.exception.NotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryServiceImpl implements CategoryService {

    CategoryRepository categoryRepository;
    CategoryMapper categoryMapper;

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
}

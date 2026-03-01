package ru.yandex.practicum.client.fallback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.client.category.CategoryServiceClient;
import ru.yandex.practicum.dto.category.CategoryDto;
import ru.yandex.practicum.exception.NotFoundException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryServiceClientFallback implements CategoryServiceClient {

    @Override
    public CategoryDto getCategory(Long catId) {
        log.error("Fallback: getCategory - сервис категорий недоступен для catId: {}", catId);
        throw new NotFoundException("Сервис категорий временно недоступен. Не удалось проверить категорию id=" + catId);
    }
}
package ru.yandex.practicum.client.category;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.yandex.practicum.dto.category.CategoryDto;

@FeignClient(name = "event-service", path = "/categories")
public interface CategoryServiceClient {

    @GetMapping("/{catId}")
    CategoryDto getCategory(@PathVariable("catId") Long catId);
}

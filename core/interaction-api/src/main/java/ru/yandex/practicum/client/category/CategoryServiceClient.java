package ru.yandex.practicum.client.category;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.dto.category.CategoryDto;

import java.util.List;
import java.util.Map;

@FeignClient(name = "category-service", path = "/categories")
public interface CategoryServiceClient {

    @GetMapping("/{catId}")
    CategoryDto getCategory(@PathVariable("catId") Long catId);

    @PostMapping("/batch")
    Map<Long, CategoryDto> getCategoriesBatch(@RequestBody List<Long> categoryIds);
}

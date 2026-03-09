package ru.yandex.practicum.client.user;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.yandex.practicum.dto.user.UserDto;

@FeignClient(name = "user-service", path = "/admin/users")
public interface UserServiceClient {

    @GetMapping("/{userId}")
    UserDto getUser(@PathVariable("userId") Long userId);
}

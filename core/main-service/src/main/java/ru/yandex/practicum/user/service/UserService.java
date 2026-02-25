package ru.yandex.practicum.user.service;

import org.springframework.data.domain.Pageable;
import ru.yandex.practicum.user.dto.NewUserRequest;
import ru.yandex.practicum.user.dto.UserDto;

import java.util.List;

public interface UserService {

    UserDto createUser(NewUserRequest request);

    List<UserDto> getUsers(List<Long> ids, Pageable pageable);

    void deleteUser(Long userId);
}

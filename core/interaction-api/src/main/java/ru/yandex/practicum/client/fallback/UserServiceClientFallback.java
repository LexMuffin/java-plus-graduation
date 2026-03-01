package ru.yandex.practicum.client.fallback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import ru.yandex.practicum.client.user.UserServiceClient;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.exception.NotFoundException;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClientFallback implements UserServiceClient {

    @Override
    public UserDto getUser(Long userId) {
        log.error("Fallback: getUser - сервис пользователей недоступен для userId: {}", userId);
        throw new NotFoundException("Сервис пользователей временно недоступен. Не удалось проверить пользователя id=" + userId);
    }
}
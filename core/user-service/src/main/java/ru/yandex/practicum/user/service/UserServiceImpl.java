package ru.yandex.practicum.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.exception.ConflictException;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.dto.user.NewUserRequest;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.user.mapper.UserMapper;
import ru.yandex.practicum.user.model.User;
import ru.yandex.practicum.user.repository.UserRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {

    UserRepository userRepository;
    UserMapper userMapper;

    @Override
    @Transactional
    public UserDto createUser(NewUserRequest request) {
        log.info("Добавление: {}", request.getEmail());

        User user = userMapper.toEntity(request);

        try {
            User saved = userRepository.save(user);
            log.info("Добавлено, id: {}", saved.getId());
            return userMapper.toDto(saved);
        } catch (DataIntegrityViolationException e) {
            log.error("Email уже существует: {}", request.getEmail());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email должен быть уникальным");
        }
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, Pageable pageable) {
        if (ids == null || ids.isEmpty()) {
            log.info("Запрос всех: from={}, size={}",
                    pageable.getPageNumber() * pageable.getPageSize(),
                    pageable.getPageSize());
            return userMapper.toDto(userRepository.findAll(pageable).getContent());
        }

        log.info("Запрос пользователей: ids={}", ids);
        return userMapper.toDto(userRepository.findByIdIn(ids, pageable).getContent());
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Удаление id: {}", userId);

        if (!userRepository.existsById(userId)) {
            log.error("Не найден id: {}", userId);
            throw new NotFoundException("Пользователь не найден");
        }

        userRepository.deleteById(userId);
        log.info("Удалено id: {}", userId);
    }

    @Override
    public UserDto getUser(Long userId) {
        log.info("Запрос пользователя: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь не найден: {}", userId);
                    return new NotFoundException("Пользователь с id=" + userId + " не найден");
                });

        return userMapper.toDto(user);
    }
}

package ru.yandex.practicum.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.exception.ConflictException;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.user.dto.NewUserRequest;
import ru.yandex.practicum.user.dto.UserDto;
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

    @Override
    @Transactional
    public UserDto createUser(NewUserRequest request) {
        log.info("Добавление: {}", request.getEmail());

        User user = UserMapper.INSTANCE.toEntity(request);

        try {
            User saved = userRepository.save(user);
            log.info("Добавлено, id: {}", saved.getId());
            return UserMapper.INSTANCE.toDto(saved);
        } catch (DataIntegrityViolationException e) {
            log.error("Email уже существует: {}", request.getEmail());
            throw new ConflictException("Email должен быть уникальным");
        }
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, Pageable pageable) {
        if (ids == null || ids.isEmpty()) {
            log.info("Запрос всех: from={}, size={}",
                    pageable.getPageNumber() * pageable.getPageSize(),
                    pageable.getPageSize());
            return UserMapper.INSTANCE.toDto(userRepository.findAll(pageable).getContent());
        }

        log.info("Запрос пользователей: ids={}", ids);
        return UserMapper.INSTANCE.toDto(userRepository.findByIdIn(ids, pageable).getContent());
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
}

package ru.yandex.practicum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(NotFoundException e) {
        log.error("404: {}", e.getMessage());
        return buildError(e.getMessage(), "Объект не найден", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflict(ConflictException e) {
        log.error("409: {}", e.getMessage());
        return buildError(e.getMessage(), "Нарушение целостности данных", HttpStatus.CONFLICT);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDataIntegrity(DataIntegrityViolationException e) {
        log.error("409: Дубликат данных");
        return buildError("Имя категории должно быть уникальным",
                "Нарушение целостности данных", HttpStatus.CONFLICT);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(MethodArgumentNotValidException e) {
        log.error("400: Ошибка валидации");

        List<String> errors = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatError)
                .collect(Collectors.toList());

        String message = errors.isEmpty() ? "Ошибка валидации" : errors.get(0);

        return ApiError.builder()
                .errors(errors)
                .message(message)
                .reason("Некорректный запрос")
                .status(HttpStatus.BAD_REQUEST.toString())
                .timestamp(LocalDateTime.now().format(FORMATTER))
                .build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.error("400: Неверный тип параметра");
        String message = String.format("Неверный формат параметра: ожидается %s",
                e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "число");
        return buildError(message, "Некорректный запрос", HttpStatus.BAD_REQUEST);
    }

    private String formatError(FieldError error) {
        return String.format("Поле: %s. Ошибка: %s. Значение: %s",
                error.getField(),
                error.getDefaultMessage(),
                error.getRejectedValue());
    }

    private ApiError buildError(String message, String reason, HttpStatus status) {
        return ApiError.builder()
                .message(message)
                .reason(reason)
                .status(status.toString())
                .timestamp(LocalDateTime.now().format(FORMATTER))
                .build();
    }
}

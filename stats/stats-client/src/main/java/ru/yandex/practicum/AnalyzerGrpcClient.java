package ru.yandex.practicum;

import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.ewm.grpc.stats.dashboard.RecommendationsControllerGrpc;
import ru.yandex.practicum.ewm.grpc.stats.messages.InteractionsCountRequestProto;
import ru.yandex.practicum.ewm.grpc.stats.messages.RecommendedEventProto;
import ru.yandex.practicum.ewm.grpc.stats.messages.SimilarEventsRequestProto;
import ru.yandex.practicum.ewm.grpc.stats.messages.UserPredictionsRequestProto;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyzerGrpcClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub analyzerClient;

    public Stream<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        try {
            UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();

            log.debug("Запрос рекомендаций для пользователя: userId={}, maxResults={}", userId, maxResults);

            Iterator<RecommendedEventProto> iterator = analyzerClient.getRecommendationsForUser(request);
            return asStream(iterator);

        } catch (StatusRuntimeException e) {
            log.error("Ошибка gRPC при получении рекомендаций: {}, статус: {}",
                    e.getMessage(), e.getStatus(), e);
            return Stream.empty();
        } catch (Exception e) {
            log.error("Ошибка при получении рекомендаций: {}", e.getMessage(), e);
            return Stream.empty();
        }
    }

    public Stream<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        try {
            SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                    .setEventId(eventId)
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();

            log.debug("Запрос похожих мероприятий: eventId={}, userId={}, maxResults={}",
                    eventId, userId, maxResults);

            Iterator<RecommendedEventProto> iterator = analyzerClient.getSimilarEvents(request);
            return asStream(iterator);

        } catch (StatusRuntimeException e) {
            log.error("Ошибка gRPC при получении похожих мероприятий: {}, статус: {}",
                    e.getMessage(), e.getStatus(), e);
            return Stream.empty();
        } catch (Exception e) {
            log.error("Ошибка при получении похожих мероприятий: {}", e.getMessage(), e);
            return Stream.empty();
        }
    }

    public Stream<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            log.debug("Пустой список идентификаторов мероприятий");
            return Stream.empty();
        }

        try {
            InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                    .addAllEventId(eventIds)
                    .build();

            log.debug("Запрос количества взаимодействий для {} мероприятий", eventIds.size());

            Iterator<RecommendedEventProto> iterator = analyzerClient.getInteractionsCount(request);
            return asStream(iterator);

        } catch (StatusRuntimeException e) {
            log.error("Ошибка gRPC при получении количества взаимодействий: {}, статус: {}",
                    e.getMessage(), e.getStatus(), e);
            return Stream.empty();
        } catch (Exception e) {
            log.error("Ошибка при получении количества взаимодействий: {}", e.getMessage(), e);
            return Stream.empty();
        }
    }

    private Stream<RecommendedEventProto> asStream(Iterator<RecommendedEventProto> iterator) {
        if (iterator == null) {
            return Stream.empty();
        }
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        );
    }
}
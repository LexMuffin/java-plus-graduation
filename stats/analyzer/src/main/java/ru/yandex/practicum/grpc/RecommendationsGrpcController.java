package ru.yandex.practicum.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.ewm.grpc.stats.dashboard.RecommendationsControllerGrpc;
import ru.yandex.practicum.ewm.grpc.stats.messages.InteractionsCountRequestProto;
import ru.yandex.practicum.ewm.grpc.stats.messages.RecommendedEventProto;
import ru.yandex.practicum.ewm.grpc.stats.messages.SimilarEventsRequestProto;
import ru.yandex.practicum.ewm.grpc.stats.messages.UserPredictionsRequestProto;
import ru.yandex.practicum.service.RecommendationService;

import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsGrpcController
        extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationService recommendationService;

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("[Analyzer] Received GetSimilarEvents request: eventId={}, userId={}, maxResults={}",
                request.getEventId(), request.getUserId(), request.getMaxResults());

        try {
            List<RecommendedEventProto> recommendations =
                    recommendationService.getSimilarEvents(
                            request.getEventId(), request.getUserId(), request.getMaxResults());

            recommendations.forEach(responseObserver::onNext);
            responseObserver.onCompleted();

            log.info("[Analyzer] Sent {} similar events", recommendations.size());
        } catch (Exception e) {
            log.error("[Analyzer] Error in GetSimilarEvents: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("[Analyzer] Received GetRecommendationsForUser request: userId={}, maxResults={}",
                request.getUserId(), request.getMaxResults());

        try {
            List<RecommendedEventProto> recommendations =
                    recommendationService.getRecommendationsForUser(request.getUserId(), request.getMaxResults());

            recommendations.forEach(responseObserver::onNext);
            responseObserver.onCompleted();

            log.info("[Analyzer] Sent {} user recommendations", recommendations.size());
        } catch (Exception e) {
            log.error("[Analyzer] Error in GetRecommendationsForUser: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("[Analyzer] Received GetInteractionsCount request: eventIds={}", request.getEventIdList());

        try {
            List<RecommendedEventProto> interactions =
                    recommendationService.getInteractionsCount(request.getEventIdList());

            interactions.forEach(responseObserver::onNext);
            responseObserver.onCompleted();

            log.info("[Analyzer] Sent interactions count for {} events", interactions.size());
        } catch (Exception e) {
            log.error("[Analyzer] Error in GetInteractionsCount: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }
}

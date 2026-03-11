package ru.yandex.practicum.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.ewm.grpc.stats.collector.UserActionControllerGrpc;
import ru.yandex.practicum.ewm.grpc.stats.messages.UserActionProto;
import ru.yandex.practicum.service.UserActionService;

@Slf4j
@RequiredArgsConstructor
@GrpcService
public class UserActionControllerGrpcImpl extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final UserActionService userActionService;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        try {
            log.info("Collecting user action: userId={}, eventId={}, actionType={}",
                    request.getUserId(), request.getEventId(), request.getActionType());
            userActionService.collectUserAction(request);
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error collecting user action, exception: {}", e.getMessage());
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }
}
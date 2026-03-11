package ru.yandex.practicum;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.ewm.grpc.stats.collector.UserActionControllerGrpc;
import ru.yandex.practicum.ewm.grpc.stats.messages.ActionTypeProto;
import ru.yandex.practicum.ewm.grpc.stats.messages.UserActionProto;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CollectorGrpcClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub collectorStub;

    public void sendUserAction(long userId, long eventId, ActionTypeProto actionType) {

        UserActionProto action = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(actionType)
                .setTimestamp(
                        Timestamp.newBuilder()
                                .setSeconds(Instant.now().getEpochSecond())
                                .setNanos(Instant.now().getNano())
                                .build()
                )
                .build();

        collectorStub.collectUserAction(action);
    }
}
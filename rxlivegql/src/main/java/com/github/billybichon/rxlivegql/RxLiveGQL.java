package com.github.billybichon.rxlivegql;

import com.google.gson.Gson;
import io.reactivex.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by billy on 23/07/2017.
 */
public class RxLiveGQL {

    // message type
    private static final String GQL_CONNECTION_INIT = "connection_init";
    private static final String GQL_CONNECTION_ACK = "connection_ack";
    private static final String GQL_CONNECTION_ERROR = "connection_error";
    private static final String GQL_CONNECTION_KEEP_ALIVE = "ka";
    private static final String GQL_CONNECTION_TERMINATE = "connection_terminate";
    private static final String GQL_START = "start";
    private static final String GQL_DATA = "data";
    private static final String GQL_ERROR = "error";
    private static final String GQL_COMPLETE = "complete";
    private static final String GQL_STOP = "stop";

    private WebSocketWrapper webSocketWrapper;
    private CompletableEmitter initEmitter;
    private HashMap<String, List<FlowableEmitter<String>>> subscriptionsMap = new HashMap<>();

    public RxLiveGQL() {

    }

    public interface Decoder<T> {
        T onDecode(String json);
    }


    /**
     * Connect to GraphQL Apollo server.
     *
     * @param url The url of the GraphQL server
     * @return A completable (onComplete when the connection is successful, onError otherwise)
     */
    public Completable connect(final String url) {
        return Completable.create(emitter -> {
            webSocketWrapper = new WebSocketWrapper();
            registerToAsync();
            try {
                webSocketWrapper.connect(url);
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e.getCause());
            }
        });
    }

    private void registerToAsync() {
        webSocketWrapper.registerToAsync().subscribe(msg -> {
                    switch (msg.type) {
                        case GQL_CONNECTION_ACK:
                            if (initEmitter != null)
                                initEmitter.onComplete();
                            break;
                        case GQL_CONNECTION_ERROR:
                            if (initEmitter != null)
                                initEmitter.onError(new IOException(msg.payload.data.toString()));
                            break;
                        case GQL_DATA:
                            if (subscriptionsMap.containsKey(msg.id)) {
                                subscriptionsMap.get(msg.id).forEach(stringFlowableEmitter -> {
                                    stringFlowableEmitter.onNext(msg.payload.data.toString());
                                });
                            }
                            break;
                        case "custom":
                            switch (msg.id) {
                                case "open":
                                    // todo
                                    break;
                                case "close":
                                    if (msg.payload == null)
                                        sendCompleteToAllEmitter();
                                    else
                                        sendErrorToAllEmitter(new Throwable(msg.payload.data.toString()));
                                    break;
                                case "error":
                                    // todo
                                    break;
                            }
                            break;
                    }
                }, this::sendErrorToAllEmitter);
    }

    private void sendErrorToAllEmitter(Throwable throwable) {
        subscriptionsMap.forEach((first, second) -> {
            second.forEach(e -> {
                e.onError(throwable);
            });
        });
    }

    private void sendCompleteToAllEmitter() {
        initEmitter.onComplete();
        subscriptionsMap.forEach((first, second) -> {
            second.forEach(Emitter::onComplete);
        });
    }

    /**
     * Initialize the connection with the GraphQL server.
     *
     * @return A completable (onComplete when the initialization is successful, onError otherwise)
     */
    public Completable initServer() {
        return Completable.create(emitter -> {
            MessageServer message = new MessageServer(null, null, GQL_CONNECTION_INIT);
            webSocketWrapper.sendMessage(message);
            initEmitter = emitter;
        });
    }

    public Completable closeConnection() {
        return Completable.create(emitter -> {
            MessageServer message = new MessageServer(null, null, GQL_CONNECTION_TERMINATE);
            webSocketWrapper.sendMessage(message);
            webSocketWrapper.closeConnection();
            emitter.onComplete();
        });
    }

    public Flowable<String> subscribe(String query, final String tag) {
        return Flowable.create(emitter -> {
            MessageServer message = new MessageServer(new PayloadServer(query, null, null), tag, GQL_START);
            webSocketWrapper.sendMessage(message);
            if (subscriptionsMap.containsKey(tag))
                subscriptionsMap.get(tag).add(emitter);
            else {
                List tmp = new ArrayList<FlowableEmitter<String>>();
                tmp.add(emitter);
                subscriptionsMap.put(tag, tmp);
            }
        }, BackpressureStrategy.BUFFER);
    }

    public Completable unsubscribe(String tag) {
        return Completable.create(emitter -> {
            MessageServer message = new MessageServer(null, tag, GQL_STOP);
            webSocketWrapper.sendMessage(message);
            subscriptionsMap.get(tag).forEach(Emitter::onComplete);
            subscriptionsMap.remove(tag);
            emitter.onComplete();
        });
    }
}

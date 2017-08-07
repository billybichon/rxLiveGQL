package com.github.billybichon.rxlivegql;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;

import java.io.IOException;

/**
 * Created by billy on 23/07/2017.
 * <p>
 * ReactiveX implementation of a websocket communication with a GraphQL server.
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

    public interface Decoder<T> {
        T decode(String json);
    }

    /**
     * Connect to GraphQL Apollo server.
     * <p>
     * The first onNext is fire when the connection is successful, onError will be launch if the connection is abnormally lost.
     * <p>
     * When the connection is normally close, onComplete is called.
     *
     * @param url The url of the GraphQL server
     * @return A Flowable
     */
    public Flowable<String> connect(final String url) {
        return Flowable.create(emitter -> {
            webSocketWrapper = new WebSocketWrapper();
            webSocketWrapper.getSubject().subscribe(msg -> {
                switch (msg.type) {
                    case "custom":
                        switch (msg.id) {
                            case "open":
                                emitter.onNext("");
                                break;
                            case "close":
                                if (msg.payload == null)
                                    emitter.onComplete();
                                else
                                    emitter.onError(new Throwable(msg.payload.data.toString()));
                                break;
                            case "error":
                                emitter.onError(new Throwable((msg.payload != null) ? msg.payload.data.toString() : "unknown error"));
                                break;
                        }
                        break;
                }
            });
            try {
                webSocketWrapper.connect(url);
            } catch (Exception e) {
                emitter.onError(e.getCause());
            }
        }, BackpressureStrategy.BUFFER);
    }

    /**
     * Initialize the connection with the GraphQL server.
     *
     * @return A completable (onComplete when the initialization is successful, onError otherwise)
     */
    public Completable initServer() {
        return Completable.create(emitter -> {
            webSocketWrapper.getSubject().subscribe(msg -> {
                switch (msg.type) {
                    case GQL_CONNECTION_ACK:
                        emitter.onComplete();
                        break;
                    case GQL_CONNECTION_ERROR:
                        emitter.onError(new IOException(msg.payload.data.toString()));
                        break;
                    case GQL_ERROR:
                        emitter.onError(new Throwable("please check that the server implements the Apollo protocol", new ApolloProtocolException("Unknown error")));
                        break;
                }
            });
            MessageServer message = new MessageServer(null, null, GQL_CONNECTION_INIT);
            webSocketWrapper.sendMessage(message);
        });
    }


    /**
     * Close the connection with the server.
     *
     * @return A completable which will indicate that the connection has been successfully closed (or not if a network error occurred)
     */
    public Completable closeConnection() {
        return Completable.create(emitter -> {
            MessageServer message = new MessageServer(null, null, GQL_CONNECTION_TERMINATE);
            webSocketWrapper.sendMessage(message);
            webSocketWrapper.closeConnection();
            emitter.onComplete();
        });
    }


    /**
     * Subscribe to an event base on the query
     *
     * @param query The query to subscribe
     * @param tag   The tag associated with this query
     * @return A flowable which will return a string each time the associated query respond.
     */
    public Flowable<String> subscribe(String query, final String tag) {
        return Flowable.create(emitter -> {
            webSocketWrapper.getSubject()
                    .filter(messageClient -> messageClient.type.equals(GQL_DATA) && messageClient.id.equals(tag))
                    .subscribe(msg -> {
                        emitter.onNext(msg.payload.data.toString());
                    });
            MessageServer message = new MessageServer(new PayloadServer(query, null, null), tag, GQL_START);
            webSocketWrapper.sendMessage(message);
        }, BackpressureStrategy.BUFFER);
    }

    /**
     * Subscribe to an event base on the query
     *
     * @param query     The query to subscribe
     * @param tag       The tag associated with this query
     * @param className The class name of the decoder object
     * @param <T>       The object return by the query
     * @param <U>       The object that can decode the T object
     * @return A flowable which will return a object T each time the associated query respond.
     */
    // todo throws a class cast exception
    public <T, U extends Decoder<T>> Flowable<T> subscribe(final String query, final String tag, final Class<U> className) {
        return Flowable.create(emitter -> {
            webSocketWrapper.getSubject()
                    .filter(messageClient -> messageClient.type.equals(GQL_DATA) && messageClient.id.equals(tag))
                    .subscribe(msg -> {
                        U tmp = className.newInstance();
                        T tmpDecoded = tmp.decode(msg.payload.data.toString());
                        emitter.onNext(tmpDecoded);
                    });
            MessageServer message = new MessageServer(new PayloadServer(query, null, null), tag, GQL_START);
            webSocketWrapper.sendMessage(message);
        }, BackpressureStrategy.BUFFER);
    }

    /**
     * Unsubscribe to a subscription by his tag
     *
     * @param tag The tag of the subscription
     * @return A completable which will indicate that the subscription has been successfully unsubscribe (or not if a network error occurred)
     */
    public Completable unsubscribe(String tag) {
        return Completable.create(emitter -> {
            MessageServer message = new MessageServer(null, tag, GQL_STOP);
            webSocketWrapper.sendMessage(message);
            emitter.onComplete();
        });
    }
}

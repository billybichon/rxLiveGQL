package com.github.billybichon.rxlivegql;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;

/**
 * Created by billy on 23/07/2017.
 */
@ClientEndpoint(subprotocols = {"graphql-ws"}, encoders = {MessageServer.Encoder.class}, decoders = {MessageClient.Decoder.class})
public class WebSocketWrapper {

//    private ObservableEmitter<MessageClient> emitter;
    private Session session;
    private PublishSubject<MessageClient> subject;

    WebSocketWrapper() {
        subject = PublishSubject.create();
    }

    void connect(final String url) throws DeploymentException, IOException, IllegalStateException {
        ContainerProvider.getWebSocketContainer().connectToServer(WebSocketWrapper.this, URI.create(url));
    }

    PublishSubject<MessageClient> getSubject() {
        return subject;
    }

    void closeConnection() {
        try {
            if (session != null && session.isOpen())
                session.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.session = null;
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        if (this.subject != null) {
            MessageClient message = new MessageClient(null, "open", "custom");
            this.subject.onNext(message);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        if (this.subject != null) {
            MessageClient message;
            if (closeReason.getCloseCode().getCode() != 1000)
                message = new MessageClient(new PayloadClient(closeReason.getReasonPhrase()), "close", "custom");
            else
                message = new MessageClient(null, "close", "custom");
            this.subject.onNext(message);
        }
    }

    @OnMessage
    public void onMessage(MessageClient message) {
        if (this.subject != null)
            this.subject.onNext(message);
    }

    @OnError
    public void onError(Throwable e) {
        if (this.subject != null) {
            MessageClient message = new MessageClient(new PayloadClient(e.getMessage()), "error", "custom");
            this.subject.onNext(message);
        }
    }

    void sendMessage(MessageServer msg) throws IOException, EncodeException {
        if (session != null && session.isOpen()) {
            session.getBasicRemote().sendObject(msg);
        }
    }
}

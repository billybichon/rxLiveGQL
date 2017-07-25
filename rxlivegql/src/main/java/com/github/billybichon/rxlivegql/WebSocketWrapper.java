package com.github.billybichon.rxlivegql;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;

/**
 * Created by billy on 23/07/2017.
 */
@ClientEndpoint(subprotocols = {"graphql-ws"}, encoders = {MessageServer.Encoder.class}, decoders = {MessageClient.Decoder.class})
public class WebSocketWrapper {

    private FlowableEmitter<MessageClient> emitter;
    private Session session;

    public WebSocketWrapper() {

    }

    public void connect(final String url) throws DeploymentException, IOException, IllegalStateException {
        ContainerProvider.getWebSocketContainer().connectToServer(WebSocketWrapper.this, URI.create(url));
    }

    public Flowable<MessageClient> registerToAsync() {
        return Flowable.create(emt -> {
            emitter = emt;
        }, BackpressureStrategy.BUFFER);
    }

    public void closeConnection() {
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
        if (this.emitter != null) {
            MessageClient message = new MessageClient(null, "open", "custom");
            this.emitter.onNext(message);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        if (this.emitter != null) {
            MessageClient message;
            if (closeReason.getCloseCode().getCode() != 1000)
                message = new MessageClient(new PayloadClient(closeReason.getReasonPhrase()), "close", "custom");
            else
                message = new MessageClient(null, "close", "custom");
            this.emitter.onNext(message);
        }
    }

    @OnMessage
    public void onMessage(MessageClient message) {
        if (this.emitter != null)
            this.emitter.onNext(message);
    }

    @OnError
    public void onError(Throwable e) {
        if (this.emitter != null) {
            MessageClient message = new MessageClient(new PayloadClient(e.getMessage()), "error", "custom");
            this.emitter.onNext(message);
        }
    }

    public void sendMessage(MessageServer msg) throws IOException, EncodeException {
        if (session != null && session.isOpen()) {
            session.getBasicRemote().sendObject(msg);
        }
    }
}

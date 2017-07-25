package com.github.billybichon.rxlivegql;

import com.google.gson.Gson;

import javax.websocket.EncodeException;
import javax.websocket.EndpointConfig;

/**
 * Created by billy on 23/07/2017.
 * <p>
 * Structure of the message defined by the GraphQL over WebSocket Protocol.
 */
class MessageServer {

    public PayloadServer payload;
    public String id;
    public String type;

    public MessageServer(PayloadServer payload, String id, String type) {
        this.payload = payload;
        this.id = id;
        this.type = type;
    }

    public static class Encoder implements javax.websocket.Encoder.Text<MessageServer> {

        public String encode(MessageServer object) throws EncodeException {
            return new Gson().toJson(object);
        }

        public void init(EndpointConfig config) {

        }

        public void destroy() {

        }
    }
}

package com.github.billybichon.rxlivegql;

import com.google.gson.Gson;

import javax.websocket.DecodeException;
import javax.websocket.EndpointConfig;

/**
 * Created by billy on 23/07/2017.
 */

class MessageClient {

    public PayloadClient payload;
    public String id;
    public String type;

    MessageClient(PayloadClient payload, String id, String type) {
        this.payload = payload;
        this.id = id;
        this.type = type;
    }

    public static class Decoder implements javax.websocket.Decoder.Text<MessageClient> {

        private Gson gson = new Gson();

        public MessageClient decode(String s) throws DecodeException {
            return gson.fromJson(s, MessageClient.class);
        }

        public boolean willDecode(String s) {
            return true;
        }

        public void init(EndpointConfig config) {

        }

        public void destroy() {

        }
    }
}

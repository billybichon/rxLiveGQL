package com.github.billybichon.rxlivegql;

/**
 * Created by billy on 31/07/2017.
 *
 * Exception indicating some kind of error with the apollo protocol implementation.
 */
public class ApolloProtocolException extends Exception {

    public ApolloProtocolException(String message) {
        super(message);
    }

    public ApolloProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.github.billybichon.rxlivegql;

/**
 * Created by billy on 23/07/2017.
 */

class PayloadServer {

    public String query;
    public Object variables;
    public String operationName;

    public PayloadServer(String query, Object variables, String operationName) {
        this.query = query;
        this.variables = variables;
        this.operationName = operationName;
    }
}

package com.microfocus.octane.plugins.rest;

/**
 * This class is used only to catch and rethrow internally the scenario when we have a timed out request.
 */
public class RequestTimedOutException extends RestStatusException {

    public RequestTimedOutException(Response response) {
        super(response);
    }
}

package com.grash.advancedsearch;

/**
 * Thrown when a search query references a field or relation path that is not
 * allowed by {@link SearchFieldPolicy}.
 * <p>
 * The message keeps the offending field for server-side logging purposes, but
 * the HTTP layer must only surface the generic {@link #getClientMessage()} so
 * that no internal model information is disclosed to the client.
 */
public class InvalidSearchFieldException extends RuntimeException {

    private static final String CLIENT_MESSAGE = "Invalid search field";

    public InvalidSearchFieldException(String field) {
        super("Invalid search field: " + field);
    }

    public String getClientMessage() {
        return CLIENT_MESSAGE;
    }
}

package com.strac.files.exceptions;

/**
 * @author Charles on 22/12/2024
 */
public class RecordNotFoundException extends RuntimeException {
    public RecordNotFoundException(String message) {
        super(message);
    }
}

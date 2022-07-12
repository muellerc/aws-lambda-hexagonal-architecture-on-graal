package com.amazonaws.example.cmr.domain;

public class DeleteUnicornException extends RuntimeException{

    public DeleteUnicornException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}

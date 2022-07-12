package com.amazonaws.example.cmr.domain;

public class CreateUnicornException extends RuntimeException{

    public CreateUnicornException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}

package com.amazonaws.example.cmr.domain;

public class UpdateUnicornException extends RuntimeException{

    public UpdateUnicornException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}

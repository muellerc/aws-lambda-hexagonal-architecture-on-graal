package com.amazonaws.example.cmr.domain;

public class GetUnicornException extends RuntimeException{

    public GetUnicornException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}

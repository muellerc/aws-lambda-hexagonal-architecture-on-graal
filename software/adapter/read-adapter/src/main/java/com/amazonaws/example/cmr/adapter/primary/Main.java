package com.amazonaws.example.cmr.adapter.primary;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

public class Main {

    private static String REQUEST = "{\"pathParameters\":{\"id\":\"4d2cbf86-31db-447a-96fc-a0e4f7cf3a68\"}}";

    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {
        GetUnicornFunction function = new GetUnicornFunction();
        InputStream inputStream = new ByteArrayInputStream(REQUEST.getBytes(StandardCharsets.UTF_8));
        OutputStream outputStream = new ByteArrayOutputStream();

        function.handleRequest(inputStream, outputStream, null);
    }
}

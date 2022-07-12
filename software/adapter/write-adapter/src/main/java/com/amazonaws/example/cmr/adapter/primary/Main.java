package com.amazonaws.example.cmr.adapter.primary;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

public class Main {

    private static String REQUEST = "{\"body\": {\"name\":\"Big Unicorn\",\"age\":\"13\",\"type\":\"Beautiful\",\"size\":\"196 cm\"}}";

    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {
        CreateUnicornFunction function = new CreateUnicornFunction();
        InputStream inputStream = new ByteArrayInputStream(REQUEST.getBytes(StandardCharsets.UTF_8));
        OutputStream outputStream = new ByteArrayOutputStream();

        function.handleRequest(inputStream, outputStream, null);
    }
}

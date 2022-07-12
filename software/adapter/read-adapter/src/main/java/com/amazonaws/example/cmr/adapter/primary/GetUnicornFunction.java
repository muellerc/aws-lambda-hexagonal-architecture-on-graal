package com.amazonaws.example.cmr.adapter.primary;

import com.amazonaws.example.cmr.adapter.secondary.AWSGetUnicorn;
import com.amazonaws.example.cmr.domain.Unicorn;
import com.amazonaws.example.cmr.port.GetUnicorn;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.jr.ob.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.*;

public class GetUnicornFunction implements RequestStreamHandler {

    private static final Logger logger = LoggerFactory.getLogger(GetUnicornFunction.class);

    private GetUnicorn port;

    public GetUnicornFunction() throws ExecutionException, InterruptedException {
        Region region = Region.of(System.getenv("AWS_REGION"));
        String table = System.getenv("TABLE_NAME");

        AwsCredentialsProvider provider = EnvironmentVariableCredentialsProvider.create();
        // HTTP client sharing: https://aws.amazon.com/blogs/developer/aws-sdk-for-java-2-x-released/
        SdkAsyncHttpClient sdkAsyncHttpClient = AwsCrtAsyncHttpClient.builder()
                .maxConcurrency(20)
                .build();

        DynamoDbAsyncClient dynamoDbClient = DynamoDbAsyncClient.builder()
                .credentialsProvider(provider)
                .region(region)
                .httpClient(sdkAsyncHttpClient)
                .build();
        try {
            // Force to establish the HTTPS connections during the initialization,
            // when we can benefit from a performance boost.
            dynamoDbClient.describeEndpoints().get();
        } catch (Exception e) {
            // ignore
        }

        port = new AWSGetUnicorn(dynamoDbClient, table);
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        String unicornId = readUnicornId(inputStream, context);

        Unicorn unicorn = port.getUnicorn(unicornId);
        logger.info("Unicorn found: " + unicorn);

        writeUnicorn(unicorn, outputStream, context);
    }

    private String readUnicornId(InputStream inputStream, Context context) throws IOException {
        Map<String, Object> request = JSON.std.mapFrom(inputStream);
        String id = ((Map<String, String>) request.get("pathParameters")).get("id");

        return id;
    }

    private void writeUnicorn(Unicorn unicorn, OutputStream outputStream, Context context) throws IOException {
        JSON.std.composeTo(outputStream)
                .startObject()
                .put("statusCode", 200)
                .put("body", JSON.std.asString(unicorn))
                .end()
                .finish();
    }
}

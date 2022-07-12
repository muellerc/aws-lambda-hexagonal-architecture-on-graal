package com.amazonaws.example.cmr.adapter.primary;

import com.amazonaws.example.cmr.adapter.secondary.AWSCreateUnicorn;
import com.amazonaws.example.cmr.adapter.secondary.AWSUpdateUnicorn;
import com.amazonaws.example.cmr.domain.Unicorn;
import com.amazonaws.example.cmr.port.CreateUnicorn;
import com.amazonaws.example.cmr.port.UpdateUnicorn;
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
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.model.DescribeEventBusRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.*;

public class UpdateUnicornFunction implements RequestStreamHandler {

    private static final Logger logger = LoggerFactory.getLogger(UpdateUnicornFunction.class);

    private UpdateUnicorn port;

    public UpdateUnicornFunction() throws ExecutionException, InterruptedException {
        Region region = Region.of(System.getenv("AWS_REGION"));
        String table = System.getenv("TABLE_NAME");
        String eventBus = System.getenv("EVENT_BUS");

        AwsCredentialsProvider provider = EnvironmentVariableCredentialsProvider.create();
        // HTTP client sharing: https://aws.amazon.com/blogs/developer/aws-sdk-for-java-2-x-released/
        SdkAsyncHttpClient sdkAsyncHttpClient = AwsCrtAsyncHttpClient.builder()
                .maxConcurrency(20)
                .build();

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Future<DynamoDbAsyncClient> dynamoDbClientFuture = executorService.submit(new Callable<DynamoDbAsyncClient>() {
            public DynamoDbAsyncClient call() throws Exception {
                DynamoDbAsyncClient client = DynamoDbAsyncClient.builder()
                        .credentialsProvider(provider)
                        .region(region)
                        .httpClient(sdkAsyncHttpClient)
                        .build();
                try {
                    // Force to establish the HTTPS connections during the initialization,
                    // when we can benefit from a performance boost.
                    client.describeEndpoints().get();
                } catch (Exception e) {
                    // ignore
                }
                return client;
            }
        });
        Future<EventBridgeAsyncClient> eventBridgeClientFuture = executorService.submit(new Callable<EventBridgeAsyncClient>() {
            public EventBridgeAsyncClient call() throws Exception {
                EventBridgeAsyncClient client = EventBridgeAsyncClient.builder()
                        .credentialsProvider(provider)
                        .region(region)
                        .httpClient(sdkAsyncHttpClient)
                        .build();

                try {
                    client.describeEventBus(
                            DescribeEventBusRequest.builder()
                                    .name(eventBus)
                                    .build())
                            .get();
                } catch (Exception e) {
                    // ignore
                }

                return client;
            }
        });
        EventBridgeAsyncClient eventBridgeClient = eventBridgeClientFuture.get();
        DynamoDbAsyncClient dynamoDbClient = dynamoDbClientFuture.get();

        port = new AWSUpdateUnicorn(dynamoDbClient, table, eventBridgeClient, eventBus);
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        Unicorn newUnicorn = readUnicorn(inputStream, context);

        Unicorn unicorn = port.updateUnicorn(newUnicorn);
        logger.info("Unicorn updated: " + unicorn);

        writeUnicorn(unicorn, outputStream, context);
    }

    private Unicorn readUnicorn(InputStream inputStream, Context context) throws IOException {
        Map<String, Object> request = JSON.std.mapFrom(inputStream);
        String id = ((Map<String, String>) request.get("pathParameters")).get("id");
        Unicorn unicorn = JSON.std.beanFrom(Unicorn.class, request.get("body"));
        unicorn.setId(id);

        return unicorn;
    }

    private void writeUnicorn(Unicorn unicorn, OutputStream outputStream, Context context) throws IOException {
        JSON.std.composeTo(outputStream)
                .startObject()
                .put("statusCode", 201)
                .put("body", JSON.std.asString(unicorn))
                .end()
                .finish();
    }
}

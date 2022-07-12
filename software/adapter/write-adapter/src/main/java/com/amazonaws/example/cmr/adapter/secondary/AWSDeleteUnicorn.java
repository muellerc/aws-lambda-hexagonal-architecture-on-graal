package com.amazonaws.example.cmr.adapter.secondary;

import com.amazonaws.example.cmr.domain.DeleteUnicornException;
import com.amazonaws.example.cmr.domain.Unicorn;
import com.amazonaws.example.cmr.domain.UnicornEventType;
import com.amazonaws.example.cmr.port.DeleteUnicorn;
import com.fasterxml.jackson.jr.ob.JSON;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class AWSDeleteUnicorn implements DeleteUnicorn {

    private DynamoDbAsyncClient dynamoDbClient;
    private String table;
    private EventBridgeAsyncClient eventBridgeClient;
    private String eventBus;

    public AWSDeleteUnicorn(DynamoDbAsyncClient dynamoDbClient, String table, EventBridgeAsyncClient eventBridgeClient, String eventBus) {
        this.dynamoDbClient = dynamoDbClient;
        this.table = table;
        this.eventBridgeClient = eventBridgeClient;
        this.eventBus = eventBus;
    }

    @Override
    public void deleteUnicorn(String unicornId) {
        try {
            delete(unicornId);

            Unicorn unicorn = new Unicorn();
            unicorn.setId(unicornId);
            publishEvent(unicorn, UnicornEventType.UNICORN_DELETED);
        } catch (Exception e) {
            throw new DeleteUnicornException("Error while deleting the Unicorn", e);
        }
    }

    private void delete(String unicornId) throws ExecutionException, InterruptedException {
        DeleteItemResponse deleteItemResponse = dynamoDbClient.deleteItem(
                DeleteItemRequest.builder()
                        .tableName(table)
                        .key(Map.of("id", AttributeValue.builder().s(unicornId).build()))
                        .build())
                .get();
    }

    private void publishEvent(Unicorn unicorn, UnicornEventType unicornEventType) throws IOException, ExecutionException, InterruptedException {
        eventBridgeClient.putEvents(PutEventsRequest.builder()
                .entries(PutEventsRequestEntry.builder()
                        .source("com.unicorn.store")
                        .eventBusName("unicorns")
                        .detailType(unicornEventType.name())
                        .detail(JSON.std.asString(unicorn))
                        .build())
                .build())
                .get();
    }
}

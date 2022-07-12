package com.amazonaws.example.cmr.adapter.secondary;

import com.amazonaws.example.cmr.domain.Unicorn;
import com.amazonaws.example.cmr.domain.UnicornEventType;
import com.amazonaws.example.cmr.domain.UpdateUnicornException;
import com.amazonaws.example.cmr.port.UpdateUnicorn;
import com.fasterxml.jackson.jr.ob.JSON;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class AWSUpdateUnicorn implements UpdateUnicorn {

    private DynamoDbAsyncClient dynamoDbClient;
    private String table;
    private EventBridgeAsyncClient eventBridgeClient;
    private String eventBus;

    public AWSUpdateUnicorn(DynamoDbAsyncClient dynamoDbClient, String table, EventBridgeAsyncClient eventBridgeClient, String eventBus) {
        this.dynamoDbClient = dynamoDbClient;
        this.table = table;
        this.eventBridgeClient = eventBridgeClient;
        this.eventBus = eventBus;
    }

    @Override
    public Unicorn updateUnicorn(Unicorn unicorn) {
        try {
            save(unicorn);
            publishEvent(unicorn, UnicornEventType.UNICORN_UPDATED);

            return unicorn;
        } catch (Exception e) {
            throw new UpdateUnicornException("Error while updating the Unicorn", e);
        }
    }

    private void save(Unicorn unicorn) throws ExecutionException, InterruptedException {
        Map<String, AttributeValue> itemAttributes = new HashMap<>();
        itemAttributes.put("id", AttributeValue.builder().s(unicorn.getId()).build());
        itemAttributes.put("type", AttributeValue.builder().s(unicorn.getType()).build());
        itemAttributes.put("name", AttributeValue.builder().s(unicorn.getName()).build());
        itemAttributes.put("age", AttributeValue.builder().s(unicorn.getAge()).build());
        itemAttributes.put("size", AttributeValue.builder().s(unicorn.getSize()).build());

        dynamoDbClient.putItem(
                PutItemRequest.builder()
                        .tableName(table)
                        .item(itemAttributes)
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

package com.amazonaws.example.cmr.adapter.secondary;

import com.amazonaws.example.cmr.domain.GetUnicornException;
import com.amazonaws.example.cmr.domain.Unicorn;
import com.amazonaws.example.cmr.port.GetUnicorn;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import java.util.Map;
import java.util.concurrent.ExecutionException;

public class AWSGetUnicorn implements GetUnicorn {

    private DynamoDbAsyncClient dynamoDbClient;
    private String table;

    public AWSGetUnicorn(DynamoDbAsyncClient dynamoDbClient, String table) {
        this.dynamoDbClient = dynamoDbClient;
        this.table = table;
    }

    @Override
    public Unicorn getUnicorn(String unicornId) {
        try {
            Unicorn unicorn = get(unicornId);

            return unicorn;
        } catch (Exception e) {
            throw new GetUnicornException("Error while getting the Unicorn", e);
        }
    }

    private Unicorn get(String unicornId) throws ExecutionException, InterruptedException {
        GetItemResponse getItemResponse = dynamoDbClient.getItem(
                GetItemRequest.builder()
                        .tableName(table)
                        .key(Map.of("id", AttributeValue.builder().s(unicornId).build()))
                        .build())
                .get();

        Map<String, AttributeValue> items = getItemResponse.item();

        Unicorn unicorn = new Unicorn();
        if (items != null && !items.isEmpty()) {
            unicorn.setId(getItemResponse.item().get("id").s());
            unicorn.setType(getItemResponse.item().get("type").s());
            unicorn.setName(getItemResponse.item().get("name").s());
            unicorn.setAge(getItemResponse.item().get("age").s());
            unicorn.setSize(getItemResponse.item().get("size").s());
        }

        return unicorn;
    }
}

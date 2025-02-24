package com.example.sfn.lambda.repository;

import com.example.sfn.lambda.dto.SqsMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class DynamoDbRepository {

    private final DynamoDbClient dynamoDbClient;

    @Inject
    public DynamoDbRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public void saveMessage(final SqsMessage message, final String tableName) {
        Map<String, AttributeValue> item = new HashMap<>();

        item.put("orderId", AttributeValue.builder().s(message.getOrderId()).build());
        item.put("startTime", AttributeValue.builder().s(message.getStartTime()).build());
        item.put("transactionId", AttributeValue.builder().s(message.getTransactionId()).build());
        item.put("taskToken", AttributeValue.builder().s(message.getTaskToken()).build());
        item.put("status", AttributeValue.builder().s("PENDING").build());

        PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();

        dynamoDbClient.putItem(putItemRequest);
    }
}
package com.example.sfn.lambda.exception;

public class DynamoDBException extends MessageProcessingException {
    public DynamoDBException(String message, Throwable cause) {
        super(message, cause);
    }
}
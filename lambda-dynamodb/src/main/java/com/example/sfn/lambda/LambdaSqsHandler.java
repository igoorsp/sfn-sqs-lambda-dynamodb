package com.example.sfn.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.example.sfn.lambda.dto.SqsMessage;
import com.example.sfn.lambda.exception.InvalidMessageException;
import com.example.sfn.lambda.exception.MessageProcessingException;
import com.example.sfn.lambda.repository.DynamoDbRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Named("sqsEmailHandler")
public class LambdaSqsHandler implements RequestHandler<SQSEvent, Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LambdaSqsHandler.class);

    @ConfigProperty(name = "app.dynamodb.table")
    String dynamoDbTable;

    private final DynamoDbRepository dynamoDbRepository;

    @Inject
    public LambdaSqsHandler(DynamoDbRepository dynamoDbRepository) {
        this.dynamoDbRepository = dynamoDbRepository;
    }

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        for (SQSEvent.SQSMessage msg : event.getRecords()) {
            LOGGER.info("Context Event: event={}, context={}", event, context);
            LOGGER.info("Processando body da msg: body={}", msg.getBody());
            try {
                // Parseia a mensagem
                SqsMessage sqsMessage = parseMessage(msg.getBody());
                LOGGER.info("Processando mensagem: TaskToken={}, Status={}", sqsMessage.getTaskToken(), sqsMessage.getStatus());

                // Salva no DynamoDB
                dynamoDbRepository.saveMessage(sqsMessage, dynamoDbTable);
                LOGGER.info("Mensagem salva no DynamoDB com sucesso: TaskToken={}", sqsMessage.getTaskToken());

            } catch (InvalidMessageException e) {
                LOGGER.error("Mensagem inválida: {}", msg.getBody(), e);
                throw new MessageProcessingException("Falha ao processar mensagem inválida", e);
            } catch (MessageProcessingException e) {
                LOGGER.error("Erro ao processar mensagem: {}", msg.getBody(), e);
                throw e; // Relança a exceção para falhar a execução da Lambda
            }
        }
        return null; // Retorno void, pois o sucesso é confirmado pelos logs
    }

    private SqsMessage parseMessage(String messageBody) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            SqsMessage message = objectMapper.readValue(messageBody, SqsMessage.class);

            // Valida campos obrigatórios
            if (message.getTaskToken() == null || message.getTransactionId() == null) {
                throw new InvalidMessageException("Campos obrigatórios faltando: taskToken e transactionId");
            }

            return message;
        } catch (JsonProcessingException e) {
            throw new InvalidMessageException("JSON inválido: " + messageBody, e);
        }
    }
}
package com.airline.flight.listener;

import com.airline.tracing.annotation.KafkaOtelTrace;
import com.airline.flight.exception.KafkaMessageProcessingException;
import com.airline.flight.service.FlightService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationListener {
    
    private final FlightService flightService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @KafkaListener(topics = {"reservation.requested"}, groupId = "flight")
    @KafkaOtelTrace(
        spanName = "process-reservation-requested", 
        attributes = {"event.type=reservation.requested", "service=flight"},
        recordMessageContent = true
    )
    public void handleReservationRequested(
            @org.springframework.messaging.handler.annotation.Payload String message,
            @org.springframework.messaging.handler.annotation.Headers org.springframework.messaging.MessageHeaders headers
    ) {
        log.info("Received reservation.requested event: {}", message);
        
        JsonNode reservationData = parseMessageData(message);
        String reservationId = reservationData.get("reservationId").asText();
        String flightId = reservationData.get("flightId").asText();
        int requestedSeats = reservationData.get("requestedSeats").asInt();
        
        log.info("Processing seat reservation for reservationId: {}, flightId: {}, seats: {}", 
                reservationId, flightId, requestedSeats);
        
        processReservationRequest(reservationId, flightId, requestedSeats);
    }
    
    /**
     * 메시지 데이터를 파싱합니다.
     */
    private JsonNode parseMessageData(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new KafkaMessageProcessingException("Message cannot be null or empty");
        }
        
        try {
            return objectMapper.readTree(message);
        } catch (JsonProcessingException e) {
            throw new KafkaMessageProcessingException("Failed to parse JSON message", e);
        }
    }
    
    /**
     * 예약 요청을 처리하고 적절한 이벤트를 발행합니다.
     */
    private void processReservationRequest(String reservationId, String flightId, int requestedSeats) {
        validateReservationData(reservationId, flightId, requestedSeats);
        flightService.reserveSeats(flightId, requestedSeats);
        publishSeatReservedEvent(reservationId, flightId, requestedSeats);
    }
    
    /**
     * 예약 데이터를 검증합니다.
     */
    private void validateReservationData(String reservationId, String flightId, int requestedSeats) {
        if (reservationId == null || reservationId.trim().isEmpty()) {
            throw new KafkaMessageProcessingException("Reservation ID cannot be null or empty");
        }
        if (flightId == null || flightId.trim().isEmpty()) {
            throw new KafkaMessageProcessingException("Flight ID cannot be null or empty");
        }
        if (requestedSeats <= 0) {
            throw new KafkaMessageProcessingException("Requested seats must be greater than 0");
        }
    }
    
    /**
     * 좌석 예약 성공 이벤트를 발행합니다.
     */
    private void publishSeatReservedEvent(String reservationId, String flightId, int reservedSeats) {
        var eventData = Map.of(
            "reservationId", reservationId,
            "flightId", flightId,
            "reservedSeats", reservedSeats,
            "seatReservationStatus", "CONFIRMED",
            "timestamp", System.currentTimeMillis()
        );
        
        try {
            String seatReservedEvent = objectMapper.writeValueAsString(eventData);
            kafkaTemplate.send("seat.reserved", reservationId, seatReservedEvent);
            log.info("Published seat.reserved event for reservationId: {} with data: {}", reservationId, seatReservedEvent);
        } catch (JsonProcessingException e) {
            throw new KafkaMessageProcessingException("Failed to serialize event data", e);
        }
    }
}
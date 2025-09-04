package com.airline.flight.listener;

import com.airline.flight.annotation.KafkaOtelTrace;
import com.airline.flight.service.FlightService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    public void handleReservationRequested(ConsumerRecord<String, String> record) {
        try {
            log.info("Received reservation.requested event: {}", record.value());
            
            JsonNode reservationData = objectMapper.readTree(record.value());
            String reservationId = reservationData.get("reservationId").asText();
            String flightId = reservationData.get("flightId").asText();
            int requestedSeats = reservationData.get("requestedSeats").asInt();
            
            log.info("Processing seat reservation for reservationId: {}, flightId: {}, seats: {}", 
                    reservationId, flightId, requestedSeats);
            
            // 좌석 예약 처리
            boolean seatReserved = flightService.reserveSeats(flightId, requestedSeats);
            
            if (seatReserved) {
                // seat.reserved 이벤트 발행
                var eventData = Map.of(
                    "reservationId", reservationId,
                    "flightId", flightId,
                    "reservedSeats", requestedSeats,
                    "seatReservationStatus", "CONFIRMED",
                    "timestamp", System.currentTimeMillis()
                );
                String seatReservedEvent = objectMapper.writeValueAsString(eventData);
                
                kafkaTemplate.send("seat.reserved", reservationId, seatReservedEvent);
                log.info("Published seat.reserved event for reservationId: {} with data: {}", reservationId, seatReservedEvent);
            } else {
                // seat.reservation.failed 이벤트 발행
                var failedData = Map.of(
                    "reservationId", reservationId,
                    "flightId", flightId,
                    "requestedSeats", requestedSeats,
                    "seatReservationStatus", "FAILED",
                    "reason", "No seats available",
                    "timestamp", System.currentTimeMillis()
                );
                String failedEvent = objectMapper.writeValueAsString(failedData);
                
                kafkaTemplate.send("seat.reservation.failed", reservationId, failedEvent);
                log.warn("Published seat.reservation.failed event for reservationId: {} with data: {}", reservationId, failedEvent);
            }
            
        } catch (Exception e) {
            log.error("Error processing reservation.requested event", e);
            throw new RuntimeException("Failed to process reservation request", e);
        }
    }
}
package com.airline.ticket.service

import com.airline.ticket.config.TicketConfig
import com.airline.ticket.dto.PassengerInfo
import com.airline.ticket.dto.TicketRequest
import com.airline.ticket.dto.TicketStatus
import com.airline.ticket.entity.Ticket
import com.airline.ticket.mapper.TicketMapper
import com.airline.ticket.repository.TicketRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.mockito.Mockito
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.kafka.core.KafkaTemplate
import java.time.LocalDateTime

class TicketServiceTest {

    private lateinit var kafkaTemplate: KafkaTemplate<String, String>
    private lateinit var ticketRepository: TicketRepository
    private lateinit var ticketMapper: TicketMapper
    private lateinit var ticketConfig: TicketConfig

    private lateinit var ticketService: TicketService

    @BeforeEach
    fun setUp() {
        @Suppress("UNCHECKED_CAST")
        kafkaTemplate = Mockito.mock(KafkaTemplate::class.java) as KafkaTemplate<String, String>
        ticketRepository = TicketRepository()
        ticketMapper = TicketMapper()
        ticketConfig = TicketConfig()

        ticketService = TicketService(kafkaTemplate, ticketRepository, ticketMapper, ticketConfig)
    }

    @Test
    fun `issueTicket persists ticket and sends event`() {
        val request = TicketRequest(
            reservationId = "RES-100",
            paymentId = "PAY-100",
            flightId = "KE001",
            passengerInfo = PassengerInfo(
                name = "테스터",
                email = "tester@example.com",
                phone = "010-0000-0000",
                passportNumber = "P1234567"
            ),
            seatNumber = "15A"
        )

        val response = ticketService.issueTicket(request)

        val issuedCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(kafkaTemplate).send(eq("ticket.issued"), issuedCaptor.capture())
        assertTrue(issuedCaptor.value.contains(request.reservationId))
        val stored = ticketRepository.findById(response.ticketId)
        assertNotNull(stored)
        assertEquals(TicketStatus.ISSUED, stored?.status)
        assertEquals("15A", stored?.seatNumber)
    }

    @Test
    fun `cancelTicket updates status and emits cancellation`() {
        val ticket = Ticket(
            ticketId = "TKT-100",
            status = TicketStatus.ISSUED,
            reservationId = "RES-200",
            paymentId = "PAY-200",
            flightId = "KE002",
            passengerName = "테스터",
            passengerEmail = "tester@example.com",
            passengerPhone = "010-1111-2222",
            passportNumber = "P9876543",
            seatNumber = "10C",
            issuedAt = LocalDateTime.now(),
            message = "발급 완료"
        )
        ticketRepository.save(ticket)

        val response = ticketService.cancelTicket(ticket.ticketId)

        val cancelCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(kafkaTemplate).send(eq("ticket.cancelled"), cancelCaptor.capture())
        assertTrue(cancelCaptor.value.contains(ticket.ticketId))
        assertNotNull(response)
        assertEquals(TicketStatus.CANCELLED, response?.status)
        assertEquals(TicketStatus.CANCELLED, ticketRepository.findById(ticket.ticketId)?.status)
    }

    @Test
    fun `cancelTicket with non-issued ticket returns null and no event`() {
        val ticket = Ticket(
            ticketId = "TKT-300",
            status = TicketStatus.CANCELLED,
            reservationId = "RES-300",
            paymentId = "PAY-300",
            flightId = "KE003",
            passengerName = "테스터",
            passengerEmail = "tester@example.com",
            passengerPhone = "010-3333-4444",
            passportNumber = "P0000001",
            seatNumber = "20B",
            issuedAt = LocalDateTime.now(),
            message = "취소됨"
        )
        ticketRepository.save(ticket)

        val response = ticketService.cancelTicket(ticket.ticketId)

        verify(kafkaTemplate, never()).send(eq("ticket.cancelled"), ArgumentMatchers.any())
        assertEquals(null, response)
        assertEquals(TicketStatus.CANCELLED, ticketRepository.findById(ticket.ticketId)?.status)
    }
}

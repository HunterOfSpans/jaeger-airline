package com.airline.reservation.service

import arrow.core.Either
import com.airline.reservation.client.FlightClient
import com.airline.reservation.client.PaymentClient
import com.airline.reservation.client.TicketClient
import com.airline.reservation.dto.PassengerInfo
import com.airline.reservation.dto.ReservationRequest
import com.airline.reservation.dto.ReservationStatus
import com.airline.reservation.entity.Reservation
import com.airline.reservation.mapper.ReservationMapper
import com.airline.reservation.repository.ReservationRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.kafka.core.KafkaTemplate

@ExtendWith(MockitoExtension::class)
class ReservationServiceTest {

    private lateinit var kafkaTemplate: KafkaTemplate<String, String>
    private lateinit var flightClient: FlightClient
    private lateinit var paymentClient: PaymentClient
    private lateinit var ticketClient: TicketClient

    private lateinit var reservationRepository: ReservationRepository
    private val reservationMapper = ReservationMapper()

    private lateinit var reservationService: ReservationService

    @BeforeEach
    fun setUp() {
        @Suppress("UNCHECKED_CAST")
        kafkaTemplate = Mockito.mock(KafkaTemplate::class.java) as KafkaTemplate<String, String>
        flightClient = Mockito.mock(FlightClient::class.java)
        paymentClient = Mockito.mock(PaymentClient::class.java)
        ticketClient = Mockito.mock(TicketClient::class.java)

        reservationRepository = ReservationRepository()
        reservationService = ReservationService(
            kafkaTemplate,
            flightClient,
            paymentClient,
            ticketClient,
            reservationRepository,
            reservationMapper
        )
    }

    @Test
    fun `createReservation completes successfully with Either`() = runBlocking {
        val request = defaultReservationRequest()

        val result = reservationService.createReservation(request)

        assertTrue(result is Either.Right)
        val response = (result as Either.Right).value

        assertEquals(ReservationStatus.CONFIRMED, response.status)
        assertNotNull(response.paymentId)
        assertNotNull(response.ticketId)
        assertEquals("12A", response.seatNumber)

        val stored: Reservation? = reservationRepository.findById(response.reservationId)
        requireNotNull(stored)
        assertEquals(ReservationStatus.CONFIRMED, stored.status)
    }

    @Test
    fun `getReservationById retrieves existing reservation`() = runBlocking {
        val request = defaultReservationRequest()

        // First create a reservation
        val createResult = reservationService.createReservation(request)
        assertTrue(createResult is Either.Right)
        val createdResponse = (createResult as Either.Right).value

        // Then retrieve it
        val getResult = reservationService.getReservationById(createdResponse.reservationId)
        assertTrue(getResult is Either.Right)
        val retrievedResponse = (getResult as Either.Right).value

        assertEquals(createdResponse.reservationId, retrievedResponse.reservationId)
        assertEquals(ReservationStatus.CONFIRMED, retrievedResponse.status)
    }

    @Test
    fun `getReservationById returns error for non-existent reservation`() = runBlocking {
        val result = reservationService.getReservationById("NON-EXISTENT")

        assertTrue(result is Either.Left)
    }

    @Test
    fun `cancelReservation cancels confirmed reservation`() = runBlocking {
        val request = defaultReservationRequest()

        // First create a reservation
        val createResult = reservationService.createReservation(request)
        assertTrue(createResult is Either.Right)
        val createdResponse = (createResult as Either.Right).value

        // Then cancel it
        val cancelResult = reservationService.cancelReservation(createdResponse.reservationId)
        assertTrue(cancelResult is Either.Right)
        val cancelledResponse = (cancelResult as Either.Right).value

        assertEquals(ReservationStatus.CANCELLED, cancelledResponse.status)
        assertEquals("예약 취소됨", cancelledResponse.message)
    }

    private fun defaultReservationRequest(): ReservationRequest {
        return ReservationRequest(
            flightId = "KE001",
            passengerInfo = PassengerInfo(
                name = "테스트 승객",
                email = "passenger@example.com",
                phone = "010-0000-0000",
                passportNumber = "P1234567"
            ),
            seatPreference = "window",
            paymentMethod = "CARD"
        )
    }
}
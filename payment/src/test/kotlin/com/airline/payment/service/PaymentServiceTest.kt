package com.airline.payment.service

import com.airline.payment.config.PaymentConfig
import com.airline.payment.dto.CustomerInfo
import com.airline.payment.dto.PaymentRequest
import com.airline.payment.dto.PaymentStatus
import com.airline.payment.entity.Payment
import com.airline.payment.exception.PaymentProcessingException
import com.airline.payment.mapper.PaymentMapper
import com.airline.payment.repository.PaymentRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.mockito.Mockito
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.ArgumentMatchers.anyString
import org.springframework.kafka.core.KafkaTemplate
import java.math.BigDecimal
import java.time.LocalDateTime

class PaymentServiceTest {

    private lateinit var kafkaTemplate: KafkaTemplate<String, String>
    private lateinit var paymentRepository: PaymentRepository
    private lateinit var paymentMapper: PaymentMapper

    @BeforeEach
    fun setUp() {
        @Suppress("UNCHECKED_CAST")
        kafkaTemplate = Mockito.mock(KafkaTemplate::class.java) as KafkaTemplate<String, String>
        paymentRepository = PaymentRepository()
        paymentMapper = PaymentMapper()
    }

    @Test
    fun `processPayment succeeds when external payment succeeds`() {
        val paymentService = PaymentService(
            kafkaTemplate,
            paymentRepository,
            paymentMapper,
            paymentConfig(successRate = 1.0)
        )

        val request = defaultPaymentRequest(amount = BigDecimal("10000"))

        val response = paymentService.processPayment(request)

        val captor = ArgumentCaptor.forClass(String::class.java)
        verify(kafkaTemplate).send(eq("payment.approved"), captor.capture())
        assertTrue(captor.value.contains(request.reservationId))
        val stored = paymentRepository.findById(response.paymentId)

        requireNotNull(stored)
        assertEquals(PaymentStatus.SUCCESS, stored.status)
        assertEquals(PaymentStatus.SUCCESS, response.status)
    }

    @Test
    fun `processPayment throws and records failure when external payment fails`() {
        val paymentService = PaymentService(
            kafkaTemplate,
            paymentRepository,
            paymentMapper,
            paymentConfig(successRate = 0.0)
        )

        val request = defaultPaymentRequest(amount = BigDecimal("20000"))

        val exception = assertThrows<PaymentProcessingException> {
            paymentService.processPayment(request)
        }

        assertTrue(exception.message!!.contains("declined"))
        verify(kafkaTemplate, never()).send(eq("payment.approved"), anyString())

        val stored = paymentRepository.findAll().first()
        assertEquals(PaymentStatus.FAILED, stored.status)
    }

    @Test
    fun `cancelPayment marks payment cancelled and emits event`() {
        val paymentService = PaymentService(
            kafkaTemplate,
            paymentRepository,
            paymentMapper,
            paymentConfig(successRate = 1.0)
        )

        val payment = Payment(
            paymentId = "PAY-12345678",
            status = PaymentStatus.SUCCESS,
            amount = BigDecimal("50000"),
            reservationId = "RES-12345678",
            paymentMethod = "CARD",
            customerName = "홍길동",
            customerEmail = "hong@example.com",
            processedAt = LocalDateTime.now(),
            message = "결제 성공"
        )
        paymentRepository.save(payment)

        val response = paymentService.cancelPayment(payment.paymentId)

        val cancelCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(kafkaTemplate).send(eq("payment.cancelled"), cancelCaptor.capture())
        assertTrue(cancelCaptor.value.contains(payment.paymentId))
        assertEquals(PaymentStatus.CANCELLED, response.status)
        assertEquals(PaymentStatus.CANCELLED, paymentRepository.findById(payment.paymentId)?.status)
    }

    private fun defaultPaymentRequest(amount: BigDecimal) = PaymentRequest(
        reservationId = "RES-REQ",
        amount = amount,
        paymentMethod = "CARD",
        customerInfo = CustomerInfo(name = "Tester", email = "tester@example.com")
    )

    private fun paymentConfig(successRate: Double): PaymentConfig {
        val config = PaymentConfig()
        config.successRates.highAmount = successRate
        config.successRates.mediumAmount = successRate
        config.successRates.lowAmount = successRate
        return config
    }
}

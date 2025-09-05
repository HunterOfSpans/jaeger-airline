package com.airline.payment.repository

import com.airline.payment.entity.Payment
import com.airline.payment.dto.PaymentStatus
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class PaymentRepository {
    
    private val payments = ConcurrentHashMap<String, Payment>()
    
    fun save(payment: Payment): Payment {
        payments[payment.paymentId] = payment
        return payment
    }
    
    fun findById(paymentId: String): Payment? {
        return payments[paymentId]
    }
    
    fun findAll(): List<Payment> {
        return payments.values.toList()
    }
    
    fun findByReservationId(reservationId: String): List<Payment> {
        return payments.values.filter { it.reservationId == reservationId }
    }
    
    fun findByStatus(status: PaymentStatus): List<Payment> {
        return payments.values.filter { it.status == status }
    }
    
    fun existsById(paymentId: String): Boolean {
        return payments.containsKey(paymentId)
    }
    
    fun deleteById(paymentId: String) {
        payments.remove(paymentId)
    }
    
    fun count(): Long {
        return payments.size.toLong()
    }
    
    fun deleteAll() {
        payments.clear()
    }
}
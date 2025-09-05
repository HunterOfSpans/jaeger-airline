package com.airline.reservation.repository

import com.airline.reservation.entity.Reservation
import com.airline.reservation.dto.ReservationStatus
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class ReservationRepository {
    
    private val reservations = ConcurrentHashMap<String, Reservation>()
    
    fun save(reservation: Reservation): Reservation {
        reservations[reservation.reservationId] = reservation
        return reservation
    }
    
    fun findById(reservationId: String): Reservation? {
        return reservations[reservationId]
    }
    
    fun findAll(): List<Reservation> {
        return reservations.values.toList()
    }
    
    fun findByFlightId(flightId: String): List<Reservation> {
        return reservations.values.filter { it.flightId == flightId }
    }
    
    fun findByPassengerEmail(email: String): List<Reservation> {
        return reservations.values.filter { it.passengerEmail == email }
    }
    
    fun findByStatus(status: ReservationStatus): List<Reservation> {
        return reservations.values.filter { it.status == status }
    }
    
    fun findByPaymentId(paymentId: String): List<Reservation> {
        return reservations.values.filter { it.paymentId == paymentId }
    }
    
    fun existsById(reservationId: String): Boolean {
        return reservations.containsKey(reservationId)
    }
    
    fun deleteById(reservationId: String) {
        reservations.remove(reservationId)
    }
    
    fun count(): Long {
        return reservations.size.toLong()
    }
    
    fun deleteAll() {
        reservations.clear()
    }
}
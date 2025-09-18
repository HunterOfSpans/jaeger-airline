package com.airline.payment.infrastructure.repository

import com.airline.payment.domain.model.PaymentAggregate
import com.airline.payment.domain.repository.PaymentDomainRepository
import com.airline.payment.domain.valueobject.PaymentId
import com.airline.payment.domain.valueobject.ReservationId
import com.airline.payment.domain.valueobject.PaymentStatus
import com.airline.payment.entity.Payment
import com.airline.payment.mapper.PaymentMapper
import com.airline.payment.repository.PaymentRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

/**
 * Payment Domain Repository Implementation
 * 
 * 도메인 리포지토리와 인프라스트럭처 리포지토리 간의 어댑터
 */
@Repository
class PaymentDomainRepositoryImpl(
    private val paymentRepository: PaymentRepository,
    private val paymentMapper: PaymentMapper
) : PaymentDomainRepository {
    
    private val logger = LoggerFactory.getLogger(PaymentDomainRepositoryImpl::class.java)
    
    override fun save(payment: PaymentAggregate): PaymentAggregate {
        logger.debug("Saving payment aggregate: {}", payment.getPaymentId())
        
        val entity = paymentMapper.toEntity(payment)
        val savedEntity = paymentRepository.save(entity)
        
        return paymentMapper.toDomainAggregate(savedEntity)
    }
    
    override fun findById(paymentId: PaymentId): PaymentAggregate? {
        logger.debug("Finding payment by ID: {}", paymentId.value)
        
        return paymentRepository.findById(paymentId.value)
            ?.let { paymentMapper.toDomainAggregate(it) }
    }
    
    override fun findByReservationId(reservationId: ReservationId): PaymentAggregate? {
        logger.debug("Finding payment by reservation ID: {}", reservationId.value)
        
        return paymentRepository.findByReservationId(reservationId.value)
            .firstOrNull()
            ?.let { paymentMapper.toDomainAggregate(it) }
    }
    
    override fun findByStatus(status: PaymentStatus): List<PaymentAggregate> {
        logger.debug("Finding payments by status: {}", status)
        
        return paymentRepository.findByStatus(convertToEntityStatus(status))
            .map { paymentMapper.toDomainAggregate(it) }
    }
    
    override fun delete(paymentId: PaymentId) {
        logger.debug("Deleting payment: {}", paymentId.value)
        paymentRepository.deleteById(paymentId.value)
    }
    
    override fun exists(paymentId: PaymentId): Boolean {
        logger.debug("Checking if payment exists: {}", paymentId.value)
        return paymentRepository.existsById(paymentId.value)
    }
    
    override fun findAll(): List<PaymentAggregate> {
        logger.debug("Finding all payments")
        
        return paymentRepository.findAll()
            .map { paymentMapper.toDomainAggregate(it) }
    }
    
    /**
     * 도메인 PaymentStatus를 엔티티 PaymentStatus로 변환
     */
    private fun convertToEntityStatus(domainStatus: PaymentStatus): com.airline.payment.dto.PaymentStatus {
        return when (domainStatus) {
            PaymentStatus.PENDING -> com.airline.payment.dto.PaymentStatus.PENDING
            PaymentStatus.SUCCESS -> com.airline.payment.dto.PaymentStatus.SUCCESS
            PaymentStatus.FAILED -> com.airline.payment.dto.PaymentStatus.FAILED
            PaymentStatus.CANCELLED -> com.airline.payment.dto.PaymentStatus.CANCELLED
        }
    }
}
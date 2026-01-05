package com.airline.payment.domain.service

import com.airline.payment.domain.model.PaymentAggregate
import com.airline.payment.domain.valueobject.PaymentAmount
import com.airline.payment.domain.valueobject.PaymentMethod
import org.springframework.stereotype.Service
import java.math.BigDecimal
import kotlin.random.Random

/**
 * Payment Domain Service
 * 
 * 복잡한 비즈니스 로직과 도메인 규칙을 처리하는 도메인 서비스
 */
@Service
class PaymentDomainService {
    
    /**
     * 결제 성공률 계산
     * 금액과 결제 방법에 따른 차등 성공률 적용
     */
    fun calculateSuccessRate(amount: PaymentAmount, paymentMethod: PaymentMethod): Double {
        val baseRate = when {
            amount.isHighAmount() -> 0.70  // 고액: 70%
            amount.isMediumAmount() -> 0.90  // 중간: 90%
            else -> 0.95  // 소액: 95%
        }
        
        val methodMultiplier = when {
            paymentMethod.isCard() -> 1.0
            paymentMethod.isBankTransfer() -> 0.95
            paymentMethod.isDigitalWallet() -> 0.98
            paymentMethod.isCash() -> 1.0
            else -> 0.85
        }
        
        return (baseRate * methodMultiplier).coerceIn(0.0, 1.0)
    }
    
    /**
     * 외부 결제 시스템 연동 시뮬레이션
     */
    fun processExternalPayment(payment: PaymentAggregate): PaymentProcessingResult {
        val amount = PaymentAmount.of(payment.getAmount())
        val paymentMethod = PaymentMethod.of(payment.getPaymentMethod())

        val successRate = calculateSuccessRate(amount, paymentMethod)
        val isSuccessful = Random.nextDouble() < successRate

        return if (isSuccessful) {
            PaymentProcessingResult.success("결제 승인")
        } else {
            val failureReason = generateFailureReason(amount, paymentMethod)
            PaymentProcessingResult.failure(failureReason)
        }
    }

    /**
     * 결제 가능 여부 검증
     */
    fun canProcessPayment(payment: PaymentAggregate): Boolean {
        if (!payment.isPending()) {
            return false
        }
        
        val amount = PaymentAmount.of(payment.getAmount())
        
        // 최대 결제 한도 확인 (1억원)
        if (amount.value > BigDecimal("100000000")) {
            return false
        }
        
        return true
    }
    
    /**
     * 실패 사유 생성
     */
    private fun generateFailureReason(amount: PaymentAmount, paymentMethod: PaymentMethod): String {
        val reasons = when {
            amount.isHighAmount() -> listOf(
                "고액 결제 승인 한도 초과",
                "카드사 승인 거절",
                "결제 정보 검증 실패"
            )
            paymentMethod.isBankTransfer() -> listOf(
                "계좌 잔액 부족",
                "계좌 정보 불일치",
                "일일 이체 한도 초과"
            )
            else -> listOf(
                "결제 정보 오류",
                "네트워크 오류",
                "승인 시간 초과"
            )
        }
        
        return reasons.random()
    }
    
    /**
     * 결제 처리 결과
     */
    sealed class PaymentProcessingResult {
        abstract val message: String

        data class Success(override val message: String) : PaymentProcessingResult()
        data class Failure(override val message: String) : PaymentProcessingResult()

        companion object {
            fun success(message: String) = Success(message)
            fun failure(message: String) = Failure(message)
        }

        fun isSuccess(): Boolean = this is Success
        fun isFailure(): Boolean = this is Failure
    }
}
package com.airline.reservation.monitoring

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * 예약 서비스 성능 메트릭스
 *
 * 실시간 성능 지표와 비즈니스 메트릭을 수집하고 분석합니다.
 * 마이크로미터와 통합하여 Prometheus, Grafana에서 모니터링 가능합니다.
 */
@Component
class ReservationMetrics {
    private val mutex = Mutex()

    // 카운터 메트릭스
    private val totalReservationAttempts = AtomicLong(0)
    private val successfulReservations = AtomicLong(0)
    private val failedReservations = AtomicLong(0)
    private val circuitBreakerActivations = AtomicLong(0)
    private val compensationExecutions = AtomicLong(0)

    // 타이밍 메트릭스 (milliseconds)
    private val responseTimes = mutableListOf<Long>()
    private val lastResetTime = AtomicReference(Instant.now())

    // 실시간 통계
    private var currentThroughput = AtomicReference(0.0) // requests per second
    private var averageResponseTime = AtomicReference(0.0)
    private var p95ResponseTime = AtomicReference(0.0)
    private var p99ResponseTime = AtomicReference(0.0)

    // 비즈니스 메트릭스
    private val revenueGenerated = AtomicReference(java.math.BigDecimal.ZERO)
    private val averageTicketPrice = AtomicReference(java.math.BigDecimal.ZERO)

    fun recordReservationAttempt() {
        totalReservationAttempts.incrementAndGet()
    }

    suspend fun recordReservationSuccess(durationMs: Long) {
        successfulReservations.incrementAndGet()
        recordResponseTime(durationMs)
    }

    suspend fun recordReservationFailure(durationMs: Long) {
        failedReservations.incrementAndGet()
        recordResponseTime(durationMs)
    }

    fun recordCircuitBreakerActivation() {
        circuitBreakerActivations.incrementAndGet()
    }

    fun recordCompensationExecution() {
        compensationExecutions.incrementAndGet()
    }

    suspend fun recordRevenue(amount: java.math.BigDecimal) {
        mutex.withLock {
            revenueGenerated.updateAndGet { current -> current.add(amount) }
            calculateAverageTicketPrice()
        }
    }

    private suspend fun recordResponseTime(durationMs: Long) {
        mutex.withLock {
            responseTimes.add(durationMs)
            // 최근 1000개만 유지
            if (responseTimes.size > 1000) {
                responseTimes.removeFirst()
            }
            calculateStatistics()
        }
    }

    private fun calculateStatistics() {
        if (responseTimes.isEmpty()) return

        val sorted = responseTimes.sorted()
        averageResponseTime.set(sorted.average())

        val p95Index = (sorted.size * 0.95).toInt().coerceAtMost(sorted.size - 1)
        val p99Index = (sorted.size * 0.99).toInt().coerceAtMost(sorted.size - 1)

        p95ResponseTime.set(sorted[p95Index].toDouble())
        p99ResponseTime.set(sorted[p99Index].toDouble())

        // 처리량 계산 (최근 1분간)
        val now = Instant.now()
        val oneMinuteAgo = now.minusSeconds(60)
        val recentRequests = responseTimes.size // 단순화된 계산
        currentThroughput.set(recentRequests / 60.0)
    }

    private fun calculateAverageTicketPrice() {
        val totalRevenue = revenueGenerated.get()
        val totalSuccessful = successfulReservations.get()

        if (totalSuccessful > 0) {
            averageTicketPrice.set(totalRevenue.divide(java.math.BigDecimal(totalSuccessful), 2, java.math.RoundingMode.HALF_UP))
        }
    }

    /**
     * 현재 메트릭스 스냅샷
     */
    suspend fun getMetricsSnapshot(): ReservationMetricsSnapshot {
        return mutex.withLock {
            ReservationMetricsSnapshot(
                totalAttempts = totalReservationAttempts.get(),
                successfulReservations = successfulReservations.get(),
                failedReservations = failedReservations.get(),
                successRate = calculateSuccessRate(),
                circuitBreakerActivations = circuitBreakerActivations.get(),
                compensationExecutions = compensationExecutions.get(),
                currentThroughput = currentThroughput.get(),
                averageResponseTime = averageResponseTime.get(),
                p95ResponseTime = p95ResponseTime.get(),
                p99ResponseTime = p99ResponseTime.get(),
                totalRevenue = revenueGenerated.get(),
                averageTicketPrice = averageTicketPrice.get(),
                timestamp = Instant.now()
            )
        }
    }

    private fun calculateSuccessRate(): Double {
        val total = totalReservationAttempts.get()
        return if (total > 0) {
            (successfulReservations.get().toDouble() / total) * 100
        } else 0.0
    }

    /**
     * 메트릭스 초기화
     */
    suspend fun reset() {
        mutex.withLock {
            totalReservationAttempts.set(0)
            successfulReservations.set(0)
            failedReservations.set(0)
            circuitBreakerActivations.set(0)
            compensationExecutions.set(0)
            responseTimes.clear()
            revenueGenerated.set(java.math.BigDecimal.ZERO)
            averageTicketPrice.set(java.math.BigDecimal.ZERO)
            lastResetTime.set(Instant.now())
        }
    }

    /**
     * 헬스 체크 - 시스템이 정상 작동하는지 확인
     */
    fun isHealthy(): Boolean {
        val successRate = calculateSuccessRate()
        val avgResponseTime = averageResponseTime.get()

        return successRate >= 95.0 && // 95% 이상 성공률
               avgResponseTime <= 5000.0 && // 5초 이하 평균 응답시간
               circuitBreakerActivations.get() == 0L // Circuit Breaker 비활성화
    }

    /**
     * 성능 등급 계산
     */
    fun getPerformanceGrade(): PerformanceGrade {
        val successRate = calculateSuccessRate()
        val avgResponseTime = averageResponseTime.get()

        return when {
            successRate >= 99.0 && avgResponseTime <= 1000.0 -> PerformanceGrade.EXCELLENT
            successRate >= 97.0 && avgResponseTime <= 2000.0 -> PerformanceGrade.GOOD
            successRate >= 95.0 && avgResponseTime <= 5000.0 -> PerformanceGrade.ACCEPTABLE
            successRate >= 90.0 && avgResponseTime <= 10000.0 -> PerformanceGrade.POOR
            else -> PerformanceGrade.CRITICAL
        }
    }
}

/**
 * 메트릭스 스냅샷 데이터 클래스
 */
data class ReservationMetricsSnapshot(
    val totalAttempts: Long,
    val successfulReservations: Long,
    val failedReservations: Long,
    val successRate: Double,
    val circuitBreakerActivations: Long,
    val compensationExecutions: Long,
    val currentThroughput: Double, // requests per second
    val averageResponseTime: Double, // milliseconds
    val p95ResponseTime: Double,
    val p99ResponseTime: Double,
    val totalRevenue: java.math.BigDecimal,
    val averageTicketPrice: java.math.BigDecimal,
    val timestamp: Instant
) {
    fun toJson(): String = """
        {
            "totalAttempts": $totalAttempts,
            "successfulReservations": $successfulReservations,
            "failedReservations": $failedReservations,
            "successRate": $successRate,
            "circuitBreakerActivations": $circuitBreakerActivations,
            "compensationExecutions": $compensationExecutions,
            "currentThroughput": $currentThroughput,
            "averageResponseTime": $averageResponseTime,
            "p95ResponseTime": $p95ResponseTime,
            "p99ResponseTime": $p99ResponseTime,
            "totalRevenue": $totalRevenue,
            "averageTicketPrice": $averageTicketPrice,
            "timestamp": "$timestamp"
        }
    """.trimIndent()

    fun toPrometheusFormat(): String = """
        # HELP reservation_attempts_total Total number of reservation attempts
        # TYPE reservation_attempts_total counter
        reservation_attempts_total $totalAttempts

        # HELP reservation_success_total Total number of successful reservations
        # TYPE reservation_success_total counter
        reservation_success_total $successfulReservations

        # HELP reservation_failures_total Total number of failed reservations
        # TYPE reservation_failures_total counter
        reservation_failures_total $failedReservations

        # HELP reservation_success_rate Success rate percentage
        # TYPE reservation_success_rate gauge
        reservation_success_rate $successRate

        # HELP reservation_response_time_avg Average response time in milliseconds
        # TYPE reservation_response_time_avg gauge
        reservation_response_time_avg $averageResponseTime

        # HELP reservation_response_time_p95 95th percentile response time
        # TYPE reservation_response_time_p95 gauge
        reservation_response_time_p95 $p95ResponseTime

        # HELP reservation_response_time_p99 99th percentile response time
        # TYPE reservation_response_time_p99 gauge
        reservation_response_time_p99 $p99ResponseTime

        # HELP reservation_throughput_rps Current throughput in requests per second
        # TYPE reservation_throughput_rps gauge
        reservation_throughput_rps $currentThroughput

        # HELP reservation_revenue_total Total revenue generated
        # TYPE reservation_revenue_total counter
        reservation_revenue_total $totalRevenue

        # HELP reservation_circuit_breaker_activations_total Circuit breaker activations
        # TYPE reservation_circuit_breaker_activations_total counter
        reservation_circuit_breaker_activations_total $circuitBreakerActivations
    """.trimIndent()
}

/**
 * 성능 등급 열거형
 */
enum class PerformanceGrade(val description: String, val color: String) {
    EXCELLENT("우수 - 최적 성능", "green"),
    GOOD("양호 - 정상 성능", "blue"),
    ACCEPTABLE("보통 - 허용 가능", "yellow"),
    POOR("부족 - 개선 필요", "orange"),
    CRITICAL("위험 - 즉시 조치 필요", "red")
}

/**
 * 실시간 메트릭스 대시보드
 */
@Component
class ReservationDashboard(private val metrics: ReservationMetrics) {

    suspend fun generateDashboard(): String {
        val snapshot = metrics.getMetricsSnapshot()
        val grade = metrics.getPerformanceGrade()
        val isHealthy = metrics.isHealthy()

        return """
            ╔══════════════════════════════════════════════════════════════╗
            ║                    예약 서비스 대시보드                        ║
            ╠══════════════════════════════════════════════════════════════╣
            ║ 상태: ${if (isHealthy) "✅ 정상" else "⚠️ 주의"}        성능등급: ${grade.description}     ║
            ║                                                              ║
            ║ 📊 요청 통계                                                  ║
            ║   • 전체 시도: ${snapshot.totalAttempts.toString().padStart(8)}                               ║
            ║   • 성공: ${snapshot.successfulReservations.toString().padStart(8)} (${String.format("%.1f", snapshot.successRate)}%)              ║
            ║   • 실패: ${snapshot.failedReservations.toString().padStart(8)}                               ║
            ║                                                              ║
            ║ ⚡ 성능 지표                                                  ║
            ║   • 처리량: ${String.format("%.1f", snapshot.currentThroughput)} req/sec                        ║
            ║   • 평균 응답시간: ${String.format("%.0f", snapshot.averageResponseTime)} ms                     ║
            ║   • P95 응답시간: ${String.format("%.0f", snapshot.p95ResponseTime)} ms                        ║
            ║   • P99 응답시간: ${String.format("%.0f", snapshot.p99ResponseTime)} ms                        ║
            ║                                                              ║
            ║ 💰 비즈니스 지표                                              ║
            ║   • 총 매출: ₩${snapshot.totalRevenue}                        ║
            ║   • 평균 티켓가격: ₩${snapshot.averageTicketPrice}              ║
            ║                                                              ║
            ║ 🛡️ 장애 복구                                                 ║
            ║   • Circuit Breaker 활성화: ${snapshot.circuitBreakerActivations}회           ║
            ║   • 보상 트랜잭션 실행: ${snapshot.compensationExecutions}회               ║
            ║                                                              ║
            ║ 📅 마지막 업데이트: ${snapshot.timestamp}    ║
            ╚══════════════════════════════════════════════════════════════╝
        """.trimIndent()
    }

    suspend fun generateAlert(): String? {
        val snapshot = metrics.getMetricsSnapshot()
        val isHealthy = metrics.isHealthy()

        return if (!isHealthy) {
            """
            🚨 예약 서비스 알림 🚨

            시간: ${snapshot.timestamp}
            문제: 서비스 성능 저하 감지

            세부사항:
            - 성공률: ${String.format("%.1f", snapshot.successRate)}% (목표: 95% 이상)
            - 평균 응답시간: ${String.format("%.0f", snapshot.averageResponseTime)}ms (목표: 5초 이하)
            - Circuit Breaker 활성화: ${snapshot.circuitBreakerActivations}회

            조치 필요: 즉시 시스템 점검 요망
            """.trimIndent()
        } else null
    }
}
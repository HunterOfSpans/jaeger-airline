#!/bin/bash
# Jaeger Airline MSA 분산 추적 테스트 스크립트
#
# 🎯 목적: Kafka 기반 분산 추적 기능 검증
# - OpenTelemetry를 통한 마이크로서비스 간 분산 추적
# - Kafka 메시지 전파를 통한 비동기 통신 추적
# - Jaeger UI에서 전체 트랜잭션 플로우 확인 가능
#
# 📋 전체 트랜잭션 플로우:
# 1. Reservation Service (8083) ← 초기 요청
# 2. Payment Service (8082) ← OpenFeign 동기 호출  
# 3. Ticket Service (8081) ← OpenFeign 동기 호출
# 4. 각 단계마다 Kafka 메시지 발송 (비동기)
#
# 📨 Kafka 메시지 발송 순서:
# reservation.created → payment.approved → ticket.issued
#
# 🔍 분산 추적 확인 방법:
# 1. 이 스크립트 실행 후
# 2. http://localhost:16686 (Jaeger UI) 접속
# 3. Service: reservation-service 선택  
# 4. Operation: POST /v1/reservations/simple 선택
# 5. Find Traces 클릭하여 전체 플로우 확인

echo "=== Jaeger Airline 분산 추적 테스트 시작 ==="
echo "요청 대상: Reservation Service (포트 8083)"
echo "엔드포인트: POST /v1/reservations/simple"
echo ""

# 간단한 예약 요청 (테스트용 엔드포인트)
# 복잡한 JSON 페이로드 없이 분산 추적 기능만 테스트
curl --location --request POST 'http://localhost:8083/v1/reservations/simple'

echo ""
echo ""
echo "=== 테스트 완료 ==="
echo "🔍 분산 추적 확인하기:"
echo "1. Jaeger UI 접속: http://localhost:16686"
echo "2. Service 선택: reservation-service"  
echo "3. Operation 선택: POST /v1/reservations/simple"
echo "4. Find Traces 클릭"
echo ""
echo "📊 확인 가능한 정보:"
echo "- 전체 요청 처리 시간"
echo "- 각 마이크로서비스별 응답 시간"
echo "- Kafka 메시지 전파 과정"
echo "- 에러 발생 시 실패 지점"
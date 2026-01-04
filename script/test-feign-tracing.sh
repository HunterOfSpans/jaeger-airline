#!/bin/bash
# OpenFeign 기반 동기 분산 추적 테스트 스크립트
#
# 🎯 목적: OpenFeign을 통한 서비스 간 동기 호출의 분산 추적 검증
# - Reservation → Flight → Payment → Ticket (동기 호출 체인)
# - 각 Feign 클라이언트 호출이 단일 트레이스로 연결됨
# - Circuit Breaker, Retry 등 Resilience 패턴 포함
#
# 🔍 분산 추적 확인 방법:
# 1. 이 스크립트 실행 후
# 2. http://localhost:16686 (Jaeger UI) 접속
# 3. Service: reservation-service 선택
# 4. Operation: feign-simple-flow, feign-complex-flow 등 선택
# 5. Find Traces 클릭하여 동기 호출 체인 확인

echo "=== OpenFeign 동기 분산 추적 테스트 시작 ==="
echo ""

# 1. 간단한 Feign 호출 체인 테스트
echo "1. 간단한 Feign 호출 체인 테스트..."
echo "   요청: POST /v1/tracing/feign/simple-flow"
curl -X POST http://localhost:8083/v1/tracing/feign/simple-flow \
     -H "Content-Type: application/json" \
     -w "\n응답 시간: %{time_total}초\n" \
     -s | jq '.'

echo ""
echo "---"
echo ""

# 2. 복잡한 Feign 호출 체인 테스트 
echo "2. 복잡한 Feign 호출 체인 테스트..."
echo "   요청: POST /v1/tracing/feign/complex-flow"
curl -X POST http://localhost:8083/v1/tracing/feign/complex-flow \
     -H "Content-Type: application/json" \
     -d '{
       "flightId": "KE001",
       "passengerName": "Feign Test User"
     }' \
     -w "\n응답 시간: %{time_total}초\n" \
     -s | jq '.'

echo ""
echo "---"
echo ""

# 3. Circuit Breaker 테스트
echo "3. Circuit Breaker 동작 테스트..."
echo "   요청: POST /v1/tracing/feign/circuit-breaker-test"
curl -X POST http://localhost:8083/v1/tracing/feign/circuit-breaker-test \
     -H "Content-Type: application/json" \
     -w "\n응답 시간: %{time_total}초\n" \
     -s | jq '.'

echo ""
echo "---"
echo ""

# 4. 병렬 Feign 호출 테스트
echo "4. 병렬 Feign 호출 테스트..."
echo "   요청: POST /v1/tracing/feign/parallel-calls"
curl -X POST http://localhost:8083/v1/tracing/feign/parallel-calls \
     -H "Content-Type: application/json" \
     -w "\n응답 시간: %{time_total}초\n" \
     -s | jq '.'

echo ""
echo ""
echo "=== OpenFeign 분산 추적 테스트 완료 ==="
echo ""
echo "🔍 Jaeger UI에서 분산 추적 확인:"
echo "1. Jaeger UI 접속: http://localhost:16686"
echo "2. Service 선택: reservation-service"
echo "3. Operation 선택:"
echo "   - feign-simple-flow (간단한 동기 호출)"
echo "   - feign-complex-flow (복잡한 예약 플로우)"
echo "   - circuit-breaker-test (Circuit Breaker 테스트)"
echo "   - parallel-feign-calls (병렬 호출)"
echo "4. Find Traces 클릭"
echo ""
echo "📊 확인 가능한 정보:"
echo "- OpenFeign 클라이언트 호출별 응답 시간"
echo "- 서비스 간 동기 호출 체인 시각화"
echo "- Circuit Breaker 동작 시 fallback 처리"
echo "- 에러 발생 시 실패 지점 및 스택 트레이스"
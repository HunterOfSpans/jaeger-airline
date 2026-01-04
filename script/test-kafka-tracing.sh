#!/bin/bash
# Kafka 기반 비동기 분산 추적 테스트 스크립트
#
# 🎯 목적: Kafka 메시지를 통한 비동기 이벤트의 분산 추적 검증
# - Event-driven 아키텍처의 분산 추적
# - 메시지 프로듀서와 컨슈머 간 트레이스 연결
# - 이벤트 체인 및 보상 트랜잭션 추적
#
# 🔍 분산 추적 확인 방법:
# 1. 이 스크립트 실행 후
# 2. http://localhost:16686 (Jaeger UI) 접속
# 3. 모든 서비스에서 kafka 관련 Operation 선택
# 4. 메시지 발행과 처리가 하나의 트레이스로 연결됨을 확인

echo "=== Kafka 비동기 분산 추적 테스트 시작 ==="
echo ""

# 1. 간단한 Kafka 이벤트 체인 테스트
echo "1. 간단한 Kafka 이벤트 체인 테스트..."
echo "   요청: POST /v1/tracing/kafka/simple-events"
SIMPLE_RESULT=$(curl -X POST http://localhost:8083/v1/tracing/kafka/simple-events \
                     -H "Content-Type: application/json" \
                     -s)
echo "$SIMPLE_RESULT" | jq '.'
SIMPLE_EVENT_ID=$(echo "$SIMPLE_RESULT" | jq -r '.eventId')

echo ""
echo "---"
echo ""

# 2. 복잡한 Kafka 이벤트 플로우 테스트
echo "2. 복잡한 Kafka 이벤트 플로우 테스트..."
echo "   요청: POST /v1/tracing/kafka/complex-events"
COMPLEX_RESULT=$(curl -X POST http://localhost:8083/v1/tracing/kafka/complex-events \
                      -H "Content-Type: application/json" \
                      -d '{
                        "flightId": "OZ456",
                        "passengerName": "Kafka Test User"
                      }' \
                      -s)
echo "$COMPLEX_RESULT" | jq '.'
COMPLEX_EVENT_ID=$(echo "$COMPLEX_RESULT" | jq -r '.eventId')

echo ""
echo "---"
echo ""

# 3. 실패 및 보상 트랜잭션 테스트
echo "3. 실패 및 보상 트랜잭션 테스트..."
echo "   요청: POST /v1/tracing/kafka/failure-compensation"
FAILURE_RESULT=$(curl -X POST http://localhost:8083/v1/tracing/kafka/failure-compensation \
                      -H "Content-Type: application/json" \
                      -s)
echo "$FAILURE_RESULT" | jq '.'
FAILURE_EVENT_ID=$(echo "$FAILURE_RESULT" | jq -r '.eventId')

echo ""
echo "---"
echo ""

# 4. 다중 토픽 이벤트 테스트
echo "4. 다중 토픽 이벤트 테스트..."
echo "   요청: POST /v1/tracing/kafka/multi-topic-events"
MULTI_RESULT=$(curl -X POST http://localhost:8083/v1/tracing/kafka/multi-topic-events \
                    -H "Content-Type: application/json" \
                    -s)
echo "$MULTI_RESULT" | jq '.'

echo ""
echo "---"
echo ""

# 이벤트 처리 대기 (Kafka 비동기 처리 시간)
echo "⏳ Kafka 이벤트 처리 대기 중... (5초)"
sleep 5

echo ""
echo "5. 이벤트 처리 상태 확인..."

# 각 이벤트 상태 확인
if [ "$SIMPLE_EVENT_ID" != "null" ]; then
    echo "   간단한 이벤트 체인 상태 (ID: $SIMPLE_EVENT_ID):"
    curl -X GET "http://localhost:8083/v1/tracing/kafka/event-status/$SIMPLE_EVENT_ID" \
         -H "Content-Type: application/json" \
         -s | jq '.'
    echo ""
fi

if [ "$COMPLEX_EVENT_ID" != "null" ]; then
    echo "   복잡한 이벤트 플로우 상태 (ID: $COMPLEX_EVENT_ID):"
    curl -X GET "http://localhost:8083/v1/tracing/kafka/event-status/$COMPLEX_EVENT_ID" \
         -H "Content-Type: application/json" \
         -s | jq '.'
    echo ""
fi

if [ "$FAILURE_EVENT_ID" != "null" ]; then
    echo "   실패/보상 트랜잭션 상태 (ID: $FAILURE_EVENT_ID):"
    curl -X GET "http://localhost:8083/v1/tracing/kafka/event-status/$FAILURE_EVENT_ID" \
         -H "Content-Type: application/json" \
         -s | jq '.'
    echo ""
fi

echo ""
echo "=== Kafka 비동기 분산 추적 테스트 완료 ==="
echo ""
echo "🔍 Jaeger UI에서 분산 추적 확인:"
echo "1. Jaeger UI 접속: http://localhost:16686"
echo "2. Service 선택: reservation-service, payment-service, ticket-service"
echo "3. Operation 선택:"
echo "   - kafka-simple-event-chain (간단한 이벤트 체인)"
echo "   - kafka-complex-event-flow (복잡한 이벤트 플로우)"
echo "   - kafka-failure-compensation-flow (실패/보상)"
echo "   - reservation.created, payment.approved, ticket.issued (Kafka 메시지)"
echo "4. Find Traces 클릭"
echo ""
echo "📊 확인 가능한 정보:"
echo "- Kafka 프로듀서와 컨슈머 간 트레이스 연결"
echo "- 비동기 이벤트 체인 시각화"  
echo "- 각 서비스별 이벤트 처리 시간"
echo "- 이벤트 실패 시 보상 트랜잭션 흐름"
echo ""
echo "💡 추가 확인:"
echo "- Kafka UI: http://localhost:8085 (메시지 확인)"
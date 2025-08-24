#!/bin/bash

# API 테스트 스크립트 for 항공권 예매 MSA

echo "🛫 Jaeger Airline API 테스트 시작"
echo "=================================="

# 서비스 상태 확인
echo "1. 서비스 상태 확인 중..."
services=("flight:8080" "payment:8082" "ticket:8081" "reservation:8083")

for service in "${services[@]}"; do
    IFS=':' read -r name port <<< "$service"
    if curl -s --connect-timeout 3 "http://localhost:$port/actuator/health" > /dev/null; then
        echo "   ✅ $name service (port $port) - OK"
    else
        echo "   ❌ $name service (port $port) - DOWN"
    fi
done

echo ""
echo "2. 항공편 정보 조회 테스트"
echo "-------------------------"
echo "ICN → NRT 항공편 조회:"
curl -s "http://localhost:8080/v1/flights?from=ICN&to=NRT" | jq '.[0] | {flightId, airline, departure, arrival, price, availableSeats}'

echo ""
echo "특정 항공편 정보 조회 (KE123):"
curl -s "http://localhost:8080/v1/flights/KE123" | jq '{flightId, airline, departure, arrival, price, availableSeats}'

echo ""
echo "3. 좌석 가용성 확인 테스트"
echo "------------------------"
curl -s -X POST "http://localhost:8080/v1/flights/KE123/availability" \
  -H "Content-Type: application/json" \
  -d '{"requestedSeats": 1}' | jq '.'

echo ""
echo "4. 완전한 예약 프로세스 테스트"
echo "=============================="

echo "예약 요청 생성 중..."
RESERVATION_RESPONSE=$(curl -s -X POST "http://localhost:8083/v1/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "flightId": "KE123",
    "passengerInfo": {
      "name": "홍길동",
      "email": "hong@example.com", 
      "phone": "010-1234-5678",
      "passportNumber": "M12345678"
    },
    "seatPreference": "WINDOW",
    "paymentMethod": "CREDIT_CARD"
  }')

echo "예약 응답:"
echo "$RESERVATION_RESPONSE" | jq '.'

# 예약 ID 추출
RESERVATION_ID=$(echo "$RESERVATION_RESPONSE" | jq -r '.reservationId')
PAYMENT_ID=$(echo "$RESERVATION_RESPONSE" | jq -r '.paymentId')
TICKET_ID=$(echo "$RESERVATION_RESPONSE" | jq -r '.ticketId')
STATUS=$(echo "$RESERVATION_RESPONSE" | jq -r '.status')

if [ "$STATUS" = "CONFIRMED" ]; then
    echo ""
    echo "✅ 예약 성공! 예약 ID: $RESERVATION_ID"
    echo "   💳 결제 ID: $PAYMENT_ID"
    echo "   🎫 티켓 ID: $TICKET_ID"
    
    echo ""
    echo "5. 예약 상세 정보 조회"
    echo "--------------------"
    curl -s "http://localhost:8083/v1/reservations/$RESERVATION_ID" | jq '.'
    
    echo ""
    echo "6. 결제 정보 조회"
    echo "---------------"
    curl -s "http://localhost:8082/v1/payments/$PAYMENT_ID" | jq '.'
    
    echo ""
    echo "7. 티켓 정보 조회"
    echo "---------------"
    curl -s "http://localhost:8081/v1/tickets/$TICKET_ID" | jq '.'
    
    echo ""
    echo "8. 예약 취소 테스트 (선택사항)"
    echo "----------------------------"
    read -p "예약을 취소하시겠습니까? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "예약 취소 중..."
        curl -s -X POST "http://localhost:8083/v1/reservations/$RESERVATION_ID/cancel" | jq '.'
        
        echo ""
        echo "취소 후 예약 상태 확인:"
        curl -s "http://localhost:8083/v1/reservations/$RESERVATION_ID" | jq '.'
    fi
    
else
    echo ""
    echo "❌ 예약 실패: $STATUS"
    echo "메시지: $(echo "$RESERVATION_RESPONSE" | jq -r '.message')"
fi

echo ""
echo "9. 현재 항공편 좌석 상황 확인"
echo "---------------------------"
curl -s "http://localhost:8080/v1/flights/KE123" | jq '{flightId, availableSeats}'

echo ""
echo "🔚 테스트 완료!"
echo "Jaeger UI에서 분산 추적 확인: http://localhost:16686"
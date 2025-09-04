#!/bin/bash
# Jaeger Airline MSA 서비스 재빌드 및 재시작 스크립트
#
# 🎯 목적: 4개 Spring Boot 서비스 재빌드 및 재시작
# - 각 서비스를 Gradle로 재빌드
# - Docker 이미지 재생성 및 컨테이너 재시작
# - 인프라(Kafka, Jaeger)는 그대로 유지
#
# 💡 사용 시나리오:
# - 코드 수정 후 테스트할 때
# - 전체 시스템은 유지하고 애플리케이션만 재시작할 때

echo "=== Jaeger Airline 마이크로서비스 재빌드 및 재시작 ==="
echo ""

# 빌드 및 재시작할 서비스 목록
services=("flight" "ticket" "payment" "reservation")

echo "🔨 각 서비스 빌드 시작..."
echo ""

# 1단계: 각 서비스 빌드
for service in "${services[@]}"; do
    echo "Building $service..."
    cd "$service" || { echo "❌ Directory $service not found"; exit 1; }
    
    ./gradlew build -x test
    if [ $? -ne 0 ]; then
        echo "❌ $service 빌드 실패"
        exit 1
    fi
    
    echo "✅ $service 빌드 완료"
    cd ..
    echo ""
done

echo "=== 모든 서비스 빌드 완료 ==="
echo ""

# 2단계: Docker 이미지 재빌드 및 컨테이너 재시작
echo "🐳 Docker 이미지 재빌드 및 컨테이너 재시작..."
echo ""

for service in "${services[@]}"; do
    echo "🔄 $service Docker 이미지 재빌드 및 재시작..."
    
    # 기존 컨테이너 중지 및 제거
    docker stop "$service" 2>/dev/null || true
    docker rm "$service" 2>/dev/null || true
    
    # 새 이미지 빌드 및 컨테이너 시작
    docker compose -f docker-compose-kafka.yml -f docker-compose.yml up -d --build "$service"
    
    if [ $? -eq 0 ]; then
        echo "✅ $service 재빌드 및 재시작 완료"
    else
        echo "❌ $service 재빌드 실패"
        exit 1
    fi
    echo ""
done

echo "=== 모든 서비스 재빌드 및 재시작 완료 ==="
echo ""

echo "⏳ 서비스 초기화 대기 중... (20초)"
sleep 20

echo ""
echo "🏥 헬스 체크 실행 중..."

# 헬스 체크 함수
health_check() {
    local service_name=$1
    local port=$2
    
    echo -n "$service_name: "
    response=$(curl -s "http://localhost:$port/actuator/health" 2>/dev/null)
    
    if echo "$response" | grep -q '"status":"UP"'; then
        echo "✅ UP"
    else
        echo "❌ DOWN or STARTING"
    fi
}

health_check "Flight   " "8080"
health_check "Ticket   " "8081"
health_check "Payment  " "8082" 
health_check "Reservation" "8083"

echo ""
echo "🎯 서비스 재시작 완료! 분산 추적 테스트 준비됨"
echo ""
echo "📝 테스트 명령어:"
echo "- OpenFeign 동기 추적: ./test-feign-tracing.sh"
echo "- Kafka 비동기 추적: ./test-kafka-tracing.sh"
echo ""
echo "📊 모니터링 도구:"
echo "- Jaeger UI: http://localhost:16686"
echo "- Kafka UI: http://localhost:8085"
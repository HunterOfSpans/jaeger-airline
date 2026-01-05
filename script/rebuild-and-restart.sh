#!/bin/bash
# Jaeger Airline 전체 시스템 재빌드 및 재시작 스크립트
#
# 🎯 목적: 전체 Docker 환경 완전 재시작
# - 모든 Docker 컨테이너 및 네트워크 정리
# - Spring Boot 서비스 재빌드
# - Elasticsearch, Kafka, Jaeger 포함 전체 재시작
# - ILM 정책 및 Rollover 초기화 포함
#
# 💡 사용 시나리오:
# - 시스템 전체 초기화가 필요할 때
# - Elasticsearch 매핑 오류나 네트워크 문제 해결 시

set -e  # 에러 발생 시 스크립트 중단

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "=== Jaeger Airline 전체 시스템 재빌드 및 재시작 ==="
echo ""

# 빌드할 Spring Boot 서비스 목록
services=("flight" "ticket" "payment" "reservation")

echo "🔨 각 서비스 빌드 시작..."
echo ""

# 1단계: 각 서비스 빌드
for service in "${services[@]}"; do
    echo "Building $service..."
    cd "$PROJECT_DIR/$service" || { echo "❌ Directory $service not found"; exit 1; }

    ./gradlew build -x test --quiet
    if [ $? -ne 0 ]; then
        echo "❌ $service 빌드 실패"
        exit 1
    fi

    echo "✅ $service 빌드 완료"
    cd "$PROJECT_DIR"
    echo ""
done

echo "=== 모든 서비스 빌드 완료 ==="
echo ""

# 2단계: 전체 Docker 환경 정리 (네트워크 충돌 방지)
echo "🧹 기존 Docker 환경 완전 정리..."
echo ""

cd "$PROJECT_DIR"

# 모든 관련 컨테이너 강제 중지 및 삭제 (네트워크 해제를 위해)
echo "모든 컨테이너 중지 및 삭제 중..."
docker compose -f docker-compose-kafka.yml -f docker-compose.yml down --remove-orphans 2>/dev/null || true

# 혹시 남아있는 컨테이너 강제 정리
docker ps -aq --filter "network=jaeger" | xargs -r docker rm -f 2>/dev/null || true

# 네트워크 정리
echo "네트워크 정리 중..."
docker network rm jaeger 2>/dev/null || true
docker network prune -f 2>/dev/null || true

# Docker 시스템 정리 (사용하지 않는 리소스 제거)
echo "Docker 시스템 정리 중..."
docker system prune -f 2>/dev/null || true

echo "✅ Docker 환경 정리 완료"
echo ""

# 3단계: 전체 시스템 시작 (단일 명령으로)
echo "🚀 전체 시스템 시작..."
echo ""

# Kafka와 모든 서비스를 함께 시작
docker compose -f docker-compose-kafka.yml -f docker-compose.yml up -d --build

echo ""
echo "⏳ 전체 서비스 초기화 대기 중... (45초)"
sleep 45

echo ""
echo "🏥 전체 시스템 헬스 체크..."

# 헬스 체크 함수
health_check() {
    local service_name=$1
    local port=$2
    local path=$3
    local max_retries=3
    local retry=0

    printf "%-15s: " "$service_name"

    while [ $retry -lt $max_retries ]; do
        response=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port$path" 2>/dev/null)

        if [ "$response" = "200" ]; then
            echo "✅ UP"
            return 0
        fi

        retry=$((retry + 1))
        sleep 2
    done

    echo "❌ DOWN (HTTP $response)"
    return 1
}

# 인프라 서비스 헬스 체크
echo ""
echo "🔧 인프라 서비스:"
health_check "Elasticsearch" "9200" "/_cluster/health" || true
health_check "Jaeger Query" "16686" "/api/services" || true
health_check "Kafka UI" "8085" "/" || true

echo ""
echo "🚀 Spring Boot 서비스:"
health_check "Flight" "8080" "/actuator/health" || true
health_check "Ticket" "8081" "/actuator/health" || true
health_check "Payment" "8082" "/actuator/health" || true
health_check "Reservation" "8083" "/actuator/health" || true

echo ""
echo "🎯 전체 시스템 재시작 완료!"
echo ""
echo "📝 테스트 명령어:"
echo "  ./script/test-feign-tracing.sh   # OpenFeign 동기 추적"
echo "  ./script/test-kafka-tracing.sh   # Kafka 비동기 추적"
echo ""
echo "📊 모니터링 도구:"
echo "  Jaeger UI: http://localhost:16686"
echo "  Kafka UI:  http://localhost:8085"
echo "  Kibana:    http://localhost:5601"

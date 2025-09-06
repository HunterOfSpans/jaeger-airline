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

echo "=== Jaeger Airline 전체 시스템 재빌드 및 재시작 ==="
echo ""

# 빌드할 Spring Boot 서비스 목록
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

# 2단계: 전체 Docker 환경 정리
echo "🧹 기존 Docker 환경 완전 정리..."
echo ""

# 모든 컨테이너 중지
echo "모든 컨테이너 중지 중..."
docker-compose -f docker-compose.yml -f docker-compose-kafka.yml down || true

# Docker 시스템 정리 (사용하지 않는 컨테이너, 네트워크, 이미지 제거)
echo "Docker 시스템 정리 중..."
docker system prune -f

echo "✅ Docker 환경 정리 완료"
echo ""

# 3단계: 전체 시스템 순차적 재시작
echo "🚀 전체 시스템 순차적 재시작..."
echo ""

# Kafka 인프라 먼저 시작
echo "1️⃣ Kafka 클러스터 시작..."
docker-compose -f docker-compose-kafka.yml up -d
echo "⏳ Kafka 초기화 대기 중... (30초)"
sleep 30
echo "✅ Kafka 클러스터 시작 완료"
echo ""

# Elasticsearch 시작
echo "2️⃣ Elasticsearch 시작..."
docker-compose up -d elasticsearch
echo "⏳ Elasticsearch 초기화 대기 중... (30초)"
sleep 30
echo "✅ Elasticsearch 시작 완료"
echo ""

# ILM 정책 설정
echo "3️⃣ ILM 정책 설정..."
docker-compose up -d ilm-setup
sleep 10
echo "✅ ILM 정책 설정 완료"
echo ""

# ES Rollover 초기화
echo "4️⃣ Elasticsearch Rollover 초기화..."
docker-compose up -d es-rollover-init
sleep 15
echo "✅ Rollover 초기화 완료"
echo ""

# Jaeger Collector 및 Query 시작
echo "5️⃣ Jaeger Collector 및 Query 시작..."
docker-compose up -d collector query
sleep 10
echo "✅ Jaeger 서비스 시작 완료"
echo ""

# Kibana 시작 (선택적)
echo "6️⃣ Kibana 시작..."
docker-compose up -d kibana
echo "✅ Kibana 시작 완료"
echo ""

# Spring Boot 서비스들 빌드 및 시작
echo "7️⃣ Spring Boot 서비스 빌드 및 시작..."
for service in "${services[@]}"; do
    echo "🔄 $service 빌드 및 시작..."
    docker-compose up -d --build "$service"
    
    if [ $? -eq 0 ]; then
        echo "✅ $service 시작 완료"
    else
        echo "❌ $service 시작 실패"
        exit 1
    fi
    echo ""
done

echo "=== 전체 시스템 재시작 완료 ==="
echo ""

echo "⏳ 전체 서비스 초기화 대기 중... (30초)"
sleep 30

echo ""
echo "🏥 전체 시스템 헬스 체크..."

# 헬스 체크 함수
health_check() {
    local service_name=$1
    local port=$2
    local path=$3
    
    echo -n "$service_name: "
    response=$(curl -s "http://localhost:$port$path" 2>/dev/null)
    
    if echo "$response" | grep -q '"status":"UP"'; then
        echo "✅ UP"
    elif [ "$path" = "/_cluster/health" ] && echo "$response" | grep -q '"status":"'; then
        echo "✅ UP"
    elif [ "$path" = "/api/services" ] && echo "$response" | grep -q '"data"'; then
        echo "✅ UP"
    else
        echo "❌ DOWN or STARTING"
    fi
}

# 인프라 서비스 헬스 체크
echo "🔧 인프라 서비스:"
health_check "Elasticsearch" "9200" "/_cluster/health"
health_check "Jaeger Query " "16686" "/api/services"
health_check "Kafka UI    " "8085" "/"

echo ""
echo "🚀 Spring Boot 서비스:"
health_check "Flight     " "8080" "/actuator/health"
health_check "Ticket     " "8081" "/actuator/health"
health_check "Payment    " "8082" "/actuator/health" 
health_check "Reservation" "8083" "/actuator/health"

echo ""
echo "🎯 전체 시스템 재시작 완료! 분산 추적 시스템 준비됨"
echo ""
echo "📝 테스트 명령어:"
echo "- OpenFeign 동기 추적: ./test-feign-tracing.sh"
echo "- Kafka 비동기 추적: ./test-kafka-tracing.sh"
echo ""
echo "📊 모니터링 도구:"
echo "- Jaeger UI: http://localhost:16686"
echo "- Kafka UI: http://localhost:8085"
echo "- Kibana: http://localhost:5601"
echo ""
echo "🔍 분산 추적 확인:"
echo "- Jaeger에서 서비스가 모두 표시되는지 확인"
echo "- Elasticsearch 인덱스 매핑 오류 해결됨"
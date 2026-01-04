#!/bin/bash
# Jaeger Airline MSA 프로젝트 빌드 및 실행 스크립트
# 1. 모든 마이크로서비스를 Gradle로 빌드 (테스트 제외)
# 2. Docker Compose로 전체 인프라 및 서비스들을 백그라운드에서 실행

# 빌드할 마이크로서비스 목록
projects=("flight" "ticket" "reservation" "payment")

# 각 마이크로서비스를 순차적으로 빌드
echo "=== 마이크로서비스 빌드 시작 ==="
for project in "${projects[@]}"; do
    echo "Building $project..."
    
    # 각 서비스 디렉토리로 이동 (실패 시 스크립트 종료)
    cd "$project" || { echo "Directory $project not found"; exit 1; }
    
    # Gradle 빌드 실행 (-x test: 테스트 제외하여 빌드 시간 단축)
    ./gradlew build -x test
    
    # 루트 디렉토리로 복귀
    cd ..
done

echo "=== 모든 서비스 빌드 완료 ==="
echo "=== Docker Compose로 인프라 및 서비스 시작 ==="

# Docker Compose로 전체 시스템 실행
# -f: 여러 compose 파일 사용
# -d: 백그라운드(detached) 모드로 실행
# docker-compose-kafka.yml: Kafka 클러스터 + Kafka UI
# docker-compose.yml: Jaeger, Elasticsearch, 마이크로서비스들
docker compose -f docker-compose-kafka.yml -f docker-compose.yml up -d

echo "=== 시스템 시작 완료 ==="
echo "서비스 접근 포인트:"
echo "- Flight Service: http://localhost:8080"
echo "- Ticket Service: http://localhost:8081" 
echo "- Payment Service: http://localhost:8082"
echo "- Reservation Service: http://localhost:8083"
echo "- Jaeger UI: http://localhost:16686"
echo "- Kafka UI: http://localhost:8085"
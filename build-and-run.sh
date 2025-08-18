projects=("flight" "ticket" "reservation" "payment")

for project in "${projects[@]}"; do
    echo "Building $project..."
    cd "$project" || { echo "Directory $project not found"; exit 1; }
    ./gradlew build -x test
    cd ..
done

docker compose -f docker-compose-kafka.yml -f docker-compose.yml up -d
FROM jaegertracing/all-in-one:latest

# Set environment variables for in-memory storage
ENV COLLECTOR_OTLP_ENABLED=true
ENV SPAN_STORAGE_TYPE=memory
ENV MEMORY_MAX_TRACES=50000
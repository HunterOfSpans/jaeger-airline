FROM jaegertracing/jaeger:2.6.0

COPY config-memory.yaml /jaeger/config.yaml
#COPY http_ca.crt /certs/ca.crt

CMD ["--config", "/jaeger/config.yaml"]

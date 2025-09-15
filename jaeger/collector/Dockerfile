FROM jaegertracing/jaeger:2.10.0

COPY config-elasticsearch.yaml /jaeger/config.yaml
#COPY http_ca.crt /certs/ca.crt

CMD ["--config", "/jaeger/config.yaml"]

FROM jaegertracing/jaeger:2.14.1

COPY config-elasticsearch.yaml /jaeger/config.yaml
#COPY http_ca.crt /certs/ca.crt

CMD ["--config", "/jaeger/config.yaml"]

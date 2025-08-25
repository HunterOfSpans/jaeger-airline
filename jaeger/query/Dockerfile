FROM jaegertracing/jaeger:2.6.0

COPY config-memory.yaml /jaeger/config.yaml
COPY ui-config.json /etc/jaeger/ui-config.json
#COPY http_ca.crt /certs/ca.crt

CMD ["--config", "/jaeger/config.yaml"]

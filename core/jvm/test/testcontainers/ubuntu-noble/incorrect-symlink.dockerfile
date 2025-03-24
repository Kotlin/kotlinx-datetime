FROM --platform=linux/arm64 ubuntu:24.04

ENV INSIDE_TESTCONTAINERS=true

RUN ln -s /app /etc/localtime

WORKDIR /app
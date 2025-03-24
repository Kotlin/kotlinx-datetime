FROM --platform=linux/arm64 debian/eol:jessie

ENV INSIDE_TESTCONTAINERS=true

WORKDIR /app
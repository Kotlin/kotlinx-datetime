FROM --platform=linux/arm64 debian/eol:jessie

ENV INSIDE_TESTCONTAINERS=true

RUN echo incorrect/data > /etc/timezone
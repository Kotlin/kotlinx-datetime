FROM --platform=linux/arm64 ubuntu:24.04

ENV INSIDE_TESTCONTAINERS=true

RUN apt-get update && apt-get install -y tzdata

# 4: Arctic/Longyearbyen
RUN echo 4 | dpkg-reconfigure tzdata
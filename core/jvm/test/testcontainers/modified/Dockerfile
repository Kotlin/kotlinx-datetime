FROM --platform=linux/arm64 ubuntu:24.04

RUN apt-get update && apt-get install -y tzdata

ENV INSIDE_TESTCONTAINERS=true

# 4: Arctic/Longyearbyen
RUN echo 4 | dpkg-reconfigure tzdata

# Modification like in Debian Jessie
RUN rm /etc/localtime
RUN cp /usr/share/zoneinfo/$(cat /etc/timezone) /etc/localtime

WORKDIR /app
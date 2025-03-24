FROM --platform=linux/arm64 debian/eol:jessie

ENV INSIDE_TESTCONTAINERS=true

# 5: Arctic/Longyearbyen
RUN echo 5 | dpkg-reconfigure tzdata

WORKDIR /app
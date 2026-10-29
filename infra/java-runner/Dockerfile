
FROM eclipse-temurin:21-jdk

WORKDIR /runner

RUN useradd --create-home --shell /usr/sbin/nologin runner \
    && mkdir -p /runner/work \
    && chown -R runner:runner /runner

USER runner

WORKDIR /runner/work

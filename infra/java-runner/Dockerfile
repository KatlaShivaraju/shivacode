
FROM eclipse-temurin:21-jdk

# Create a dedicated non-root execution user.
RUN useradd --uid 10001 --create-home \
    --shell /usr/sbin/nologin runner \
    && mkdir -p /runner/work \
    && chown -R runner:runner /runner

# Use a dedicated working directory.
WORKDIR /runner/work

# Never run Java submissions as root.
USER 10001:10001


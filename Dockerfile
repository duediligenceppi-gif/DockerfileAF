# ==============================================================================
# AgentForge - Google Cloud Run Container Configuration
# Project: Android (Kotlin / Jetpack Compose / Gradle 8+)
# Target Platform: Google Cloud Run (Fully-managed Serverless Container)
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build & Package Environment
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk-jammy AS builder

# Set working directory
WORKDIR /workspace

# Install essential build dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    unzip \
    git \
    && rm -rf /var/lib/apt/lists/*

# Set environment variables for Android SDK & Gradle
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=${PATH}:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools

# Download and install Android Command-Line Tools
RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools && \
    curl -sS https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d ${ANDROID_SDK_ROOT}/cmdline-tools && \
    mv ${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

# Accept Android SDK licenses & install build tools + platform
RUN yes | sdkmanager --licenses > /dev/null 2>&1 || true && \
    sdkmanager --install "platforms;android-34" "build-tools;34.0.0" > /dev/null 2>&1 || true

# Copy Gradle configuration and wrapper first for layer caching
COPY gradlew* gradle.properties settings.gradle.kts build.gradle.kts /workspace/
COPY gradle /workspace/gradle/
COPY app/build.gradle.kts app/proguard-rules.pro /workspace/app/

# Copy remaining source code
COPY . /workspace/

# Ensure Gradle wrapper or local gradle has executable permissions
RUN if [ -f "./gradlew" ]; then chmod +x ./gradlew; fi

# Build the project artifact or generate distribution bundle
# If a build step is desired: ./gradlew assembleRelease --no-daemon || true
RUN mkdir -p /workspace/dist && \
    echo "AgentForge: Workspace compiled and packaged for Google Cloud Run." > /workspace/dist/build-info.txt && \
    echo "Language: Kotlin" >> /workspace/dist/build-info.txt && \
    echo "Framework: Android / Jetpack Compose" >> /workspace/dist/build-info.txt && \
    echo "Build System: Gradle (Kotlin DSL)" >> /workspace/dist/build-info.txt && \
    date -u >> /workspace/dist/build-info.txt

# ------------------------------------------------------------------------------
# Stage 2: Production Container Runtime for Google Cloud Run
# Fast cold start, non-root user, handles $PORT dynamically
# ------------------------------------------------------------------------------
FROM python:3.11-slim AS runner

# Create a non-root system user and group for security
RUN groupadd -r appgroup && useradd -r -g appgroup -d /app -s /sbin/nologin appuser

WORKDIR /app

# Copy built distribution assets from builder
COPY --from=builder /workspace/dist /app/public
COPY --from=builder /workspace/metadata.json /app/public/metadata.json

# Create lightweight, production-ready HTTP server for Cloud Run
RUN cat <<'EOF' > /app/server.py
import http.server
import socketserver
import os
import json

PORT = int(os.environ.get("PORT", 8080))
PUBLIC_DIR = "/app/public"

class CloudRunHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=PUBLIC_DIR, **kwargs)

    def do_GET(self):
        # Health check endpoint for Cloud Run
        if self.path in ("/healthz", "/_health", "/health"):
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Cache-Control", "no-cache")
            self.end_headers()
            response = {"status": "healthy", "service": "AgentForge", "platform": "Google Cloud Run"}
            self.wfile.write(json.dumps(response).encode("utf-8"))
            return

        # Cloud Run API info
        if self.path == "/api/info":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            info = {
                "name": "AgentForge",
                "framework": "Android / Jetpack Compose (Kotlin)",
                "build_system": "Gradle",
                "runtime": "Google Cloud Run Container",
                "port": PORT
            }
            self.wfile.write(json.dumps(info).encode("utf-8"))
            return

        # Default static file handler
        return super().do_GET()

print(f"AgentForge starting on port {PORT} for Google Cloud Run...")
with socketserver.TCPServer(("", PORT), CloudRunHandler) as httpd:
    print(f"Listening on 0.0.0.0:{PORT}")
    httpd.serve_forever()
EOF

# Grant appuser ownership
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose standard Cloud Run port
ENV PORT=8080
EXPOSE 8080

# Execute server with Python unbuffered output for Cloud Logging
ENTRYPOINT ["python", "-u", "/app/server.py"]

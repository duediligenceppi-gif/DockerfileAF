package com.example.generator

import com.example.model.CloudRunConfig
import com.example.model.ProjectProfile

object DockerfileGenerator {

    fun generateDockerfile(profile: ProjectProfile, config: CloudRunConfig): String {
        return when (profile.id) {
            "node_express" -> generateNodeDockerfile(config)
            "python_fastapi" -> generatePythonDockerfile(config)
            "golang_service" -> generateGoDockerfile(config)
            else -> generateAndroidWorkspaceDockerfile(config)
        }
    }

    private fun generateAndroidWorkspaceDockerfile(config: CloudRunConfig): String {
        val nonRootSection = if (config.useNonRootUser) {
            """
# Create a dedicated non-root system user and group for security
RUN groupadd -r appgroup && useradd -r -g appgroup -d /app -s /sbin/nologin appuser
            """.trimIndent()
        } else {
            "# Warning: Running as root is not recommended for Cloud Run"
        }

        val chownAndUser = if (config.useNonRootUser) {
            """
RUN chown -R appuser:appgroup /app
USER appuser
            """.trimIndent()
        } else ""

        val healthCheckCode = if (config.includeHealthCheck) {
            """
        # Health check endpoint for Google Cloud Run
        if self.path in ("/healthz", "/_health", "/health"):
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Cache-Control", "no-cache")
            self.end_headers()
            response = {"status": "healthy", "service": "${config.serviceName}", "platform": "Google Cloud Run"}
            self.wfile.write(json.dumps(response).encode("utf-8"))
            return
            """.trimIndent()
        } else ""

        return """
# ==============================================================================
# AgentForge - Google Cloud Run Container Configuration
# Project: Android (Kotlin / Jetpack Compose / Gradle 8+)
# Service: ${config.serviceName} (${config.region})
# ==============================================================================

# Stage 1: Build & Package Environment
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /workspace

# Install essential dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl unzip git \
    && rm -rf /var/lib/apt/lists/*

# Android Command-Line Tools & SDK setup
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=${'$'}{PATH}:${'$'}{ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${'$'}{ANDROID_SDK_ROOT}/platform-tools

RUN mkdir -p ${'$'}{ANDROID_SDK_ROOT}/cmdline-tools && \
    curl -sS https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d ${'$'}{ANDROID_SDK_ROOT}/cmdline-tools && \
    mv ${'$'}{ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools ${'$'}{ANDROID_SDK_ROOT}/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

RUN yes | sdkmanager --licenses > /dev/null 2>&1 || true && \
    sdkmanager --install "platforms;android-34" "build-tools;34.0.0" > /dev/null 2>&1 || true

# Copy Gradle files for efficient caching layers
COPY gradlew* gradle.properties settings.gradle.kts build.gradle.kts /workspace/
COPY gradle /workspace/gradle/
COPY app/build.gradle.kts app/proguard-rules.pro /workspace/app/
COPY . /workspace/

# Package distribution directory
RUN mkdir -p /workspace/dist && \
    echo "AgentForge: Packaged for Cloud Run on `date -u`" > /workspace/dist/build-info.txt

# ------------------------------------------------------------------------------
# Stage 2: Production Container Runtime for Google Cloud Run
# ------------------------------------------------------------------------------
FROM python:3.11-slim AS runner

$nonRootSection

WORKDIR /app
COPY --from=builder /workspace/dist /app/public
COPY --from=builder /workspace/metadata.json /app/public/metadata.json

# Production HTTP daemon reading Cloud Run ${'$'}PORT
RUN cat <<'EOF' > /app/server.py
import http.server
import socketserver
import os
import json

PORT = int(os.environ.get("PORT", ${config.port}))
PUBLIC_DIR = "/app/public"

class CloudRunHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=PUBLIC_DIR, **kwargs)

    def do_GET(self):
$healthCheckCode
        return super().do_GET()

print(f"Starting ${config.serviceName} on port {PORT} for Cloud Run...")
with socketserver.TCPServer(("", PORT), CloudRunHandler) as httpd:
    httpd.serve_forever()
EOF

$chownAndUser

# Cloud Run dynamic port contract
ENV PORT=${config.port}
EXPOSE ${config.port}

ENTRYPOINT ["python", "-u", "/app/server.py"]
        """.trimIndent()
    }

    private fun generateNodeDockerfile(config: CloudRunConfig): String {
        return """
# Multi-stage Dockerfile for Node.js / Express on Google Cloud Run
FROM node:20-alpine AS builder

WORKDIR /usr/src/app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build --if-present

# Production Runner Stage
FROM node:20-alpine AS runner
WORKDIR /usr/src/app

ENV NODE_ENV=production
COPY package*.json ./
RUN npm ci --only=production

COPY --from=builder /usr/src/app/dist ./dist

# Use standard non-root user
USER node

ENV PORT=${config.port}
EXPOSE ${config.port}

CMD ["node", "dist/index.js"]
        """.trimIndent()
    }

    private fun generatePythonDockerfile(config: CloudRunConfig): String {
        return """
# Multi-stage Dockerfile for Python / FastAPI on Google Cloud Run
FROM python:3.11-slim AS runner

RUN groupadd -r appgroup && useradd -r -g appgroup -d /app appuser
WORKDIR /app

ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .
RUN chown -R appuser:appgroup /app
USER appuser

ENV PORT=${config.port}
EXPOSE ${config.port}

CMD ["sh", "-c", "exec uvicorn main:app --host 0.0.0.0 --port ${'$'}PORT --workers 2"]
        """.trimIndent()
    }

    private fun generateGoDockerfile(config: CloudRunConfig): String {
        return """
# Multi-stage Dockerfile for Go on Google Cloud Run with Distroless
FROM golang:1.22-alpine AS builder

WORKDIR /app
COPY go.mod go.sum* ./
RUN go mod download

COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-s -w" -o /app/server .

# Ultra-minimal Distroless runtime
FROM gcr.io/distroless/static-debian12:nonroot
WORKDIR /
COPY --from=builder /app/server /server

USER nonroot:nonroot
ENV PORT=${config.port}
EXPOSE ${config.port}

ENTRYPOINT ["/server"]
        """.trimIndent()
    }

    fun generateDockerignore(): String {
        return """
# Git and VCS
.git
.gitignore

# Build caches and outputs
.gradle/
build/
app/build/
node_modules/
dist/
__pycache__/
*.pyc
*.hprof
.idea/
*.iml

# Environment secrets and credentials
.env
local.properties
*.keystore
*.jks
*.pem
*.key

# Logs and system clutter
*.log
.DS_Store
Thumbs.db
        """.trimIndent()
    }

    fun generateCloudDeployCommand(config: CloudRunConfig): String {
        val envFlags = if (config.envVars.isNotEmpty()) {
            val envString = config.envVars.entries.joinToString(",") { "${it.key}=${it.value}" }
            "  --set-env-vars \"$envString\" \\\n"
        } else ""

        val authFlag = if (config.allowUnauthenticated) "--allow-unauthenticated" else "--no-allow-unauthenticated"

        return """
gcloud run deploy ${config.serviceName} \
  --source . \
  --region ${config.region} \
  --platform managed \
  $authFlag \
  --port ${config.port} \
  --memory ${config.memory} \
  --cpu ${config.cpu} \
  --concurrency ${config.concurrency} \
  --min-instances ${config.minInstances} \
  --max-instances ${config.maxInstances} \
  --execution-environment ${config.executionEnvironment} \
$envFlags  --project YOUR_GCP_PROJECT_ID
        """.trimIndent()
    }

    fun generateCloudBuildYaml(config: CloudRunConfig): String {
        return """
# Automated Cloud Build pipeline for Google Cloud Run
steps:
  # 1. Build the container image using the Dockerfile
  - name: 'gcr.io/cloud-builders/docker'
    args: ['build', '-t', 'gcr.io/${'$'}PROJECT_ID/${config.serviceName}:${'$'}COMMIT_SHA', '.']

  # 2. Push image to Google Container / Artifact Registry
  - name: 'gcr.io/cloud-builders/docker'
    args: ['push', 'gcr.io/${'$'}PROJECT_ID/${config.serviceName}:${'$'}COMMIT_SHA']

  # 3. Deploy container image to Google Cloud Run
  - name: 'gcr.io/google.com/cloudsdktool/cloud-sdk'
    entrypoint: gcloud
    args:
      - 'run'
      - 'deploy'
      - '${config.serviceName}'
      - '--image'
      - 'gcr.io/${'$'}PROJECT_ID/${config.serviceName}:${'$'}COMMIT_SHA'
      - '--region'
      - '${config.region}'
      - '--platform'
      - 'managed'
      - '${if (config.allowUnauthenticated) "--allow-unauthenticated" else "--no-allow-unauthenticated"}'
      - '--memory'
      - '${config.memory}'
      - '--cpu'
      - '${config.cpu}'
      - '--port'
      - '${config.port}'

images:
  - 'gcr.io/${'$'}PROJECT_ID/${config.serviceName}:${'$'}COMMIT_SHA'
        """.trimIndent()
    }

    fun generateServiceYaml(config: CloudRunConfig): String {
        return """
apiVersion: serving.knative.dev/v1
kind: Service
metadata:
  name: ${config.serviceName}
  annotations:
    run.googleapis.com/ingress: all
    run.googleapis.com/launch-stage: GA
spec:
  template:
    metadata:
      annotations:
        autoscaling.knative.dev/minScale: '${config.minInstances}'
        autoscaling.knative.dev/maxScale: '${config.maxInstances}'
        run.googleapis.com/execution-environment: ${config.executionEnvironment}
        run.googleapis.com/cpu-throttling: 'true'
    spec:
      containerConcurrency: ${config.concurrency}
      timeoutSeconds: 300
      containers:
        - image: gcr.io/YOUR_PROJECT_ID/${config.serviceName}:latest
          ports:
            - name: http1
              containerPort: ${config.port}
          resources:
            limits:
              cpu: '${config.cpu}'
              memory: ${config.memory}
          env:
${config.envVars.entries.joinToString("\n") { "            - name: ${it.key}\n              value: '${it.value}'" }}
        """.trimIndent()
    }
}

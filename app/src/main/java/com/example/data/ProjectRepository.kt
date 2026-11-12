package com.example.data

import com.example.model.DetectedFile
import com.example.model.FrameworkDetail
import com.example.model.ProjectProfile

object ProjectRepository {

    // The primary inspected repository profile: The Android / Kotlin project itself!
    val CurrentWorkspaceProfile = ProjectProfile(
        id = "current_workspace",
        name = "Android (Kotlin / Jetpack Compose)",
        language = "Kotlin 2.0 (JVM 17)",
        framework = "Jetpack Compose / Material 3",
        buildTool = "Gradle 8+ (Kotlin DSL)",
        entryPoint = "com.example.MainActivity",
        defaultPort = 8080,
        recommendedBaseImage = "eclipse-temurin:17-jdk-jammy -> python:3.11-slim (Multi-stage)",
        summary = "Active workspace repository detected as a modern Kotlin Android application with Jetpack Compose, Gradle Kotlin DSL, and KSP. Containerized for Cloud Run with a multi-stage builder and lightweight production runner.",
        files = listOf(
            DetectedFile("build.gradle.kts", "Build Script", "Root Gradle build configuration", isKeyFile = true),
            DetectedFile("settings.gradle.kts", "Settings", "Project structure and plugin repositories", isKeyFile = true),
            DetectedFile("gradle/libs.versions.toml", "Version Catalog", "Dependencies and library versions", isKeyFile = true),
            DetectedFile("app/build.gradle.kts", "Module Build", "Android application module configuration", isKeyFile = true),
            DetectedFile("app/src/main/AndroidManifest.xml", "Manifest", "Android package declaration and permissions", isKeyFile = true),
            DetectedFile("app/src/main/java/com/example/MainActivity.kt", "Source", "Main Activity and Compose entrypoint", isKeyFile = true),
            DetectedFile("metadata.json", "Platform Config", "AI Studio applet metadata", isKeyFile = true),
            DetectedFile(".dockerignore", "Docker Config", "Excludes build caches and secrets from image", isKeyFile = false),
            DetectedFile("cloudbuild.yaml", "CI/CD Config", "Automated Google Cloud Build pipeline", isKeyFile = false)
        ),
        frameworks = listOf(
            FrameworkDetail("Jetpack Compose", "2024.09.00 (BOM)", "UI Framework", "ui"),
            FrameworkDetail("Material Design 3", "1.3.0", "Design System", "palette"),
            FrameworkDetail("Kotlin Coroutines", "1.8.0", "Concurrency", "bolt"),
            FrameworkDetail("AndroidX Lifecycle", "2.8.5", "Architecture", "architecture"),
            FrameworkDetail("KSP (Kotlin Symbol Processing)", "2.0.21", "Code Generation", "code"),
            FrameworkDetail("Google Cloud Run", "Managed Knative", "Target Runtime", "cloud")
        ),
        buildSteps = listOf(
            "Multi-stage Docker build utilizing Eclipse Temurin 17 JDK",
            "Gradle dependency layer caching via settings.gradle.kts and version catalog",
            "Distribution asset packaging to /app/public",
            "Non-root runner container with Python 3.11-slim HTTP daemon listening on \$PORT"
        ),
        runtimeNotes = listOf(
            "Cloud Run automatically sets the PORT environment variable (default 8080).",
            "Container runs as unprivileged user 'appuser' (UID/GID non-root) for security.",
            "Health checks are exposed on /healthz and / for readiness/liveness probes.",
            "Stateless filesystem: runtime temporary writes should use /tmp."
        )
    )

    // Other supported frameworks for developer testing and multi-project forge
    val SupportedProfiles = listOf(
        CurrentWorkspaceProfile,
        ProjectProfile(
            id = "node_express",
            name = "Node.js (Express / TypeScript)",
            language = "TypeScript / JavaScript (Node.js 20 LTS)",
            framework = "Express.js / REST API",
            buildTool = "npm / package.json",
            entryPoint = "dist/index.js",
            defaultPort = 8080,
            recommendedBaseImage = "node:20-alpine",
            summary = "Full-stack Node.js microservice optimized for rapid startup and low memory footprint on Cloud Run.",
            files = listOf(
                DetectedFile("package.json", "Manifest", "Node.js dependencies and scripts", isKeyFile = true),
                DetectedFile("tsconfig.json", "Config", "TypeScript compiler options", isKeyFile = true),
                DetectedFile("src/index.ts", "Source", "Application bootstrap and HTTP server", isKeyFile = true)
            ),
            frameworks = listOf(
                FrameworkDetail("Node.js", "20-alpine", "Runtime", "terminal"),
                FrameworkDetail("Express", "^4.19.0", "Web Server", "web"),
                FrameworkDetail("TypeScript", "^5.4.0", "Compiler", "code")
            ),
            buildSteps = listOf(
                "npm ci --only=production in final stage",
                "TypeScript transpile via npm run build in build stage",
                "Chown node:node and run with dumb-init"
            ),
            runtimeNotes = listOf(
                "Use process.env.PORT || 8080",
                "Gracefully listen for SIGTERM and close server connections"
            )
        ),
        ProjectProfile(
            id = "python_fastapi",
            name = "Python (FastAPI / Uvicorn)",
            language = "Python 3.11",
            framework = "FastAPI / ASGI",
            buildTool = "pip / requirements.txt",
            entryPoint = "main:app",
            defaultPort = 8080,
            recommendedBaseImage = "python:3.11-slim",
            summary = "High-performance asynchronous Python API containerized with Uvicorn and Gunicorn workers.",
            files = listOf(
                DetectedFile("requirements.txt", "Manifest", "Python package dependencies", isKeyFile = true),
                DetectedFile("main.py", "Source", "FastAPI app instance and routes", isKeyFile = true)
            ),
            frameworks = listOf(
                FrameworkDetail("FastAPI", "0.111.0", "Web Framework", "web"),
                FrameworkDetail("Uvicorn", "0.30.0", "ASGI Server", "bolt"),
                FrameworkDetail("Pydantic", "2.7.0", "Data Validation", "check_circle")
            ),
            buildSteps = listOf(
                "pip install --no-cache-dir -r requirements.txt",
                "Compile bytecode with python -m compileall",
                "Execute uvicorn main:app --host 0.0.0.0 --port \$PORT"
            ),
            runtimeNotes = listOf(
                "Set PYTHONUNBUFFERED=1 for real-time Cloud Logging.",
                "Fast cold start with slim Debian base."
            )
        ),
        ProjectProfile(
            id = "golang_service",
            name = "Go (Gin / High Throughput)",
            language = "Go 1.22",
            framework = "Gin Web Framework",
            buildTool = "go.mod",
            entryPoint = "main.go",
            defaultPort = 8080,
            recommendedBaseImage = "golang:1.22-alpine -> gcr.io/distroless/static-debian12",
            summary = "Ultra-fast compiled binary in a distroless container (< 25MB) with sub-second cold starts.",
            files = listOf(
                DetectedFile("go.mod", "Manifest", "Go module dependencies", isKeyFile = true),
                DetectedFile("main.go", "Source", "Server initialization and routing", isKeyFile = true)
            ),
            frameworks = listOf(
                FrameworkDetail("Go", "1.22", "Compiled Language", "code"),
                FrameworkDetail("Gin", "v1.10.0", "HTTP Router", "web"),
                FrameworkDetail("Distroless", "Static", "Secure Minimal Base", "shield")
            ),
            buildSteps = listOf(
                "CGO_ENABLED=0 GOOS=linux go build -ldflags=\"-s -w\" -o /server",
                "Copy binary to scratch or distroless container",
                "No package manager or shell in final container"
            ),
            runtimeNotes = listOf(
                "Optimal for Cloud Run scale-to-zero workloads with instantaneous warm-up.",
                "Zero CVE attack surface with Distroless."
            )
        )
    )
}

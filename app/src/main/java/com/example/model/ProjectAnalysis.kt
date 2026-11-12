package com.example.model

data class DetectedFile(
    val path: String,
    val type: String,
    val description: String,
    val isKeyFile: Boolean = false
)

data class FrameworkDetail(
    val name: String,
    val version: String,
    val category: String,
    val iconName: String
)

data class ProjectProfile(
    val id: String,
    val name: String,
    val language: String,
    val framework: String,
    val buildTool: String,
    val entryPoint: String,
    val defaultPort: Int,
    val recommendedBaseImage: String,
    val summary: String,
    val files: List<DetectedFile>,
    val frameworks: List<FrameworkDetail>,
    val buildSteps: List<String>,
    val runtimeNotes: List<String>
)

data class CloudRunConfig(
    val serviceName: String = "agentforge-service",
    val region: String = "us-central1",
    val memory: String = "1Gi",
    val cpu: String = "1",
    val concurrency: Int = 80,
    val minInstances: Int = 0,
    val maxInstances: Int = 10,
    val port: Int = 8080,
    val allowUnauthenticated: Boolean = true,
    val executionEnvironment: String = "gen2",
    val useMultiStage: Boolean = true,
    val useNonRootUser: Boolean = true,
    val includeHealthCheck: Boolean = true,
    val envVars: Map<String, String> = mapOf(
        "ENV" to "production",
        "LOG_LEVEL" to "info"
    )
)

enum class LintSeverity {
    PASS,
    WARN,
    INFO
}

data class LinterRule(
    val id: String,
    val title: String,
    val description: String,
    val severity: LintSeverity,
    val cloudRunSpec: String,
    val recommendation: String
)

data class FileArtifact(
    val filename: String,
    val path: String,
    val language: String,
    val description: String,
    val content: String
)

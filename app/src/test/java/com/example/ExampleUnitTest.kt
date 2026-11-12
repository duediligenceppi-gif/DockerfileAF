package com.example

import com.example.data.ProjectRepository
import com.example.generator.CloudRunLinter
import com.example.generator.DockerfileGenerator
import com.example.model.CloudRunConfig
import com.example.model.LintSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun projectRepository_hasCurrentWorkspaceProfile() {
        val current = ProjectRepository.CurrentWorkspaceProfile
        assertEquals("current_workspace", current.id)
        assertTrue(current.language.contains("Kotlin"))
        assertTrue(current.framework.contains("Compose"))
        assertEquals(8080, current.defaultPort)
    }

    @Test
    fun dockerfileGenerator_generatesValidCloudRunDockerfile() {
        val config = CloudRunConfig(
            serviceName = "test-service",
            region = "us-central1",
            port = 8080,
            useNonRootUser = true,
            includeHealthCheck = true
        )
        val dockerfile = DockerfileGenerator.generateDockerfile(ProjectRepository.CurrentWorkspaceProfile, config)
        assertNotNull(dockerfile)
        assertTrue(dockerfile.contains("FROM eclipse-temurin:17-jdk-jammy AS builder"))
        assertTrue(dockerfile.contains("FROM python:3.11-slim AS runner"))
        assertTrue(dockerfile.contains("USER appuser"))
        assertTrue(dockerfile.contains("ENV PORT=8080"))
        assertTrue(dockerfile.contains("EXPOSE 8080"))
        assertTrue(dockerfile.contains("/healthz"))
    }

    @Test
    fun cloudRunLinter_evaluatesReadinessScore() {
        val config = CloudRunConfig(
            useNonRootUser = true,
            useMultiStage = true,
            includeHealthCheck = true,
            concurrency = 80,
            memory = "1Gi"
        )
        val report = CloudRunLinter.evaluate(config)
        assertTrue(report.score >= 90)
        assertTrue(report.rules.any { it.id == "RULE_PORT_CONTRACT" && it.severity == LintSeverity.PASS })
        assertTrue(report.rules.any { it.id == "RULE_NON_ROOT" && it.severity == LintSeverity.PASS })
    }

    @Test
    fun deployCommand_includesExpectedFlags() {
        val config = CloudRunConfig(
            serviceName = "agentforge",
            region = "us-west1",
            memory = "2Gi",
            cpu = "2",
            concurrency = 100,
            allowUnauthenticated = true
        )
        val cmd = DockerfileGenerator.generateCloudDeployCommand(config)
        assertTrue(cmd.contains("gcloud run deploy agentforge"))
        assertTrue(cmd.contains("--region us-west1"))
        assertTrue(cmd.contains("--memory 2Gi"))
        assertTrue(cmd.contains("--cpu 2"))
        assertTrue(cmd.contains("--concurrency 100"))
        assertTrue(cmd.contains("--allow-unauthenticated"))
    }
}

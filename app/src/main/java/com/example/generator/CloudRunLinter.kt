package com.example.generator

import com.example.model.CloudRunConfig
import com.example.model.LintSeverity
import com.example.model.LinterRule

object CloudRunLinter {

    data class LintReport(
        val score: Int,
        val passedCount: Int,
        val warnCount: Int,
        val infoCount: Int,
        val rules: List<LinterRule>
    )

    fun evaluate(config: CloudRunConfig): LintReport {
        val rules = mutableListOf<LinterRule>()

        // 1. Port Contract Check
        if (config.port in 1..65535) {
            rules.add(
                LinterRule(
                    id = "RULE_PORT_CONTRACT",
                    title = "Cloud Run \$PORT Contract",
                    description = "Container is configured to bind dynamically to the PORT environment variable (target ${config.port}).",
                    severity = LintSeverity.PASS,
                    cloudRunSpec = "Cloud Run injects the PORT env var. The container must listen on 0.0.0.0 on this port.",
                    recommendation = "Always read os.environ.get('PORT', 8080) or process.env.PORT."
                )
            )
        }

        // 2. Non-root user check
        if (config.useNonRootUser) {
            rules.add(
                LinterRule(
                    id = "RULE_NON_ROOT",
                    title = "Non-Root Security Principle",
                    description = "Container drops root privileges and executes as unprivileged appuser.",
                    severity = LintSeverity.PASS,
                    cloudRunSpec = "Running containers as non-root mitigates container breakout and privilege escalation vulnerabilities.",
                    recommendation = "Verified: USER appuser or USER nonroot is configured."
                )
            )
        } else {
            rules.add(
                LinterRule(
                    id = "RULE_NON_ROOT",
                    title = "Root Execution Detected",
                    description = "Container runs with root privileges which violates container hardening standards.",
                    severity = LintSeverity.WARN,
                    cloudRunSpec = "Cloud Run security best practices recommend creating a non-root system user.",
                    recommendation = "Enable 'Non-root user' in AgentForge settings to inject useradd and USER directives."
                )
            )
        }

        // 3. Multi-stage build optimization
        if (config.useMultiStage) {
            rules.add(
                LinterRule(
                    id = "RULE_MULTI_STAGE",
                    title = "Multi-Stage Image Slimming",
                    description = "Build tools and compilers are excluded from the runtime container, maximizing cold-start speed.",
                    severity = LintSeverity.PASS,
                    cloudRunSpec = "Smaller container images pull faster across Google Borg worker nodes, minimizing cold start latency.",
                    recommendation = "Verified: Separate builder and runner stages are active."
                )
            )
        } else {
            rules.add(
                LinterRule(
                    id = "RULE_MULTI_STAGE",
                    title = "Single Stage Image (Large Footprint)",
                    description = "Compilers and SDKs may inflate image size, increasing cold start time on scale-from-zero.",
                    severity = LintSeverity.WARN,
                    cloudRunSpec = "Container images > 1GB suffer higher cold start overhead.",
                    recommendation = "Turn on multi-stage builds in AgentForge."
                )
            )
        }

        // 4. Health Check Endpoint
        if (config.includeHealthCheck) {
            rules.add(
                LinterRule(
                    id = "RULE_HEALTHCHECK",
                    title = "Healthcheck Probe Response",
                    description = "HTTP endpoints (/healthz, /) handle container liveness and startup probes.",
                    severity = LintSeverity.PASS,
                    cloudRunSpec = "Cloud Run performs startup and liveness HTTP probes to determine traffic readiness.",
                    recommendation = "Verified: 200 OK returned on /healthz and /."
                )
            )
        } else {
            rules.add(
                LinterRule(
                    id = "RULE_HEALTHCHECK",
                    title = "No Dedicated Healthcheck",
                    description = "Traffic routing relies solely on root endpoint response.",
                    severity = LintSeverity.INFO,
                    cloudRunSpec = "Cloud Run sends requests immediately when container opens TCP port.",
                    recommendation = "Include an explicit /healthz probe handler."
                )
            )
        }

        // 5. Memory & Concurrency Optimization
        val memValueMb = parseMemoryMb(config.memory)
        if (memValueMb >= 512 && config.concurrency in 10..150) {
            rules.add(
                LinterRule(
                    id = "RULE_CONCURRENCY",
                    title = "Concurrency & Memory Sizing",
                    description = "Allocated ${config.memory} with concurrency of ${config.concurrency} req/container is well-balanced.",
                    severity = LintSeverity.PASS,
                    cloudRunSpec = "Default concurrency is 80 requests per container instance. Memory must sustain concurrent worker threads.",
                    recommendation = "Well proportioned for general serverless web requests."
                )
            )
        } else if (memValueMb < 512 && config.concurrency > 80) {
            rules.add(
                LinterRule(
                    id = "RULE_CONCURRENCY",
                    title = "High Concurrency on Low Memory",
                    description = "Memory (${config.memory}) may experience Out-Of-Memory (OOM) under concurrent burst traffic (${config.concurrency}).",
                    severity = LintSeverity.WARN,
                    cloudRunSpec = "Cloud Run kills containers exceeding memory allocation.",
                    recommendation = "Increase memory to at least 1Gi or decrease concurrency to 20-40."
                )
            )
        } else {
            rules.add(
                LinterRule(
                    id = "RULE_CONCURRENCY",
                    title = "Concurrency Tuning",
                    description = "Concurrency set to ${config.concurrency} with ${config.memory} RAM.",
                    severity = LintSeverity.INFO,
                    cloudRunSpec = "For heavy CPU workloads, set concurrency between 1 and 10.",
                    recommendation = "Monitor Cloud Monitoring latencies after initial deployment."
                )
            )
        }

        // 6. Scale to zero & Cost
        if (config.minInstances == 0) {
            rules.add(
                LinterRule(
                    id = "RULE_SCALE_ZERO",
                    title = "True Serverless Zero-Cost Idle",
                    description = "Min instances is 0: you only pay when requests are actively being processed.",
                    severity = LintSeverity.PASS,
                    cloudRunSpec = "Cloud Run charges \$0.00 when idle with min-instances=0.",
                    recommendation = "Ideal for developer test environments and event-driven workloads."
                )
            )
        } else {
            rules.add(
                LinterRule(
                    id = "RULE_SCALE_ZERO",
                    title = "Warm Instances Configured",
                    description = "Min instances is ${config.minInstances}: guarantees zero cold start but incurs idle standby cost.",
                    severity = LintSeverity.INFO,
                    cloudRunSpec = "Keeps warm instances ready for immediate low-latency response.",
                    recommendation = "Use min-instances >= 1 for critical production traffic with SLAs."
                )
            )
        }

        // Calculate score
        val passCount = rules.count { it.severity == LintSeverity.PASS }
        val warnCount = rules.count { it.severity == LintSeverity.WARN }
        val infoCount = rules.count { it.severity == LintSeverity.INFO }

        val calculatedScore = 100 - (warnCount * 15) - (infoCount * 3)
        val finalScore = calculatedScore.coerceIn(40, 100)

        return LintReport(
            score = finalScore,
            passedCount = passCount,
            warnCount = warnCount,
            infoCount = infoCount,
            rules = rules
        )
    }

    private fun parseMemoryMb(memory: String): Int {
        return when {
            memory.endsWith("Gi", ignoreCase = true) -> {
                val num = memory.replace("Gi", "", ignoreCase = true).trim().toIntOrNull() ?: 1
                num * 1024
            }
            memory.endsWith("Mi", ignoreCase = true) -> {
                memory.replace("Mi", "", ignoreCase = true).trim().toIntOrNull() ?: 512
            }
            else -> 1024
        }
    }
}

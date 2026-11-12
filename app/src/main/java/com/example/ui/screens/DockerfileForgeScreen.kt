package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CloudRunConfig
import com.example.model.ProjectProfile
import com.example.ui.NavigationTab
import com.example.ui.components.CodeBlockView
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatBadge

@Composable
fun DockerfileForgeScreen(
    profile: ProjectProfile,
    config: CloudRunConfig,
    dockerfileContent: String,
    onUpdateConfig: ((CloudRunConfig) -> CloudRunConfig) -> Unit,
    onNavigateTab: (NavigationTab) -> Unit,
    onShowToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("dockerfile_forge_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                title = "Dockerfile Generator for Google Cloud Run",
                subtitle = "Optimized for fast cold start, stateless execution, and Google Borg runtime contract"
            )
        }

        // Action Toolbar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Dockerfile", dockerfileContent)
                        clipboard.setPrimaryClip(clip)
                        onShowToast("Dockerfile copied to clipboard!")
                    },
                    modifier = Modifier.weight(1f).testTag("copy_dockerfile_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Dockerfile")
                }

                OutlinedButton(
                    onClick = { onNavigateTab(NavigationTab.CLOUDRUN) },
                    modifier = Modifier.weight(1f).testTag("navigate_deploy_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Configure Deploy")
                }
            }
        }

        // Optimization Toggles Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("docker_options_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cloud Run Container Optimizations",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Multi-stage toggle
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Multi-stage build", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text("Separates build SDKs from runtime image for faster cold starts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = config.useMultiStage,
                            onCheckedChange = { checked ->
                                onUpdateConfig { it.copy(useMultiStage = checked) }
                            },
                            modifier = Modifier.testTag("switch_multi_stage")
                        )
                    }

                    // Non-root toggle
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Non-root user execution", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text("Runs as unprivileged appuser for container security", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = config.useNonRootUser,
                            onCheckedChange = { checked ->
                                onUpdateConfig { it.copy(useNonRootUser = checked) }
                            },
                            modifier = Modifier.testTag("switch_non_root")
                        )
                    }

                    // Health check toggle
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Health check endpoint (/healthz)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text("Responds to Cloud Run startup and liveness probes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = config.includeHealthCheck,
                            onCheckedChange = { checked ->
                                onUpdateConfig { it.copy(includeHealthCheck = checked) }
                            },
                            modifier = Modifier.testTag("switch_health_check")
                        )
                    }
                }
            }
        }

        // Live Generated Dockerfile Code Viewer
        item {
            CodeBlockView(
                code = dockerfileContent,
                title = "Dockerfile (${profile.name})",
                language = "dockerfile",
                maxHeight = 420,
                onCopied = { onShowToast("Dockerfile copied!") }
            )
        }

        // Explanatory Cloud Run specs
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Cloud Run Runtime Guarantees",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Container must listen on 0.0.0.0 and dynamically bind to \${PORT} (default 8080).\n" +
                                "• SIGTERM signal is received 10 seconds before instance termination.\n" +
                                "• Filesystem is ephemeral: state stored on disk is wiped on container scale-down.\n" +
                                "• Requests are routed automatically via Google's Global Load Balancer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

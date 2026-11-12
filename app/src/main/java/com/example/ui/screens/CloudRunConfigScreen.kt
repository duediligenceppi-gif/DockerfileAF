package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CloudRunConfig
import com.example.ui.components.CodeBlockView
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatBadge

@Composable
fun CloudRunConfigScreen(
    config: CloudRunConfig,
    deployCommand: String,
    cloudBuildYaml: String,
    onUpdateConfig: ((CloudRunConfig) -> CloudRunConfig) -> Unit,
    onShowToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val regions = listOf("us-central1", "us-west1", "us-east1", "europe-west1", "asia-east1")
    val memoryOptions = listOf("256Mi", "512Mi", "1Gi", "2Gi", "4Gi")
    val cpuOptions = listOf("1", "2", "4")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("cloud_run_config_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                title = "Google Cloud Run Configuration",
                subtitle = "Fine-tune container compute, auto-scaling, ingress, and gcloud deployment flags"
            )
        }

        // Service Name & Region
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Service Identity & Region",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = config.serviceName,
                        onValueChange = { newName ->
                            onUpdateConfig { it.copy(serviceName = newName.trim().lowercase()) }
                        },
                        label = { Text("Service Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_service_name")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Deployment Region",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        regions.forEach { r ->
                            val isSelected = r == config.region
                            FilterChip(
                                selected = isSelected,
                                onClick = { onUpdateConfig { it.copy(region = r) } },
                                label = { Text(r) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }
        }

        // Compute Resources (Memory & CPU)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Compute Sizing",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Memory Allocation",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        memoryOptions.forEach { mem ->
                            val isSelected = mem == config.memory
                            FilterChip(
                                selected = isSelected,
                                onClick = { onUpdateConfig { it.copy(memory = mem) } },
                                label = { Text(mem) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "vCPU Allocation",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        cpuOptions.forEach { c ->
                            val isSelected = c == config.cpu
                            FilterChip(
                                selected = isSelected,
                                onClick = { onUpdateConfig { it.copy(cpu = c) } },
                                label = { Text("$c vCPU") }
                            )
                        }
                    }
                }
            }
        }

        // Concurrency & Autoscaling
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Concurrency per Container",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${config.concurrency} req",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Slider(
                        value = config.concurrency.toFloat(),
                        onValueChange = { v ->
                            onUpdateConfig { it.copy(concurrency = v.toInt()) }
                        },
                        valueRange = 1f..250f,
                        steps = 24,
                        modifier = Modifier.testTag("slider_concurrency")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Scale to Zero (Min Instances: 0)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text("Pay \$0 when no requests are being processed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = config.minInstances == 0,
                            onCheckedChange = { scaleZero ->
                                onUpdateConfig { it.copy(minInstances = if (scaleZero) 0 else 1) }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Allow Unauthenticated (Public API)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text("Enables public HTTPS ingress from the web", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = config.allowUnauthenticated,
                            onCheckedChange = { allow ->
                                onUpdateConfig { it.copy(allowUnauthenticated = allow) }
                            }
                        )
                    }
                }
            }
        }

        // Live gcloud CLI Command
        item {
            SectionHeader(
                title = "Generated gcloud Deploy Command",
                subtitle = "Execute in terminal from your workspace directory to deploy directly to Google Cloud Run"
            )
        }

        item {
            CodeBlockView(
                code = deployCommand,
                title = "gcloud run deploy",
                language = "bash",
                maxHeight = 180,
                onCopied = { onShowToast("Deployment command copied!") }
            )
        }

        item {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("gcloud command", deployCommand)
                    clipboard.setPrimaryClip(clip)
                    onShowToast("gcloud run deploy command copied!")
                },
                modifier = Modifier.fillMaxWidth().testTag("copy_deploy_command_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy Full gcloud Command", fontWeight = FontWeight.Bold)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.data.ProjectRepository
import com.example.generator.CloudRunLinter
import com.example.generator.DockerfileGenerator
import com.example.model.CloudRunConfig
import com.example.model.FileArtifact
import com.example.model.ProjectProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class NavigationTab(val title: String, val icon: String) {
    SCANNER("Workspace", "scanner"),
    DOCKERFILE("Dockerfile", "docker"),
    CLOUDRUN("Cloud Run", "cloud"),
    LINTER("Linter", "rule"),
    FILES("Files", "folder")
}

data class AgentForgeUiState(
    val currentTab: NavigationTab = NavigationTab.SCANNER,
    val selectedProfile: ProjectProfile = ProjectRepository.CurrentWorkspaceProfile,
    val availableProfiles: List<ProjectProfile> = ProjectRepository.SupportedProfiles,
    val config: CloudRunConfig = CloudRunConfig(),
    val generatedDockerfile: String = "",
    val generatedDockerignore: String = "",
    val generatedDeployCommand: String = "",
    val generatedCloudBuildYaml: String = "",
    val generatedServiceYaml: String = "",
    val lintReport: CloudRunLinter.LintReport = CloudRunLinter.evaluate(CloudRunConfig()),
    val activeArtifacts: List<FileArtifact> = emptyList(),
    val selectedFileArtifactIndex: Int = 0,
    val toastMessage: String? = null
)

class AgentForgeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AgentForgeUiState())
    val uiState: StateFlow<AgentForgeUiState> = _uiState.asStateFlow()

    init {
        refreshGenerations()
    }

    fun selectTab(tab: NavigationTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun selectProfile(profile: ProjectProfile) {
        _uiState.update {
            it.copy(
                selectedProfile = profile,
                config = it.config.copy(serviceName = "${profile.id.replace("_", "-")}-service")
            )
        }
        refreshGenerations()
    }

    fun updateConfig(updater: (CloudRunConfig) -> CloudRunConfig) {
        _uiState.update {
            it.copy(config = updater(it.config))
        }
        refreshGenerations()
    }

    fun selectFileArtifact(index: Int) {
        _uiState.update { it.copy(selectedFileArtifactIndex = index) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(toastMessage = message) }
    }

    private fun refreshGenerations() {
        val current = _uiState.value
        val dockerfile = DockerfileGenerator.generateDockerfile(current.selectedProfile, current.config)
        val dockerignore = DockerfileGenerator.generateDockerignore()
        val deployCmd = DockerfileGenerator.generateCloudDeployCommand(current.config)
        val cloudBuild = DockerfileGenerator.generateCloudBuildYaml(current.config)
        val serviceYaml = DockerfileGenerator.generateServiceYaml(current.config)
        val report = CloudRunLinter.evaluate(current.config)

        val artifacts = listOf(
            FileArtifact("Dockerfile", "/Dockerfile", "dockerfile", "Google Cloud Run multi-stage container configuration", dockerfile),
            FileArtifact(".dockerignore", "/.dockerignore", "ignore", "Container build exclusions to keep image small", dockerignore),
            FileArtifact("cloudbuild.yaml", "/cloudbuild.yaml", "yaml", "Google Cloud Build automated CI/CD pipeline", cloudBuild),
            FileArtifact("service.yaml", "/service.yaml", "yaml", "Knative service manifest for Cloud Run deployment", serviceYaml),
            FileArtifact("gcloud-deploy.sh", "/scripts/deploy.sh", "bash", "One-line gcloud CLI deployment script", deployCmd)
        )

        _uiState.update {
            it.copy(
                generatedDockerfile = dockerfile,
                generatedDockerignore = dockerignore,
                generatedDeployCommand = deployCmd,
                generatedCloudBuildYaml = cloudBuild,
                generatedServiceYaml = serviceYaml,
                lintReport = report,
                activeArtifacts = artifacts
            )
        }
    }
}

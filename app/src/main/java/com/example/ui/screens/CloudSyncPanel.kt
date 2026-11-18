package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CloudAuthManager
import com.example.model.CircuitProject
import com.example.model.CustomIcDefinition
import com.example.ui.theme.FaultRed
import com.example.ui.theme.LogicHighGreen
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SignalCyan
import com.example.viewmodel.WorkbenchViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.catch

@Composable
fun CloudSyncPanel(
    vm: WorkbenchViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val activeProject by vm.activeProject.collectAsStateWithLifecycle()
    val localCustomIcs by vm.customIcs.collectAsStateWithLifecycle()
    val cloudBusy by vm.cloudSyncBusy.collectAsStateWithLifecycle()

    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }
    var authBusy by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SignalCyan.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (currentUser != null) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = if (currentUser != null) LogicHighGreen else SignalCyan
                    )
                    Column {
                        Text(
                            text = "GateX Cloud Backup & File Share",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (currentUser != null) {
                                "Signed in as ${currentUser?.email ?: currentUser?.displayName ?: "Google User"}"
                            } else {
                                "Sync circuits & Custom ICs privately via Google Sign-In"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (currentUser != null) {
                    IconButton(
                        onClick = {
                            CloudAuthManager.signOut(credentialManager, scope)
                        },
                        modifier = Modifier.testTag("cloud_sign_out_btn")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Sign out", tint = FaultRed)
                    }
                }
            }

            // Offline .gatex.json file share is always available even without signing in
            OutlinedButton(
                onClick = { vm.shareGatexProjectFile() },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("cloud_share_gatex_file_btn")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = SignalAmber)
                Spacer(Modifier.width(6.dp))
                Text("Share Active Circuit (.gatex.json File)")
            }

            if (authError != null) {
                Surface(
                    color = FaultRed.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = authError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = FaultRed,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            val signedInUser = currentUser
            if (signedInUser == null) {
                Button(
                    onClick = {
                        authBusy = true
                        authError = null
                        CloudAuthManager.onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                authBusy = false
                                authError = null
                            },
                            onAuthError = { msg ->
                                authBusy = false
                                authError = msg
                            },
                            onAuthCancelled = {
                                authBusy = false
                            },
                            scope = scope
                        )
                    },
                    enabled = !authBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("cloud_google_signin_btn")
                ) {
                    if (authBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Signing in...")
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sign in with Google for Cloud Sync")
                    }
                }
            } else {
                // Strictly key Firestore observers to authenticated user's UID
                key(signedInUser.uid) {
                    AuthenticatedCloudContent(
                        vm = vm,
                        activeProjectName = activeProject.name,
                        localCustomIcs = localCustomIcs,
                        cloudBusy = cloudBusy
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedCloudContent(
    vm: WorkbenchViewModel,
    activeProjectName: String,
    localCustomIcs: List<CustomIcDefinition>,
    cloudBusy: Boolean
) {
    var cloudProjects by remember { mutableStateOf<List<CircuitProject>>(emptyList()) }
    var cloudIcs by remember { mutableStateOf<List<CustomIcDefinition>>(emptyList()) }
    var syncError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        vm.cloudSyncRepo.observeCloudProjects()
            .catch { err -> syncError = err.localizedMessage ?: "Failed to sync cloud projects." }
            .collect { list ->
                syncError = null
                cloudProjects = list
            }
    }

    LaunchedEffect(Unit) {
        vm.cloudSyncRepo.observeCloudCustomIcs()
            .catch { err -> syncError = err.localizedMessage ?: "Failed to sync custom ICs." }
            .collect { list ->
                cloudIcs = list
            }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { vm.uploadActiveProjectToCloud() },
                enabled = !cloudBusy,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("cloud_backup_project_btn")
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Backup '$activeProjectName'", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        if (localCustomIcs.isNotEmpty() || cloudIcs.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (localCustomIcs.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            localCustomIcs.forEach { vm.uploadCustomIcToCloud(it) }
                        },
                        enabled = !cloudBusy,
                        modifier = Modifier.weight(1f).testTag("cloud_upload_ics_btn")
                    ) {
                        Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Push ${localCustomIcs.size} IC(s)")
                    }
                }
                if (cloudIcs.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { vm.syncCloudCustomIcsToLocal(cloudIcs) },
                        enabled = !cloudBusy,
                        modifier = Modifier.weight(1f).testTag("cloud_pull_ics_btn")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Pull ${cloudIcs.size} IC(s)")
                    }
                }
            }
        }

        if (syncError != null) {
            Text(syncError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = FaultRed)
        }

        HorizontalDivider()
        Text(
            text = "Cloud Backed-Up Circuits (${cloudProjects.size})",
            style = MaterialTheme.typography.titleSmall,
            color = SignalCyan
        )

        if (cloudProjects.isEmpty()) {
            Text(
                text = "No cloud backups yet. Tap Backup above to save your active circuit.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            cloudProjects.forEach { proj ->
                val page = proj.activePage()
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(proj.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${page.components.size} parts • ${page.wires.size} wires",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = { vm.restoreCloudProject(proj) },
                                modifier = Modifier.testTag("cloud_restore_${proj.id}")
                            ) {
                                Text("Restore")
                            }
                            IconButton(
                                onClick = { vm.deleteCloudProject(proj.id) },
                                modifier = Modifier.testTag("cloud_delete_${proj.id}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete cloud backup", tint = FaultRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

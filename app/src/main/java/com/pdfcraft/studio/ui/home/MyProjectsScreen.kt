package com.pdfcraft.studio.ui.home

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pdfcraft.studio.R
import com.pdfcraft.studio.core.project.ProjectStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProjectsScreen(
    onBackClick: () -> Unit,
    onOpenProject: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var projects by remember { mutableStateOf(emptyList<ProjectStore.ProjectListItem>()) }
    var deleteTarget by remember { mutableStateOf<ProjectStore.ProjectListItem?>(null) }
    var renameTarget by remember { mutableStateOf<ProjectStore.ProjectListItem?>(null) }
    var renameText by remember { mutableStateOf("") }
    var menuFor by remember { mutableStateOf<ProjectStore.ProjectListItem?>(null) }

    fun reload() {
        scope.launch {
            projects = withContext(Dispatchers.IO) { ProjectStore.listProjects(context) }
        }
    }
    LaunchedEffect(Unit) { reload() }

    Scaffold(
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.my_projects),
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_desc_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { inner ->
        if (projects.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.no_saved_projects),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.no_saved_projects_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8A8A8A),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(projects, key = { it.file.absolutePath }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (!item.readable) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                context.getString(R.string.project_unreadable)
                                            )
                                        }
                                    } else {
                                        onOpenProject(item.file.absolutePath)
                                    }
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                item.name,
                                fontWeight = FontWeight.SemiBold,
                                color = if (item.readable) Color.Black else Color(0xFFB00020),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val pagesLabel = if (item.pageCount == 1)
                                context.getString(R.string.project_page_count, item.pageCount)
                            else
                                context.getString(R.string.project_pages_count, item.pageCount)
                            Text(
                                pagesLabel +
                                    "  •  " +
                                    DateUtils.getRelativeTimeSpanString(
                                        item.lastModifiedMillis,
                                        System.currentTimeMillis(),
                                        DateUtils.MINUTE_IN_MILLIS
                                    ).toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8A8A8A)
                            )
                        }
                        Box {
                            IconButton(onClick = { menuFor = item }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = null,
                                    tint = Color(0xFF444444)
                                )
                            }
                            DropdownMenu(
                                expanded = menuFor?.file?.absolutePath == item.file.absolutePath,
                                onDismissRequest = { menuFor = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.project_open)) },
                                    onClick = {
                                        menuFor = null
                                        if (!item.readable) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    context.getString(R.string.project_unreadable)
                                                )
                                            }
                                        } else {
                                            onOpenProject(item.file.absolutePath)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.pdf_library_rename)) },
                                    onClick = {
                                        menuFor = null
                                        renameText = item.name
                                        renameTarget = item
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.pdf_library_delete), color = Color(0xFFB00020)) },
                                    onClick = {
                                        menuFor = null
                                        deleteTarget = item
                                    }
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                }
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.delete_project)) },
            text = { Text(stringResource(R.string.delete_project_confirm, target.name)) },
            confirmButton = {
                TextButton(onClick = {
                    ProjectStore.deleteProject(target.file)
                    deleteTarget = null
                    reload()
                }) { Text(stringResource(R.string.delete_project)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(stringResource(R.string.rename_project)) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.rename_project_hint)) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val dest = ProjectStore.renameProject(target.file, renameText.trim())
                    if (dest == null) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.rename_project_failed)
                            )
                        }
                    } else {
                        renameTarget = null
                        reload()
                    }
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

package com.pdfcraft.studio.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pdfcraft.studio.core.session.SessionStore
import com.pdfcraft.studio.core.project.ProjectStore
import com.pdfcraft.studio.ui.editor.EditorScreen
import com.pdfcraft.studio.ui.home.HomeScreen
import com.pdfcraft.studio.ui.home.IconCenterScreen
import com.pdfcraft.studio.ui.home.MyProjectsScreen
import com.pdfcraft.studio.ui.viewer.PdfLibraryScreen
import com.pdfcraft.studio.ui.viewer.PdfViewerScreen

@Composable
fun PdfCraftNavGraph(
    openPdfUri: String? = null,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    // Last editor is the start screen from the very first frame (no Home flash).
    val resumeEditor: String? = androidx.compose.runtime.remember {
        if (!openPdfUri.isNullOrBlank()) {
            null
        } else {
            try {
                val s0 = SessionStore.load(context)
                if (s0.screen != SessionStore.EDITOR) {
                    null
                } else if (s0.editorPath.isBlank() || SessionStore.editorPathOpenable(context, s0.editorPath)) {
                    s0.editorPath
                } else {
                    SessionStore.setHome(context)
                    null
                }
            } catch (_: Exception) {
                null
            }
        }
    }
    val goHomeFromEditor: () -> Unit = {
        SessionStore.setHome(context)
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        } else {
            navController.navigate(Screen.Home.route) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    LaunchedEffect(openPdfUri) {
        val uri = openPdfUri?.takeIf { it.isNotBlank() }
        if (uri != null) {
            SessionStore.setViewer(context, uri)
            navController.navigate(Screen.PdfViewer.routeWithUri(uri)) {
                launchSingleTop = true
            }
            return@LaunchedEffect
        }
        val s = SessionStore.load(context)
        when (s.screen) {
            SessionStore.EDITOR -> {
                if (resumeEditor != null) return@LaunchedEffect
                val path = s.editorPath
                if (SessionStore.editorPathOpenable(context, path)) {
                    navController.navigate(Screen.Editor.routeOpen(path)) {
                        launchSingleTop = true
                    }
                } else {
                    // Project could not be loaded: stay on Home.
                    SessionStore.setHome(context)
                }
            }
            SessionStore.VIEWER -> {
                if (SessionStore.uriReadable(context, s.viewerUri)) {
                    navController.navigate(Screen.PdfViewer.routeWithUri(s.viewerUri)) {
                        launchSingleTop = true
                    }
                } else SessionStore.setHome(context)
            }
            SessionStore.LIBRARY -> {
                navController.navigate(Screen.PdfLibrary.route) { launchSingleTop = true }
            }
            SessionStore.PROJECTS -> {
                navController.navigate(Screen.MyProjects.route) { launchSingleTop = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (resumeEditor != null) "editor_resume" else Screen.Home.route
    ) {
        composable("editor_resume") {
            EditorScreen(
                projectPath = resumeEditor?.takeIf { it.isNotBlank() },
                onBackClick = { goHomeFromEditor() },
                onViewPdfClick = { uriString ->
                    SessionStore.setViewer(context, uriString)
                    navController.navigate(Screen.PdfViewer.routeWithUri(uriString))
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onCreatePdfClick = {
                    SessionStore.clearEditorUi(context)
                    SessionStore.setEditor(context, "")
                    navController.navigate(Screen.Editor.routeNew())
                },
                onMyProjectsClick = {
                    SessionStore.setScreen(context, SessionStore.PROJECTS)
                    navController.navigate(Screen.MyProjects.route)
                },
                onRecoverDraft = { path ->
                    navController.navigate(Screen.Editor.routeOpen(path))
                },
                onViewPdf = {
                    SessionStore.setScreen(context, SessionStore.LIBRARY)
                    navController.navigate(Screen.PdfLibrary.route)
                },
                onIconCenterClick = {
                    navController.navigate(Screen.IconCenter.route)
                }
            )
        }
        composable(Screen.PdfLibrary.route) {
            PdfLibraryScreen(
                onBackClick = { navController.popBackStack() },
                onOpenPdf = { uri ->
                    SessionStore.setViewer(context, uri)
                    navController.navigate(Screen.PdfViewer.routeWithUri(uri))
                },
                onEditPdf = { uri ->
                    SessionStore.clearEditorUi(context)
                    SessionStore.setEditor(context, "pdf|" + uri)
                    navController.navigate(Screen.Editor.routeOpen("pdf|" + uri))
                }
            )
        }
        composable(Screen.IconCenter.route) {
            IconCenterScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.MyProjects.route) {
            MyProjectsScreen(
                onBackClick = { navController.popBackStack() },
                onOpenProject = { path ->
                    SessionStore.clearEditorUi(context)
                    SessionStore.setEditor(context, path)
                    navController.navigate(Screen.Editor.routeOpen(path))
                }
            )
        }
        composable(
            route = Screen.Editor.route,
            arguments = listOf(navArgument("project") {
                type = NavType.StringType
                defaultValue = ""
            })
        ) { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString("project").orEmpty()
            val projectPath = Screen.Editor.decodeProject(encoded)
            EditorScreen(
                projectPath = projectPath,
                onBackClick = goHomeFromEditor,
                onViewPdfClick = { uriString ->
                    SessionStore.setViewer(context, uriString)
                    navController.navigate(Screen.PdfViewer.routeWithUri(uriString))
                }
            )
        }
        composable(
            route = Screen.PdfViewer.route,
            arguments = listOf(navArgument("uri") { type = NavType.StringType })
        ) { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString("uri").orEmpty()
            val uriString = Screen.PdfViewer.decodeUriArg(encoded)
            val snap = SessionStore.load(context)
            val same = snap.viewerUri == uriString
            PdfViewerScreen(
                pdfUriString = uriString,
                initialIndex = if (same) snap.viewerIndex else 0,
                initialOffset = if (same) snap.viewerOffset else 0,
                onScrollChanged = { i, o -> SessionStore.setViewerScroll(context, i, o) },
                onBackClick = {
                    try {
                        if (!navController.popBackStack()) {
                            SessionStore.setHome(context)
                        } else {
                            val entry = navController.currentBackStackEntry
                            val route = entry?.destination?.route.orEmpty()
                            if (route.startsWith("editor")) {
                                val enc = entry?.arguments?.getString("project").orEmpty()
                                SessionStore.setEditor(
                                    context,
                                    if (route == "editor_resume") resumeEditor.orEmpty()
                                    else Screen.Editor.decodeProject(enc).orEmpty()
                                )
                            } else if (route.startsWith("pdf_library")) {
                                SessionStore.setScreen(context, SessionStore.LIBRARY)
                            } else if (route.startsWith("my_projects")) {
                                SessionStore.setScreen(context, SessionStore.PROJECTS)
                            } else {
                                SessionStore.setHome(context)
                            }
                        }
                    } catch (_: Exception) {
                        try { SessionStore.setHome(context) } catch (_: Exception) {}
                    }
                }
            )
        }
    }
}

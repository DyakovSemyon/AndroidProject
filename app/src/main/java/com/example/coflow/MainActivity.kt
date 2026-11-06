package com.example.coflow

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.LocalIndication
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.coflow.repositories.FirebaseAuthRepositoryImpl
import com.example.coflow.ui.auth.AuthRoute
import com.example.coflow.ui.projects.CreateProjectRoute
import com.example.coflow.ui.projects.ProjectsRoute
import com.example.coflow.ui.tasks.ChooseAssigneeRoute
import com.example.coflow.ui.tasks.ChooseDateRoute
import com.example.coflow.ui.tasks.ChooseProjectRoute
import com.example.coflow.ui.tasks.CreateTaskRoute
import com.example.coflow.ui.tasks.CreateTaskViewModel
import com.example.coflow.ui.tasks.EditTaskRoute
import com.example.coflow.ui.tasks.TaskDetailRoute
import com.example.coflow.ui.tasks.TasksRoute
import com.example.coflow.ui.tasks.TasksScreenViewModel
import com.example.coflow.ui.theme.CoFlowIndication
import com.example.coflow.ui.theme.CoFlowTheme

object Screen {
    const val TASKS = "tasks"
    const val CREATE_TASK = "create_task"
    const val CHOOSE_ASSIGNEE = "choose_assignee"
    const val CHOOSE_PROJECT = "choose_project"
    const val CHOOSE_DATE = "choose_date"
    const val TASK_DETAIL = "task_detail/{taskId}"
    const val PROJECTS = "projects"
    const val CREATE_PROJECT = "create_project"
    const val EDIT_TASK = "edit_task/{taskId}"
}

class MainActivity : ComponentActivity() {
    @SuppressLint("ComposableDestinationInComposeScope")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CoFlowTheme {
                CompositionLocalProvider(
                    LocalIndication provides CoFlowIndication,
                ) {
                    val authRepository = remember { FirebaseAuthRepositoryImpl() }

                    // false — безопасный дефолт: если Firebase упал на старте, пользователь
                    // увидит AuthScreen, а не «Системную ошибку» на TasksScreen.
                    var isAuthenticated by remember {
                        mutableStateOf(
                            runCatching { authRepository.getCurrentUserUid() != null }
                                .getOrDefault(false),
                        )
                    }

                    if (isAuthenticated) {
                        val navController = rememberNavController()
                        val createTaskViewModel: CreateTaskViewModel = viewModel()
                        val tasksViewModel: TasksScreenViewModel = viewModel()

                        NavHost(navController = navController, startDestination = Screen.TASKS) {
                            // Мои задачи
                            composable(Screen.TASKS) {
                                TasksRoute(
                                    viewModel = tasksViewModel,
                                    onNavigateToCreateTask = { navController.navigate(Screen.CREATE_TASK) },
                                    onNavigateToTaskDetail = { taskId -> navController.navigate("task_detail/$taskId") },
                                    onNavigateToProjects = { navController.navigateToTab(Screen.PROJECTS) },
                                    onNavigateToProfile = { },
                                    onTaskSwipe = {},
                                )
                            }

                            // Проекты
                            composable(Screen.PROJECTS) {
                                ProjectsRoute(
                                    onNavigateToCreateProject = { navController.navigate(Screen.CREATE_PROJECT) },
                                    onNavigateToTasks = { navController.navigateToTab(Screen.TASKS) },
                                )
                            }

                            // Экран создания проекта
                            composable(Screen.CREATE_PROJECT) {
                                val context = LocalContext.current

                                CreateProjectRoute(
                                    onBackClick = { navController.popBackStack() },
                                    onProjectCreated = {
                                        navController.popBackStack()
                                    },
                                )
                            }

                            // Просмотр задачи
                            composable(
                                route = Screen.TASK_DETAIL,
                                arguments =
                                    listOf(
                                        navArgument("taskId") {
                                            type = NavType.StringType
                                        },
                                    ),
                            ) {
                                TaskDetailRoute(
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToEdit = { taskId -> navController.navigate("edit_task/$taskId") },
                                )
                            }

                            // Создание задачи
                            composable(Screen.CREATE_TASK) {
                                CreateTaskRoute(
                                    onBackClick = { navController.popBackStack() },
                                    onTaskCreated = {
                                        tasksViewModel.refresh()
                                        navController.popBackStack()
                                    },
                                    onNavigateToAssignees = { navController.navigate(Screen.CHOOSE_ASSIGNEE) },
                                    onNavigateToProject = { navController.navigate(Screen.CHOOSE_PROJECT) },
                                    onNavigateToDate = { navController.navigate(Screen.CHOOSE_DATE) },
                                    viewModel = createTaskViewModel,
                                )
                            }

                            // Редактирование задачи
                            composable(
                                route = Screen.EDIT_TASK,
                                arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
                            ) {
                                EditTaskRoute(
                                    onNavigateBack = { navController.popBackStack() },
                                    onTaskSaved = {
                                        tasksViewModel.refresh()
                                        navController.popBackStack()
                                    },
                                    onTaskDeleted = {
                                        tasksViewModel.refresh()
                                        navController.popBackStack()
                                    },
                                    onNavigateToAssignees = { },
                                    onNavigateToProject = { },
                                    onNavigateToDate = { },
                                )
                            }

                            // Выбор исполнителей
                            composable(Screen.CHOOSE_ASSIGNEE) {
                                ChooseAssigneeRoute(
                                    onBackClick = { navController.popBackStack() },
                                    onConfirmSelection = { assignees ->
                                        createTaskViewModel.confirmAssigneeSelection(assignees)
                                        navController.popBackStack()
                                    },
                                    viewModel = createTaskViewModel,
                                )
                            }

                            // Выбор проекта
                            composable(Screen.CHOOSE_PROJECT) {
                                ChooseProjectRoute(
                                    onBackClick = { navController.popBackStack() },
                                    onConfirmSelection = { project ->
                                        createTaskViewModel.confirmProjectSelection(project)
                                        navController.popBackStack()
                                    },
                                    viewModel = createTaskViewModel,
                                )
                            }

                            // Выбор даты
                            composable(Screen.CHOOSE_DATE) {
                                ChooseDateRoute(
                                    onBackClick = { navController.popBackStack() },
                                    onConfirmSelection = { date ->
                                        createTaskViewModel.confirmDateSelection(date)
                                        navController.popBackStack()
                                    },
                                    viewModel = createTaskViewModel,
                                )
                            }
                        }
                    } else {
                        AuthRoute(onAuthSuccess = { isAuthenticated = true })
                    }
                }
            }
        }
    }

    private fun NavHostController.navigateToTab(route: String) {
        navigate(route) {
            popUpTo(graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}
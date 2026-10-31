package com.example.coflow.ui.tasks

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coflow.R
import com.example.coflow.ui.components.*
import com.example.coflow.ui.theme.*

@Composable
fun ChooseAssigneeScreen(
    state: CreateTaskUiState,
    onBackClick: () -> Unit,
    onAssigneeSearchQueryChange: (String) -> Unit,
    onToggleAssignee: (String) -> Unit,
    onConfirmSelection: (List<Assignee>) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    // Возврат по системной кнопке "Назад"
    BackHandler(onBack = { onBackClick() })

    Box(modifier = Modifier.fillMaxSize().background(coFlowTasksBackgroundBrush())) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = CoFlowSpacing.ScreenHorizontal),
        ) {
            // Заголовок экрана
            CoFlowScreenHeader(
                title = stringResource(R.string.choose_assignee_header),
                onBackClick = {
                    focusManager.clearFocus()
                    onBackClick()
                },
            )

            // Поиск по исполнителям
            CoFlowGlassTextField(
                value = state.assigneeSearchQuery,
                onValueChange = onAssigneeSearchQueryChange,
                placeholder = stringResource(R.string.create_task_choose_assignee_placeholder),
                leadingIcon = "search",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            )

            Spacer(Modifier.height(20.dp))

            // Заголовок списка и общее количество исполнителей
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.create_task_assignee),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CoFlowColors.TextPrimary,
                )
                Text(
                    text = "${state.allAssignees.size}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CoFlowColors.TextSecondary,
                )
            }

            // Список результатов поиска
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(
                    items = state.filteredAssignees,
                    key = { it.id },
                ) { assignee ->
                    val isSelected = assignee.id in state.pendingAssigneeIds

                    CoFlowGlassCard(
                        isSelected = isSelected,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onToggleAssignee(assignee.id) },
                    ) {
                        AssigneeItem(
                            assignee = assignee,
                            isSelected = isSelected,
                        )
                    }
                }
            }

            // Нижняя панель: счётчик выбранных + кнопка подтверждения
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp),
            ) {
                Text(
                    text = getSelectedText(state.pendingSelectedCount),
                    modifier = Modifier.padding(bottom = 12.dp, start = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = CoFlowColors.TextPrimary,
                )
                CoFlowPrimaryButton(
                    text = stringResource(R.string.add_assignee_button),
                    onClick = {
                        focusManager.clearFocus()
                        val selectedAssignees =
                            state.pendingAssigneeIds.mapNotNull { id ->
                                state.filteredAssignees.firstOrNull { it.id == id }
                            }
                        onConfirmSelection(selectedAssignees)
                    },
                    enabled = state.canConfirmAssignees,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun ChooseAssigneeRoute(
    onBackClick: () -> Unit,
    onConfirmSelection: (List<Assignee>) -> Unit,
    viewModel: CreateTaskViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ChooseAssigneeScreen(
        state = state,
        onBackClick = onBackClick,
        onAssigneeSearchQueryChange = viewModel::onAssigneeSearchQueryChange,
        onToggleAssignee = viewModel::togglePendingAssignee,
        onConfirmSelection = { assignees ->
            viewModel.confirmAssigneeSelection(assignees)
            onConfirmSelection(assignees)
        },
    )
}

@Composable
private fun getSelectedText(count: Int): String =
    if (count == 0) {
        stringResource(R.string.no_assignees_selected)
    } else {
        pluralStringResource(R.plurals.selected_assignees_count, count, count)
    }

@Composable
private fun AssigneeItem(
    assignee: Assignee,
    isSelected: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .background(
                        color = assignee.avatarColor.copy(alpha = 0.2f),
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = assignee.initials,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = assignee.avatarColor,
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = assignee.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = CoFlowColors.TextPrimary,
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = assignee.email,
                fontSize = 12.sp,
                color = CoFlowColors.TextSecondary,
            )
        }

        Box(
            modifier =
                Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isSelected) CoFlowColors.Blue else Color.White,
                    ).border(
                        width = 1.5.dp,
                        color =
                            if (isSelected) {
                                CoFlowColors.Blue
                            } else {
                                CoFlowColors.TextSecondary.copy(alpha = 0.5f)
                            },
                        shape = RoundedCornerShape(6.dp),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                CoFlowIcon(
                    name = "check",
                    modifier = Modifier.size(14.dp),
                    color = Color.White,
                )
            }
        }
    }
}

@Preview(
    name = "Выбор исполнителя",
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
)
@Composable
private fun ChooseAssigneeScreenPreview() {
    CoFlowTheme {
        ChooseAssigneeScreen(
            state = CreateTaskUiState(),
            onBackClick = {},
            onAssigneeSearchQueryChange = {},
            onToggleAssignee = {},
            onConfirmSelection = {},
        )
    }
}

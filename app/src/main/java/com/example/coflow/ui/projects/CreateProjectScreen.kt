package com.example.coflow.ui.projects

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coflow.R
import com.example.coflow.ui.components.CoFlowGlassCard
import com.example.coflow.ui.components.CoFlowGlassTextField
import com.example.coflow.ui.components.CoFlowIcon
import com.example.coflow.ui.components.CoFlowPrimaryButton
import com.example.coflow.ui.components.CoFlowScreenHeader
import com.example.coflow.ui.theme.CoFlowColors
import com.example.coflow.ui.theme.CoFlowGlassStyles
import com.example.coflow.ui.theme.CoFlowSpacing
import com.example.coflow.ui.theme.CoFlowTheme
import com.example.coflow.ui.theme.coFlowTasksBackgroundBrush

@Composable
fun CreateProjectScreen(
    state: CreateProjectUiState,
    onBackClick: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAddParticipantClick: () -> Unit,
    onRemoveParticipantClick: (ProjectParticipant) -> Unit,
    onConfirmRemoveParticipant: () -> Unit,
    onDismissRemoveParticipantDialog: () -> Unit,
    onCreateProject: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    BackHandler(onBack = onBackClick)

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(coFlowTasksBackgroundBrush()),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = CoFlowSpacing.ScreenHorizontal),
        ) {
            CoFlowScreenHeader(
                title = stringResource(R.string.create_project_header),
                onBackClick = {
                    focusManager.clearFocus()
                    onBackClick()
                },
            )

            // Форма прокручивается, а кнопка создания остаётся внизу экрана.
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Название проекта
                CoFlowGlassTextField(
                    label = stringResource(R.string.create_project_name_label),
                    value = state.title,
                    onValueChange = onTitleChange,
                    placeholder = stringResource(R.string.create_project_name_placeholder),
                )

                // Описание проекта
                CoFlowGlassTextField(
                    label = stringResource(R.string.create_project_description_label),
                    value = state.description,
                    onValueChange = onDescriptionChange,
                    placeholder = stringResource(R.string.create_project_description_placeholder),
                    singleLine = false,
                    maxLength = 250,
                )

                // Участники
                Text(
                    text = stringResource(R.string.create_project_participants_label),
                    modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CoFlowColors.TextPrimary,
                )

                ParticipantsCard(
                    participants = state.participants,
                    onAddParticipantClick = {
                        focusManager.clearFocus()
                        onAddParticipantClick()
                    },
                    onRemoveParticipantClick = onRemoveParticipantClick,
                )

                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage,
                        modifier = Modifier.padding(horizontal = 4.dp),
                        fontSize = 12.sp,
                        color = CoFlowColors.Error,
                    )
                }

                Spacer(Modifier.height(4.dp))
            }

            CoFlowPrimaryButton(
                text = stringResource(R.string.create_project_button),
                onClick = {
                    focusManager.clearFocus()
                    onCreateProject()
                },
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                enabled = state.canCreate,
                leadingIcon = "check",
            )
        }
    }

    // Подтверждение отмены приглашения
    val participantPendingRemoval = state.participantPendingRemoval
    if (participantPendingRemoval != null) {
        AlertDialog(
            onDismissRequest = onDismissRemoveParticipantDialog,
            title = { Text(stringResource(R.string.create_project_cancel_invite_dialog_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.create_project_cancel_invite_dialog_message,
                        participantPendingRemoval.email,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmRemoveParticipant) {
                    Text(stringResource(R.string.create_project_cancel_invite_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRemoveParticipantDialog) {
                    Text(stringResource(R.string.create_project_cancel))
                }
            },
        )
    }
}

@Composable
fun CreateProjectRoute(
    onBackClick: () -> Unit,
    onProjectCreated: (CreateProjectResult) -> Unit,
    viewModel: CreateProjectViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CreateProjectScreen(
        state = state,
        onBackClick = onBackClick,
        onTitleChange = viewModel::onTitleChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onAddParticipantClick = viewModel::onAddParticipantClick,
        onRemoveParticipantClick = viewModel::onRemoveParticipantClick,
        onConfirmRemoveParticipant = viewModel::onConfirmRemoveParticipant,
        onDismissRemoveParticipantDialog = viewModel::onDismissRemoveParticipantDialog,
        onCreateProject = { viewModel.onCreateProject(onProjectCreated) },
    )
}

@Composable
private fun ParticipantsCard(
    participants: List<ProjectParticipant>,
    onAddParticipantClick: () -> Unit,
    onRemoveParticipantClick: (ProjectParticipant) -> Unit,
) {
    CoFlowGlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.create_project_participants_title),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = CoFlowColors.TextPrimary,
        )

        // Добавление участника — заглушка, позже откроется отдельное окно.
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onAddParticipantClick)
                    .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoFlowIcon(
                name = "person_plus",
                modifier = Modifier.size(22.dp),
                color = CoFlowColors.Blue,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.create_project_add_participant),
                fontSize = 14.sp,
                color = CoFlowColors.Blue,
            )
        }

        participants.forEach { participant ->
            HorizontalDivider(
                color = CoFlowGlassStyles.BlueBorder,
                thickness = 1.dp,
            )
            ParticipantRow(
                participant = participant,
                onRemoveClick = { onRemoveParticipantClick(participant) },
            )
        }
    }
}

@Composable
private fun ParticipantRow(
    participant: ProjectParticipant,
    onRemoveClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Аватарка: инициалы для зарегистрированного пользователя,
        // дефолтный человечек для остальных.
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PARTICIPANT_AVATAR_COLOR),
            contentAlignment = Alignment.Center,
        ) {
            if (participant.initials != null) {
                Text(
                    text = participant.initials,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoFlowColors.Blue,
                )
            } else {
                // Иконка «person» уже есть в CoFlowIcons.
                CoFlowIcon(
                    name = "person",
                    modifier = Modifier.size(20.dp),
                    color = CoFlowColors.Blue.copy(alpha = 0.75f),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = participant.email,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = CoFlowColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.width(8.dp))

        // Отмена приглашения — отдельное нажатие на красный минус, затем подтверждение.
        Box(
            modifier =
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(CoFlowColors.Error)
                    .clickable(onClick = onRemoveClick),
            contentAlignment = Alignment.Center,
        ) {
            CoFlowIcon(
                name = "minus",
                modifier = Modifier.size(14.dp),
                color = Color.White,
                contentDescription = stringResource(R.string.create_project_remove_participant),
            )
        }
    }
}

private val PARTICIPANT_AVATAR_COLOR = Color(0xFFC9DCFB)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CreateProjectScreenPreview() {
    CoFlowTheme {
        CreateProjectScreen(
            state =
                CreateProjectUiState(
                    title = "Новый проект",
                    description = "Описание проекта",
                    participants =
                        listOf(
                            ProjectParticipant("m.orlov@example.com", initials = "МО"),
                            ProjectParticipant("elena.v@example.com", initials = "ЕВ"),
                            ProjectParticipant("unknown@example.com"),
                        ),
                ),
            onBackClick = {},
            onTitleChange = {},
            onDescriptionChange = {},
            onAddParticipantClick = {},
            onRemoveParticipantClick = {},
            onConfirmRemoveParticipant = {},
            onDismissRemoveParticipantDialog = {},
            onCreateProject = {},
        )
    }
}

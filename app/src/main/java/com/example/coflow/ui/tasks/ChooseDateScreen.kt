package com.example.coflow.ui.tasks

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coflow.R
import com.example.coflow.ui.components.*
import com.example.coflow.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ChooseDateScreen(
    state: CreateTaskUiState,
    onBackClick: () -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onConfirmSelection: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val currentMonth = state.calendarMonth
    val selectedDate = state.selectedDueDate

    // Системная кнопка "Назад"
    BackHandler(onBack = onBackClick)

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
                title = stringResource(R.string.choose_date_header),
                onBackClick = {
                    focusManager.clearFocus()
                    onBackClick()
                },
            )

            // Карточка календаря
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = CoFlowGlassStyles.Shadow,
                            spotColor = CoFlowGlassStyles.Shadow,
                        ).clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.85f))
                        .padding(16.dp),
            ) {
                Column {
                    // Навигация по месяцам: название + стрелки влево/вправо
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            // Название месяца на русском (например, "Октябрь 2026")
                            // TODO: Добавить локализацию
                            text =
                                currentMonth
                                    .format(
                                        DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru")),
                                    ).replaceFirstChar { it.titlecase(Locale("ru")) },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CoFlowColors.TextPrimary,
                        )
                        Row {
                            // Кнопка "Предыдущий месяц"
                            Box(
                                modifier =
                                    Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .clickable { onMonthChange(currentMonth.minusMonths(1)) },
                                contentAlignment = Alignment.Center,
                            ) {
                                CoFlowIcon(
                                    name = "chevron_left",
                                    modifier = Modifier.size(16.dp),
                                    color = CoFlowColors.Blue,
                                    contentDescription = "Предыдущий месяц",
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            // Кнопка "Следующий месяц"
                            Box(
                                modifier =
                                    Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .clickable { onMonthChange(currentMonth.plusMonths(1)) },
                                contentAlignment = Alignment.Center,
                            ) {
                                CoFlowIcon(
                                    name = "chevron",
                                    modifier = Modifier.size(16.dp),
                                    color = CoFlowColors.Blue,
                                    contentDescription = "Следующий месяц",
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    // Заголовки дней недели
                    // TODO: Добавить локализацию
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { day ->
                            Text(
                                text = day,
                                modifier = Modifier.width(36.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = CoFlowColors.TextSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // СЕТКА ДАТ: 6 строк по 7 дней (42 ячейки)
                    CalendarGrid(
                        currentMonth = currentMonth,
                        selectedDate = selectedDate,
                    )
                    { date ->
                        if (date.month == currentMonth.month) onDateSelected(date)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Кнопка подтверждения
            CoFlowPrimaryButton(
                text = stringResource(R.string.done_button),
                onClick = {
                    focusManager.clearFocus()
                    onConfirmSelection(selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                },
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
) {
    val firstDayOfMonth = currentMonth.atDay(1)
    val offset = firstDayOfMonth.dayOfWeek.value - 1
    val daysInMonth = currentMonth.lengthOfMonth()
    val prevMonth = currentMonth.minusMonths(1)
    val daysInPrevMonth = prevMonth.lengthOfMonth()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (week in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (dayOfWeek in 0 until 7) {
                    val cellIndex = week * 7 + dayOfWeek
                    val (dayNumber, date, isCurrentMonth) =
                        when {
                            cellIndex < offset -> {
                                Triple(
                                    daysInPrevMonth - offset + cellIndex + 1,
                                    prevMonth.atDay(daysInPrevMonth - offset + cellIndex + 1),
                                    false,
                                )
                            }

                            cellIndex >= offset + daysInMonth -> {
                                Triple(
                                    cellIndex - (offset + daysInMonth) + 1,
                                    currentMonth.plusMonths(1).atDay(cellIndex - (offset + daysInMonth) + 1),
                                    false,
                                )
                            }

                            else -> {
                                Triple(cellIndex - offset + 1, currentMonth.atDay(cellIndex - offset + 1), true)
                            }
                        }
                    val isSelected = isCurrentMonth && date == selectedDate

                    Box(
                        modifier =
                            Modifier
                                .width(36.dp)
                                .height(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) CoFlowColors.Blue else Color.Transparent)
                                .clickable(enabled = isCurrentMonth) { if (isCurrentMonth) onDateSelected(date) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = dayNumber.toString(),
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color =
                                when {
                                    isSelected -> Color.White
                                    !isCurrentMonth -> CoFlowColors.TextSecondary.copy(alpha = 0.4f)
                                    else -> CoFlowColors.TextPrimary
                                },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChooseDateRoute(
    onBackClick: () -> Unit,
    onConfirmSelection: (String) -> Unit,
    viewModel: CreateTaskViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ChooseDateScreen(
        state = state,
        onBackClick = onBackClick,
        onMonthChange = viewModel::onCalendarMonthChange,
        onDateSelected = viewModel::onDateSelected,
        onConfirmSelection = { date ->
            viewModel.confirmDateSelection(date)
            onConfirmSelection(date)
        },
    )
}

@Preview(
    name = "Дата окончания",
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
)
@Composable
private fun ChooseDateScreenPreview() {
    CoFlowTheme {
        ChooseDateScreen(
            state = CreateTaskUiState(),
            onBackClick = {},
            onMonthChange = {},
            onDateSelected = {},
            onConfirmSelection = {},
        )
    }
}

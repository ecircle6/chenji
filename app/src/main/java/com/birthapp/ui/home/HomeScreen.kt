package com.birthapp.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.birthapp.ui.common.BirthdayCard
import com.birthapp.ui.common.EmptyBirthdayList
import com.birthapp.ui.theme.Coral500

private val TABS = listOf(
    "all" to "全部",
    "family" to "家人",
    "friend" to "朋友",
    "colleague" to "同事",
    "other" to "其他"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val birthdays by viewModel.displayBirthdays.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    var deleteTargetId by remember { mutableLongStateOf(-1L) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "生日提醒",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = onAddClick,
                containerColor = Coral500
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "添加生日",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Pill-style tabs
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(TABS) { (key, label) ->
                    val isSelected = selectedTab == key
                    Surface(
                        onClick = { viewModel.selectTab(key) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Coral500
                        else MaterialTheme.colorScheme.surface,
                        border = if (isSelected) null
                        else androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(
                                horizontal = 20.dp,
                                vertical = 10.dp
                            ),
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // List or empty state
            if (birthdays.isEmpty()) {
                EmptyBirthdayList(onAddClick = onAddClick)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .animateContentSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(birthdays, key = { _, item -> item.birthday.id }) { index, display ->
                        SwipeToDismissBox(
                            state = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        deleteTargetId = display.birthday.id
                                        false
                                    } else false
                                }
                            ),
                            backgroundContent = {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = Coral500
                                    )
                                }
                            },
                            enableDismissFromStartToEnd = false
                        ) {
                            BirthdayCard(
                                display = display,
                                index = index,
                                onClick = { onEditClick(display.birthday.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (deleteTargetId > 0) {
        val target = birthdays.find { it.birthday.id == deleteTargetId }
        AlertDialog(
            onDismissRequest = { deleteTargetId = -1L },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${target?.birthday?.name}」的生日记录吗？\n删除后将不再提醒。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        target?.let { viewModel.deleteBirthday(it.birthday) }
                        deleteTargetId = -1L
                    }
                ) {
                    Text("删除", color = Coral500)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetId = -1L }) {
                    Text("取消")
                }
            }
        )
    }
}

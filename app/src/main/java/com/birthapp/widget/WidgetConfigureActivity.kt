package com.birthapp.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.birthapp.BirthApp
import com.birthapp.data.Birthday
import com.birthapp.data.EventType
import com.birthapp.lunar.LunarCalendar
import com.birthapp.ui.theme.BirthAppTheme
import com.birthapp.ui.theme.Coral500
import com.birthapp.ui.theme.Teal500
import kotlinx.coroutines.launch

/**
 * 小组件配置页：首次把小组件拖到桌面时由系统自动打开（android:configure）。
 * 选择"自动（最近记录）"或指定一条记录，按 appWidgetId 存入设置，
 * 保存后立刻刷新桌面上的小组件
 */
class WidgetConfigureActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 系统通过 intent 告诉我们这是给哪个小组件实例配置的
        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val database = (application as BirthApp).database

        setContent {
            BirthAppTheme {
                var records by remember { mutableStateOf<List<Birthday>?>(null) }
                LaunchedEffect(Unit) {
                    records = database.birthdayDao().getAllOnce()
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("小组件展示", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    }
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            "选择小组件上展示的内容",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val current = WidgetConfigStore.get(this@WidgetConfigureActivity, appWidgetId)
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                SelectionRow(
                                    title = "自动（最近记录）",
                                    subtitle = "自动展示最近的生日与纪念日",
                                    selected = current == WidgetConfigStore.AUTO,
                                    onClick = { select(appWidgetId, WidgetConfigStore.AUTO) }
                                )
                            }
                            records?.forEach { b ->
                                item {
                                    SelectionRow(
                                        title = b.name,
                                        subtitle = recordSubtitle(b),
                                        selected = current == b.id.toString(),
                                        onClick = { select(appWidgetId, b.id.toString()) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** 保存选择并刷新桌面小组件，然后关闭配置页 */
    private fun select(appWidgetId: Int, selection: String) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        WidgetConfigStore.set(this, appWidgetId, selection)
        lifecycleScope.launch { WidgetRefresher.refresh(this@WidgetConfigureActivity) }
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        finish()
    }

    /** 记录副标题：类型 + 日期（与首页信息行同一套口径） */
    private fun recordSubtitle(b: Birthday): String {
        val dateLabel = if (b.calendarType == "lunar") {
            "农历${LunarCalendar.formatLunarDate(b.birthMonth, b.birthDay)}"
        } else {
            "${b.birthMonth}月${b.birthDay}日"
        }
        return "${EventType.label(b.eventType)} · $dateLabel"
    }
}

@Composable
private fun SelectionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Teal500.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) Teal500 else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = null)
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
            if (selected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "已选择",
                    tint = Coral500,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }
    }
}

package com.birthapp.ui.common

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.birthapp.ui.theme.Coral500
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 左滑删除的方向锁定判定：只有横向明显占优（|Δx| > |Δy|）才把手势判给左滑，
 * 斜向/垂直手势一律交给列表滚动，避免下滑滚动时误触发删除。
 */
internal enum class SwipeDirection { Undecided, Horizontal, Vertical }

internal fun swipeDirection(totalX: Float, totalY: Float, slop: Float): SwipeDirection {
    if (abs(totalX) < slop && abs(totalY) < slop) return SwipeDirection.Undecided
    // 两轴相等时按垂直处理，偏保守：宁可滚不动也不能误删
    return if (abs(totalX) > abs(totalY)) SwipeDirection.Horizontal else SwipeDirection.Vertical
}

/**
 * 自绘的滑动删除容器，替代 Material3 SwipeToDismissBox。
 *
 * 系统组件与 LazyColumn 的垂直滚动之间是「谁先越过触摸阈值谁赢」的竞态，
 * 下滑时手指带一点斜向分量就常被水平手势抢先、甚至因松手速度 >125dp/s 误弹删除。
 * 这里用方向锁定重写：累计位移先判定方向，横向占优才启动拖拽并消费事件；
 * 一旦发现列表滚动已消费事件立即放弃（也不再存在「滚动停止后捡起手势」的问题）。
 * 删除只按滑过卡片一半宽度判定，不做速度触发。
 *
 * @param onDelete 滑过半屏松手后回调（由调用方弹确认框，卡片随后回弹）
 * @param content  被拖拽的卡片本体
 */
@Composable
fun SwipeToDeleteBox(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // 容器宽度（px），用于「滑过半屏」的阈值判定
    var widthPx by remember { mutableFloatStateOf(0f) }
    // 卡片当前横向位移；拖拽时由手势直接写入，松手回弹由 settleJob 动画驱动
    var offsetPx by remember { mutableFloatStateOf(0f) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.onSizeChanged { widthPx = it.width.toFloat() }) {
        // 底层：右侧删除图标，卡片左滑时露出（视觉与旧 SwipeToDismissBox 保持一致）。
        // 必须用 matchParentSize 而非 fillMaxSize：列表项是无限高度约束，
        // fillMaxSize 会塌缩成图标自身高度、贴顶对齐，图标从卡片右上角透出来
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Delete, contentDescription = "删除", tint = Coral500)
        }
        // 顶层：卡片本体，跟随手指横向移动
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetPx.roundToInt(), 0) }
                .pointerInput(Unit) {
                    val slop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var totalX = 0f
                        var totalY = 0f
                        var dragging = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: break // 跟丢手指（多点/取消）直接放弃
                            // 列表滚动方已消费事件：绝不再抢（也堵住滚动结束后“捡起”误触）
                            if (change.isConsumed) break
                            if (!change.pressed) break // 手指抬起，进入收尾
                            if (!dragging) {
                                val delta = change.positionChange()
                                totalX += delta.x
                                totalY += delta.y
                                when (swipeDirection(totalX, totalY, slop)) {
                                    SwipeDirection.Horizontal -> {
                                        dragging = true
                                        settleJob?.cancel() // 回弹途中被重新拖住，先停掉旧动画
                                        change.consume() // 锁死为左滑，阻止列表把这次触摸抢去滚动
                                    }
                                    SwipeDirection.Vertical -> break // 判定为滚动，交给 LazyColumn 接管
                                    SwipeDirection.Undecided -> Unit
                                }
                            }
                            if (dragging) {
                                val delta = change.positionChange()
                                offsetPx = (offsetPx + delta.x).coerceIn(-widthPx, 0f)
                                change.consume()
                            }
                        }
                        if (dragging) {
                            // 只按滑过一半宽度判定删除，不做速度触发，快滑误触不再弹框
                            if (widthPx > 0f && offsetPx <= -widthPx * 0.5f) {
                                onDelete()
                            }
                            settleJob = scope.launch {
                                animate(
                                    initialValue = offsetPx,
                                    targetValue = 0f,
                                    initialVelocity = 0f,
                                    animationSpec = spring()
                                ) { value, _ -> offsetPx = value }
                            }
                        }
                    }
                }
        ) {
            content()
        }
    }
}

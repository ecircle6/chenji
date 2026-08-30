package com.birthapp.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 动效基建（v2.1.16）：错峰入场 / 倒计时滚动 / 呼吸光晕。
 *
 * 约定：
 * - 入场用 spring 回弹（约等于效果稿的 cubic-bezier(.24,1.1,.38,1)），错峰 55ms/项
 * - 数字滚动只用于倒计时变化（跨天/编辑后），日常重组不触发
 * - 呼吸光晕只给「今天」（UrgentCard 的边框呼吸是既有实现，语义一致：稀缺性 = 注意力）
 */

/**
 * 首屏错峰入场：列表项按索引依次上浮淡入，每个节点只在首次组合时播放。
 * 懒加载滚动到的新项也会播放入场（观感是"浮现"），筛选切换后列表重新组合时同样成立。
 *
 * enabled=false 时直接以最终态呈现、不启动动画——导航返回时列表会整体重组，
 * 若照常重放错峰会让人感觉"刷新慢"，由 [rememberStaggerPlay] 决定是否播放。
 */
fun Modifier.staggeredAppear(
    index: Int,
    enabled: Boolean = true,
    delayPerItemMs: Int = 40
): Modifier = composed {
    val alpha = remember { Animatable(if (enabled) 0f else 1f) }
    val offsetY = remember { Animatable(if (enabled) 28f else 0f) }
    LaunchedEffect(Unit) {
        if (!enabled) return@LaunchedEffect
        delay(index.coerceAtMost(8).toLong() * delayPerItemMs)
        launch {
            alpha.animateTo(
                1f,
                spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)
            )
        }
        launch {
            offsetY.animateTo(
                0f,
                spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)
            )
        }
    }
    graphicsLayer {
        this.alpha = alpha.value
        translationY = offsetY.value
    }
}

/**
 * 错峰播放闸门：冷启动首帧为 true（播放），随后置 false 并存入可保存状态——
 * 从添加/编辑/详情页返回时恢复的就是 false，列表即时呈现不重放；
 * 传入的筛选/搜索等 inputs 变化时状态重置回 true，错峰重新编排。
 */
@Composable
fun rememberStaggerPlay(vararg inputs: Any?): Boolean {
    // 必须展开传 *inputs：传数组本身的话每次重组都是新实例，状态会被反复重置成死循环
    var play by rememberSaveable(*inputs) { mutableStateOf(true) }
    SideEffect { play = false }
    return play
}

/**
 * 倒计时数字滚动：数字变化时旧值上滑淡出、新值自下滑入（odometer 观感）。
 * 等宽字阶（display 系 + tnum）保证滚动全程数字不横移。
 */
@Composable
fun AnimatedCountdownText(
    count: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displayMedium,
    color: Color = Color.Unspecified
) {
    Box(modifier = modifier) {
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                (
                    slideInVertically(
                        spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                    ) { it / 2 } + fadeIn()
                    )
                    .togetherWith(
                        slideOutVertically(
                            spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                        ) { -it / 2 } + fadeOut()
                    )
            },
            label = "countdown-roll"
        ) { value ->
            Text(text = "$value", style = style, color = color)
        }
    }
}

/**
 * 呼吸光晕：在宿主外圈绘制一圈扩散-收缩的圆角描边（2.6s/周期）。
 * 画在 drawBehind，即宿主自身背景之下、边界之外不受宿主裁剪影响；
 * 只用于「今天」的卡片/横幅，全程只有一个元素在呼吸。
 */
fun Modifier.breathingGlow(
    color: Color,
    cornerRadius: Dp = 16.dp
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "breathe-glow")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe-t"
    )
    drawBehind {
        val expand = size.minDimension * 0.045f * (1f - t) + 2.dp.toPx()
        val alpha = 0.30f * (1f - t) + 0.04f
        drawRoundRect(
            color = color.copy(alpha = alpha),
            topLeft = Offset(-expand, -expand),
            size = Size(size.width + expand * 2, size.height + expand * 2),
            cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
        )
    }
}

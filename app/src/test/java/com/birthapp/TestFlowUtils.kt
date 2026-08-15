package com.birthapp

import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.robolectric.Shadows

/**
 * 轮询等待 StateFlow 满足条件。
 *
 * Robolectric 下 Room 的查询发射在 arch_disk_io 后台线程、ViewModel 的
 * viewModelScope 跑在主 looper 上：这里每轮先 idle 主 looper 消化已排队的
 * 协程任务，再用真实时间等 Room 后台线程出结果，两者都覆盖到。
 */
fun <T> StateFlow<T>.awaitValue(
    predicate: (T) -> Boolean,
    label: String = "状态"
): T {
    // 先订阅激活 stateIn（WhileSubscribed 才有上游发射）
    val scope = CoroutineScope(Dispatchers.IO)
    val job: Job = scope.launch { collect {} }
    try {
        val deadline = System.currentTimeMillis() + 8000
        while (System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            if (predicate(value)) return value
            Thread.sleep(20)
        }
        throw AssertionError("等待 $label 超时，当前值: $value")
    } finally {
        job.cancel()
    }
}

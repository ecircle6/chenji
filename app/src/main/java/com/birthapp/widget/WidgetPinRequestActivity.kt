package com.birthapp.widget

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

/**
 * 「长按图标 → 添加小组件」的透明中转页。
 *
 * 快捷方式不直接进 MainActivity（否则 App 会被整个拉起并留在前台），
 * 而是进这个无界面页面：onResume 时（满足 requestPinAppWidget 的
 * 「前台调用」硬性要求）只发一个放置请求就退场。
 * 用户全程只见系统放置框，App 不开界面、不进最近任务、不闪屏。
 */
class WidgetPinRequestActivity : Activity() {

    private var requested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 页面本身不渲染任何内容；只接受快捷方式的 action，其他来源直接结束。
        // 注意：finish() 之后生命周期仍会走到 onResume，必须置位 requested 挡掉，
        // 否则错误来源也会在 onResume 里把放置请求发出去
        if (intent?.action != ACTION_REQUEST_WIDGET) {
            requested = true
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (requested) return
        requested = true

        val ok = WidgetPinner.request(this, WidgetPinner.successPendingIntent(this))
        if (!ok) {
            Toast.makeText(
                this,
                "当前桌面不支持直接添加，请长按桌面空白处 → 小组件 → 辰记",
                Toast.LENGTH_LONG
            ).show()
        }

        // 放置框由 system/launcher 持有，不随本页销毁。等放置框盖上来
        // （本页被暂停）或 600ms 超时，先到先退场，绝不留存根
        window.decorView.postDelayed({ if (!isFinishing) finish() }, 600)
    }

    override fun onPause() {
        super.onPause()
        // 放置框（若是独立页面窗口）已把本页盖住 → 可以退场了
        if (requested && !isFinishing) finish()
    }

    companion object {
        /** 与 res/xml/shortcuts.xml 里快捷方式 intent 的 action 一致 */
        const val ACTION_REQUEST_WIDGET = "com.birthapp.action.REQUEST_WIDGET"
    }
}
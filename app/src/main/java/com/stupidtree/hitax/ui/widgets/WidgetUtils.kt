package com.stupidtree.hitax.ui.widgets

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.stupidtree.hitax.ui.widgets.today.normal.TodayWidget
import com.stupidtree.hitax.ui.widgets.today.slim.TodayWidgetSlim

object WidgetUtils {
    val widgets = listOf(TodayWidget::class.java, TodayWidgetSlim::class.java)
    const val EVENT_REFRESH = "com.stupidtree.hita.WIDGET_EVENT_REFRESH"
    private const val REFRESH_THROTTLE_MS = 1500L
    private var lastRefreshAt = 0L

    fun sendRefreshToAll(context: Context, force: Boolean = false){
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastRefreshAt < REFRESH_THROTTLE_MS) {
            return
        }
        lastRefreshAt = now
        val appContext = context.applicationContext
        for(wid in widgets){
            val btIntent = Intent().setAction(EVENT_REFRESH)
            btIntent.setClass(appContext, wid)
            appContext.sendBroadcast(btIntent)
        }
    }
}

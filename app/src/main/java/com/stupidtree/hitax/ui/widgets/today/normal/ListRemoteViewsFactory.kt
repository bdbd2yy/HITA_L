package com.stupidtree.hitax.ui.widgets.today.normal

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.stupidtree.hitax.R
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.data.repository.TimetableRepository
import com.stupidtree.hitax.ui.widgets.today.TodayUtils
import com.stupidtree.hitax.ui.widgets.today.normal.TodayWidget.Companion.EVENT_EXTRA
import com.stupidtree.hitax.utils.TimeTools

internal class ListRemoteViewsFactory(val mContext: Context, intent: Intent) :
    RemoteViewsService.RemoteViewsFactory {
    //private val executor = Executors.newSingleThreadExecutor()
    private val mBeans = mutableListOf<EventItem>()
    private val appWidgetId = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )

    override fun getViewAt(position: Int): RemoteViews {
        // 获取 item_widget_device.xml 对应的RemoteViews
        val rv = RemoteViews(mContext.packageName, R.layout.widget_today_item)

        // 设置 第position位的“视图”的数据
        val event = mBeans[position]
        rv.setTextViewText(R.id.name, event.name)
        rv.setTextViewText(
            R.id.time, TimeTools.printTime(event.from.time) + "-" + TimeTools.printTime(
                event.to.time
            )
        )
        val result =
            if (TextUtils.isEmpty(event.place)) mContext.getString(R.string.unknown_location_widget) else event.place
        rv.setTextViewText(R.id.location, result)
        val dark = TodayUtils.isWidgetDarkTheme(mContext)
        rv.setInt(
            R.id.location_wrap,
            "setBackgroundResource",
            if (dark) R.drawable.widget_rounded_bar_dark else R.drawable.widget_rounded_bar
        )
        rv.setTextColor(R.id.name, if (dark) Color.parseColor("#EDF3FF") else Color.parseColor("#202020"))
        rv.setTextColor(R.id.time, if (dark) Color.parseColor("#9AA9C2") else Color.parseColor("#66202020"))
        rv.setTextColor(R.id.location, if (dark) Color.parseColor("#AFCBFF") else Color.parseColor("#2F4CFE"))
        rv.setInt(
            R.id.ic_sub,
            "setBackgroundResource",
            if (dark) R.drawable.widget_ic_clock_white else R.drawable.widget_ic_clock
        )
        rv.setInt(
            R.id.loc_ic,
            "setBackgroundResource",
            if (dark) R.drawable.widget_ic_location_white else R.drawable.widget_ic_location
        )

        val lockIntent = Intent()
        lockIntent.putExtra(EVENT_EXTRA, event.id)
        val bd = Bundle()
        bd.putString("eventId", event.id)
        bd.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        lockIntent.putExtras(bd)
        rv.setOnClickFillInIntent(R.id.item, lockIntent)
        return rv
    }

    override fun getLoadingView(): RemoteViews? {
        return null//return RemoteViews(mContext.packageName, R.layout.widget_today_loading)
    }

    override fun getViewTypeCount(): Int {
        return 1
    }


    override fun onCreate() {
        // 初始化“集合视图”中的数据
//        executor.execute {
//            val timetableRepo =
//                TimetableRepository.getInstance(mContext.applicationContext as Application)
//            val events = timetableRepo.getTodayEvents()
//            refresh(events)
//        }
    }

    override fun onDataSetChanged() {
        val timetableRepo =
            TimetableRepository.getInstance(mContext.applicationContext as Application)
        val events = timetableRepo.getTodayEventsSync()
        mBeans.clear()
        mBeans.addAll(events)
    }


    override fun getItemId(position: Int): Long {
        // 返回当前项在“集合视图”中的位置
        return position.toLong()
    }


    override fun hasStableIds(): Boolean {
        return false
    }

    override fun getCount(): Int {
        return mBeans.size
    }

    override fun onDestroy() {
        mBeans.clear()
    }

}

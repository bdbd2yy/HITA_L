package com.stupidtree.hitax.ui.main.navigation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.stupidtree.component.data.Trigger
import com.stupidtree.hitax.data.model.timetable.Timetable
import com.stupidtree.hitax.data.repository.TimetableRepository

class NavigationViewModel(application: Application) : AndroidViewModel(application) {

    private val timetableRepository = TimetableRepository.getInstance(application)

    private val recentTimetableController = MutableLiveData<Trigger>()
    val recentTimetableLiveData: LiveData<Timetable?> = recentTimetableController.switchMap {
            return@switchMap timetableRepository.getRecentTimetable()
        }
    val timetableCountLiveData: LiveData<Int> = recentTimetableController.switchMap {
            return@switchMap timetableRepository.getTimetableCount()
        }
    val unreadMessageLiveData: LiveData<Int> = MutableLiveData(0)

    fun startRefresh() {
        recentTimetableController.value = Trigger.actioning
    }
}

package com.birthapp.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.birthapp.BirthApp
import com.birthapp.data.AppDatabase
import com.birthapp.data.Birthday
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * 日历页 ViewModel：展示全部记录（含暂停），不做任何筛选。
 * 月历页的定位是"全局纵览"，跟首页的筛选维度无关；
 * 沿用 @JvmOverloads + in-memory 注入模式，测试可直接注入真实 Room。
 */
class CalendarViewModel @JvmOverloads constructor(
    application: Application,
    private val database: AppDatabase = (application as BirthApp).database
) : AndroidViewModel(application) {

    val allBirthdays: StateFlow<List<Birthday>> =
        database.birthdayDao().getAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
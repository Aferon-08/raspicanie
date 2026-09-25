package com.example

import android.app.Application
import com.example.data.repository.ScheduleRepository
import com.example.notifications.NotificationHelper

class PlanovoApp : Application() {

    lateinit var repository: ScheduleRepository
        private set

    lateinit var notificationHelper: NotificationHelper
        private set

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        repository = ScheduleRepository(this)
    }
}

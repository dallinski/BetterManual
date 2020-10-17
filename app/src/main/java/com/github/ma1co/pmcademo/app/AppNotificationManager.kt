package com.github.ma1co.pmcademo.app

import java.util.ArrayList

class AppNotificationManager private constructor() {
    interface NotificationListener {
        fun onNotify(message: String?)
    }

    private val listeners = ArrayList<NotificationListener>()
    fun notify(message: String?) {
        for (listener in listeners) listener.onNotify(message)
    }

    fun addListener(listener: NotificationListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: NotificationListener) {
        listeners.remove(listener)
    }

    companion object {
        @JvmStatic
        val instance = AppNotificationManager()
    }
}
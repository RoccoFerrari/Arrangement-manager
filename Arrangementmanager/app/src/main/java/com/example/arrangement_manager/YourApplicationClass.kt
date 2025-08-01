package com.example.arrangement_manager

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class YourApplicationClass : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())

    val database: ArrangementDatabase by lazy { ArrangementDatabase.getDatabase(this, applicationScope) }

    val arrangementDAO: ArrangementDAO by lazy { database.arrangementDao() }

    override fun onCreate() {
        super.onCreate()
    }

}
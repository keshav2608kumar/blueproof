package com.blueproof

import android.app.Application
import com.blueproof.data.db.BlueProofDatabase
import com.blueproof.data.repository.AttendanceRepository

class BlueProofApp : Application() {

    val database: BlueProofDatabase by lazy {
        BlueProofDatabase.getInstance(this)
    }

    val repository: AttendanceRepository by lazy {
        AttendanceRepository(database)
    }

    override fun onCreate() {
        super.onCreate()
    }
}

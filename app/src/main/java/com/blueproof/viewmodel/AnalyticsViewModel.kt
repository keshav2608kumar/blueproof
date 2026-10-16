package com.blueproof.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blueproof.BlueProofApp
import com.blueproof.data.db.entity.SessionEntity
import com.blueproof.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as BlueProofApp).repository

    data class AnalyticsData(
        val totalSessions: Int = 0,
        val totalCheckIns: Int = 0,
        val overallAttendanceRate: Float = 0f,
        val presentAbsentBreakdown: List<Pair<String, Float>> = emptyList(),
        val sessionTrend: List<Pair<String, Float>> = emptyList(),
        val statusDonut: List<Pair<String, Float>> = emptyList()
    )

    private val _analyticsData = MutableStateFlow(AnalyticsData())
    val analyticsData: StateFlow<AnalyticsData> = _analyticsData.asStateFlow()

    private val _sessions = MutableStateFlow<List<SessionEntity>>(emptyList())
    val sessions: StateFlow<List<SessionEntity>> = _sessions.asStateFlow()

    init {
        loadAnalytics()
    }

    fun loadAnalytics() {
        viewModelScope.launch {
            val allSessions = repository.getAllSessions()
            _sessions.value = allSessions

            var totalAccepted = 0
            var totalRejected = 0
            var totalPending = 0
            val trendData = mutableListOf<Pair<String, Float>>()

            allSessions.take(10).reversed().forEach { session ->
                val stats = repository.getSessionStats(session.sessionId)
                totalAccepted += stats.accepted
                totalRejected += stats.rejected
                totalPending += stats.pending

                val rate = if (stats.total > 0) (stats.accepted.toFloat() / stats.total) * 100f else 0f
                val shortName = session.name.take(8)
                trendData.add(Pair(shortName, rate))
            }

            val totalRecords = totalAccepted + totalRejected + totalPending
            val overallRate = if (totalRecords > 0) (totalAccepted.toFloat() / totalRecords) * 100f else 0f

            _analyticsData.value = AnalyticsData(
                totalSessions = allSessions.size,
                totalCheckIns = totalRecords,
                overallAttendanceRate = overallRate,
                presentAbsentBreakdown = listOf(
                    Pair("Verified", totalAccepted.toFloat()),
                    Pair("Flagged", (totalRejected + totalPending).toFloat())
                ),
                sessionTrend = trendData,
                statusDonut = listOf(
                    Pair("Accepted", totalAccepted.toFloat()),
                    Pair("Rejected", totalRejected.toFloat()),
                    Pair("Pending", totalPending.toFloat())
                ).filter { it.second > 0f }
            )
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AnalyticsViewModel(application) as T
        }
    }
}

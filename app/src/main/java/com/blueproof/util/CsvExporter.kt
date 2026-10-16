package com.blueproof.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.blueproof.data.db.entity.AttendanceRecordEntity
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    fun exportAttendanceCsv(
        context: Context,
        sessionName: String,
        records: List<AttendanceRecordEntity>
    ): Uri? {
        return try {
            val sanitizedSessionName = sessionName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Attendance_${sanitizedSessionName}_$timestamp.csv"

            val reportsDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "attendance_reports")
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }

            val csvFile = File(reportsDir, fileName)
            FileWriter(csvFile).use { writer ->
                // CSV Header
                writer.append("Student ID,Student Name,Check-In Time,RSSI (dBm),Status,Rejection Reason\n")

                // Records
                for (record in records) {
                    val line = listOf(
                        escapeCsv(record.studentId),
                        escapeCsv(record.studentName),
                        escapeCsv(formatTimestamp(record.checkInTime)),
                        record.rssiMedian.toString(),
                        escapeCsv(record.status),
                        escapeCsv(record.rejectionReason ?: "")
                    ).joinToString(",")
                    writer.append(line).append("\n")
                }
                writer.flush()
            }

            try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    csvFile
                )
            } catch (e: Exception) {
                Uri.fromFile(csvFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun formatTimestamp(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            "\"$escaped\""
        } else {
            escaped
        }
    }
}

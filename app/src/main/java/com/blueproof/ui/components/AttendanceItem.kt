package com.blueproof.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blueproof.data.db.entity.AttendanceRecordEntity
import com.blueproof.ui.theme.*
import com.blueproof.util.Constants
import com.blueproof.util.RssiAnalyzer
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AttendanceListItem(
    record: AttendanceRecordEntity,
    modifier: Modifier = Modifier
) {
    val statusColor = when (record.status) {
        Constants.STATUS_ACCEPTED -> StatusAccepted
        Constants.STATUS_REJECTED -> StatusRejected
        else -> StatusPending
    }

    val badgeLabel = when (record.status) {
        Constants.STATUS_ACCEPTED -> "VERIFIED"
        Constants.STATUS_REJECTED -> "FLAGGED"
        Constants.STATUS_PENDING_REVIEW -> "PENDING"
        else -> record.status
    }

    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(record.checkInTime))

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.studentName.ifEmpty { record.studentId },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = record.studentId,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (record.rejectionReason != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = record.rejectionReason,
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusRejected
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                // Status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeLabel,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${record.rssiMedian} dBm (${RssiAnalyzer.signalStrength(record.rssiMedian)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

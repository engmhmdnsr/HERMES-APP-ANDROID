package ee.oversight.hermes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.oversight.hermes.model.AppLanguage
import ee.oversight.hermes.model.CronJob
import ee.oversight.hermes.model.HermesStrings
import ee.oversight.hermes.ui.theme.CyberBg
import ee.oversight.hermes.ui.theme.CyberSurfaceBorder
import ee.oversight.hermes.ui.theme.CyberSurfaceElevated
import ee.oversight.hermes.ui.theme.MonospaceStyle
import ee.oversight.hermes.ui.theme.NeonAmber
import ee.oversight.hermes.ui.theme.NeonCyan
import ee.oversight.hermes.ui.theme.NeonGreen
import ee.oversight.hermes.ui.theme.NeonRed
import ee.oversight.hermes.ui.theme.NeonViolet
import ee.oversight.hermes.ui.theme.NeonVioletLight
import ee.oversight.hermes.ui.theme.TextPrimary
import ee.oversight.hermes.ui.theme.TextSecondary

/**
 * Scheduled jobs (cron) management: list server-side cron jobs and let the
 * user create / pause / resume / run / delete them. Powered by /api/jobs.
 */
@Composable
fun JobsScreen(
    jobs: List<CronJob>,
    isLoading: Boolean,
    language: AppLanguage,
    onRefresh: () -> Unit,
    onCreateJob: (name: String, schedule: String, prompt: String) -> Unit,
    onJobAction: (jobId: String, action: String) -> Unit,
    onDeleteJob: (jobId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBg)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Schedule, null, tint = NeonViolet, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (language == AppLanguage.AR) "المهام المجدولة" else "SCHEDULED JOBS",
                    style = MonospaceStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                )
                Text(
                    text = if (language == AppLanguage.AR)
                        "يعمل Hermes تلقائيًا حسب الجدول"
                    else
                        "Hermes runs automatically on a schedule",
                    style = MonospaceStyle.copy(fontSize = 10.sp, color = TextSecondary)
                )
            }
            IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (isLoading && jobs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonCyan, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
            }
            return
        }

        if (jobs.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Timer, null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(44.dp))
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = HermesStrings.jobsEmptyTitle(language),
                    style = MonospaceStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = HermesStrings.jobsEmptyDesc(language),
                    style = MonospaceStyle.copy(fontSize = 11.sp, color = TextSecondary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(jobs, key = { it.id }) { job ->
                    JobCard(job = job, language = language, onAction = onJobAction, onDelete = onDeleteJob)
                }
            }
        }

        // Create button
        Button(
            onClick = { showCreateDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(46.dp)
        ) {
            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = HermesStrings.jobsCreateNew(language),
                style = MonospaceStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            )
        }
    }

    if (showCreateDialog) {
        CreateJobDialog(
            language = language,
            onDismiss = { showCreateDialog = false },
            onCreate = { name, schedule, prompt ->
                showCreateDialog = false
                onCreateJob(name, schedule, prompt)
            }
        )
    }
}

@Composable
private fun JobCard(
    job: CronJob,
    language: AppLanguage,
    onAction: (jobId: String, action: String) -> Unit,
    onDelete: (jobId: String) -> Unit
) {
    val enabled = job.enabled
    var confirmDelete by remember { mutableStateOf(false) }
    val statusColor = when {
        job.lastStatus == "error" -> NeonRed
        enabled -> NeonGreen
        else -> NeonAmber
    }
    val statusText = when {
        job.lastStatus == "error" -> if (language == AppLanguage.AR) "خطأ" else "ERROR"
        enabled -> if (language == AppLanguage.AR) "مفعّلة" else "ENABLED"
        else -> if (language == AppLanguage.AR) "موقوفة" else "PAUSED"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CyberSurfaceElevated)
            .border(1.dp, if (enabled) NeonViolet.copy(alpha = 0.3f) else CyberSurfaceBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = job.name,
                style = MonospaceStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = statusText,
                    style = MonospaceStyle.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = statusColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Schedule row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Schedule, null, tint = NeonCyan.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = job.scheduleDisplay.ifBlank { job.scheduleKind },
                style = MonospaceStyle.copy(fontSize = 10.sp, color = NeonCyan)
            )
        }

        // Prompt snippet
        if (job.prompt.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = job.prompt,
                style = MonospaceStyle.copy(fontSize = 10.5.sp, color = TextSecondary),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Next / last run
        Row(modifier = Modifier.padding(top = 6.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = HermesStrings.jobsNextRun(language),
                    style = MonospaceStyle.copy(fontSize = 8.sp, color = TextSecondary.copy(alpha = 0.7f))
                )
                Text(
                    text = formatIso(job.nextRunAt) ?: (if (language == AppLanguage.AR) "—" else "—"),
                    style = MonospaceStyle.copy(fontSize = 10.sp, color = TextPrimary)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = HermesStrings.jobsLastRun(language),
                    style = MonospaceStyle.copy(fontSize = 8.sp, color = TextSecondary.copy(alpha = 0.7f))
                )
                Text(
                    text = formatIso(job.lastRunAt) ?: (if (language == AppLanguage.AR) "لم يشغّل بعد" else "Never"),
                    style = MonospaceStyle.copy(fontSize = 10.sp, color = TextPrimary)
                )
            }
        }

        // Error display
        if (job.lastStatus == "error" && !job.lastError.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = job.lastError.take(120),
                style = MonospaceStyle.copy(fontSize = 9.sp, color = NeonRed),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Actions row
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (enabled) {
                TextButton(onClick = { onAction(job.id, "pause") }, modifier = Modifier.height(30.dp)) {
                    Icon(Icons.Default.Pause, null, tint = NeonAmber, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (language == AppLanguage.AR) "إيقاف" else "Pause", style = MonospaceStyle.copy(fontSize = 10.sp, color = NeonAmber))
                }
            } else {
                TextButton(onClick = { onAction(job.id, "resume") }, modifier = Modifier.height(30.dp)) {
                    Icon(Icons.Default.PlayArrow, null, tint = NeonGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (language == AppLanguage.AR) "تشغيل" else "Resume", style = MonospaceStyle.copy(fontSize = 10.sp, color = NeonGreen))
                }
            }
            TextButton(onClick = { onAction(job.id, "run") }, modifier = Modifier.height(30.dp)) {
                Icon(Icons.Default.Timer, null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (language == AppLanguage.AR) "شغّل الآن" else "Run now", style = MonospaceStyle.copy(fontSize = 10.sp, color = NeonCyan))
            }
            IconButton(onClick = { confirmDelete = true }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NeonRed, modifier = Modifier.size(16.dp))
            }
        }
    }

    // Delete confirmation (destructive + irreversible: the job is gone server-side).
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Color(0xFF0F1420),
            titleContentColor = NeonRed,
            textContentColor = TextPrimary,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, null, tint = NeonRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.AR) "حذف المهمة" else "Delete Job",
                        style = MonospaceStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Text(
                    text = if (language == AppLanguage.AR)
                        "متأكد إنك عايز تحذف \"${job.name}\" نهائيًا؟ المهمة هتتشف من السيرفر ومفيش رجعة."
                    else
                        "Delete \"${job.name}\" permanently? The job is removed from the server and cannot be undone.",
                    style = MonospaceStyle.copy(fontSize = 13.sp, color = TextSecondary)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete(job.id)
                }) {
                    Text(
                        text = if (language == AppLanguage.AR) "حذف" else "Delete",
                        style = MonospaceStyle.copy(color = NeonRed, fontWeight = FontWeight.Bold)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(
                        text = if (language == AppLanguage.AR) "إلغاء" else "Cancel",
                        style = MonospaceStyle.copy(color = TextSecondary)
                    )
                }
            }
        )
    }
}

@Composable
private fun CreateJobDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onCreate: (name: String, schedule: String, prompt: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var schedule by remember { mutableStateOf("every 1h") }
    var prompt by remember { mutableStateOf("") }
    var scheduleError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F1420),
        titleContentColor = NeonViolet,
        textContentColor = TextPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, null, tint = NeonViolet, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (language == AppLanguage.AR) "مهمة جديدة" else "New Job",
                    style = MonospaceStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(HermesStrings.jobsJobNameLabel(language), style = MonospaceStyle.copy(fontSize = 11.sp)) },
                    singleLine = true,
                    textStyle = MonospaceStyle.copy(color = TextPrimary, fontSize = 12.sp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = schedule,
                    onValueChange = { schedule = it; scheduleError = null },
                    label = { Text(HermesStrings.jobsScheduleLabel(language), style = MonospaceStyle.copy(fontSize = 11.sp)) },
                    placeholder = { Text(HermesStrings.jobsScheduleHint(language), style = MonospaceStyle.copy(fontSize = 9.sp)) },
                    singleLine = true,
                    isError = scheduleError != null,
                    textStyle = MonospaceStyle.copy(color = TextPrimary, fontSize = 12.sp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (scheduleError != null) {
                    Text(
                        text = scheduleError!!,
                        style = MonospaceStyle.copy(fontSize = 10.sp, color = NeonRed)
                    )
                }
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text(HermesStrings.jobsPromptLabel(language), style = MonospaceStyle.copy(fontSize = 11.sp)) },
                    placeholder = { Text("e.g. Send me a summary of today's news", style = MonospaceStyle.copy(fontSize = 10.sp, color = TextSecondary)) },
                    minLines = 3,
                    maxLines = 5,
                    textStyle = MonospaceStyle.copy(color = TextPrimary, fontSize = 12.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val sched = schedule.trim()
                val err = validateSchedule(sched)
                if (err != null) {
                    scheduleError = err
                    return@TextButton
                }
                if (name.isNotBlank() && sched.isNotEmpty() && prompt.isNotBlank()) {
                    onCreate(name.trim(), sched, prompt.trim())
                }
            }) {
                Text(
                    text = if (language == AppLanguage.AR) "إنشاء" else "Create",
                    style = MonospaceStyle.copy(color = NeonViolet, fontWeight = FontWeight.Bold)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = if (language == AppLanguage.AR) "إلغاء" else "Cancel",
                    style = MonospaceStyle.copy(color = TextSecondary)
                )
            }
        }
    )
}

/**
 * Validate a cron schedule expression before sending it to the server.
 * Accepts: "every <n><unit>" (every 1h / every 30m / every monday 9am),
 * ISO "at <datetime>", or a 5-field cron expression.
 * Returns a human-readable error string, or null when valid.
 */
private fun validateSchedule(schedule: String): String? {
    val s = schedule.trim()
    if (s.isEmpty()) return "Schedule is required"
    val lower = s.lowercase()
    return when {
        // "every ..." natural phrases — pass through, server parses them.
        lower.startsWith("every ") -> null
        // Cron: 5 whitespace-separated fields of digits/*/?/,/-.
        s.matches(Regex("""^[0-9*/?,A-Za-z-]+(\s+[0-9*/?,A-Za-z-]+){4}$""")) -> null
        // ISO / "at ..." phrases.
        lower.startsWith("at ") -> null
        s.contains(":") && s.contains("-") && s.contains("T") -> null // ISO datetime
        else -> "Invalid schedule. Use formats like: every 1h, every monday 9am, 0 9 * * *, or an ISO date"
    }
}

/** Format an ISO-8601 timestamp to a short readable local string. */
private fun formatIso(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    return try {
        // Parse ISO-8601 (e.g. 2026-09-07T04:11:42.366951+00:00) with a format
        // that works on API 24 (no java.time without desugaring).
        val cleaned = iso.replace("Z", "+00:00")
        val parsed = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US).parse(cleaned)
            ?: java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US).parse(cleaned)
        val out = java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.US)
        out.timeZone = java.util.TimeZone.getDefault()
        out.format(parsed)
    } catch (e: Exception) {
        iso.take(16)
    }
}

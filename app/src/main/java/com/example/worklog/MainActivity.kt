package com.example.worklog

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WorkLogApp(this)
                }
            }
        }
    }
}

class WorkLogRepo(context: Context) {
    private val prefs = context.getSharedPreferences("worklog", Context.MODE_PRIVATE)

    fun getAllHours(): Map<String, Float> {
        val json = prefs.getString("hours", "{}") ?: "{}"
        val obj = JSONObject(json)
        return obj.keys().asSequence().associateWith { obj.getDouble(it).toFloat() }
    }

    fun setHours(date: LocalDate, hours: Float) {
        val json = prefs.getString("hours", "{}") ?: "{}"
        val obj = JSONObject(json)
        if (hours <= 0f) obj.remove(date.toString())
        else obj.put(date.toString(), hours.toDouble())
        prefs.edit().putString("hours", obj.toString()).apply()
    }

    fun getRate(): Float = prefs.getFloat("rate", 0f)
    fun setRate(rate: Float) = prefs.edit().putFloat("rate", rate).apply()
}

private fun formatNum(f: Float): String =
    if (f % 1f == 0f) f.toInt().toString() else f.toString()

private fun formatMoney(f: Float): String =
    DecimalFormat("#,###").format(f.toLong())

@Composable
fun WorkLogApp(context: Context) {
    val repo = remember { WorkLogRepo(context) }
    var hoursMap by remember { mutableStateOf(repo.getAllHours()) }
    var rate by remember { mutableStateOf(repo.getRate()) }
    var rateText by remember {
        mutableStateOf(if (repo.getRate() > 0f) formatNum(repo.getRate()) else "")
    }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    val monthHoursList = (1..currentMonth.lengthOfMonth()).map { day ->
        hoursMap[currentMonth.atDay(day).toString()] ?: 0f
    }
    val totalHours = monthHoursList.sum()
    val totalEarnings = totalHours * rate

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Ghi giờ làm",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = rateText,
            onValueChange = { s ->
                rateText = s
                val v = s.replace(',', '.').toFloatOrNull() ?: 0f
                rate = v
                repo.setRate(v)
            },
            label = { Text("Giá tiền mỗi giờ (VNĐ)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Tháng trước")
            }
            Text(
                "Tháng ${currentMonth.monthValue}/${currentMonth.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Tháng sau")
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN").forEach { d ->
                Text(
                    d,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        val ym = currentMonth
        val startOffset = (ym.atDay(1).dayOfWeek.value - 1) % 7
        val totalCells = startOffset + ym.lengthOfMonth()
        val rows = (totalCells + 6) / 7
        val today = LocalDate.now()

        Column(modifier = Modifier.weight(1f)) {
            for (r in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    for (c in 0 until 7) {
                        val idx = r * 7 + c
                        val dayNum = idx - startOffset + 1
                        if (dayNum in 1..ym.lengthOfMonth()) {
                            val date = ym.atDay(dayNum)
                            DayCell(
                                day = dayNum,
                                hours = hoursMap[date.toString()] ?: 0f,
                                isToday = date == today,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                onClick = { selectedDate = date }
                            )
                        } else {
                            Spacer(Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tổng giờ", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${formatNum(totalHours)} giờ",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Tổng tiền",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${formatMoney(totalEarnings)} đ",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    selectedDate?.let { date ->
        HoursDialog(
            date = date,
            currentHours = hoursMap[date.toString()] ?: 0f,
            onDismiss = { selectedDate = null },
            onConfirm = { hrs ->
                repo.setHours(date, hrs)
                hoursMap = repo.getAllHours()
                selectedDate = null
            }
        )
    }
}

@Composable
private fun DayCell(
    day: Int,
    hours: Float,
    isToday: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = if (hours > 0f)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .then(
                if (isToday)
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                day.toString(),
                fontSize = 14.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )
            if (hours > 0f) {
                Text(
                    "${formatNum(hours)}h",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun HoursDialog(
    date: LocalDate,
    currentHours: Float,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var input by remember {
        mutableStateOf(if (currentHours > 0f) formatNum(currentHours) else "")
    }
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ngày ${date.format(formatter)}") },
        text = {
            Column {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Số giờ (VD: 8 hoặc 7.5)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Text("Nhập 0 để xóa", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val h = input.replace(',', '.').toFloatOrNull() ?: 0f
                onConfirm(h)
            }) { Text("Lưu") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

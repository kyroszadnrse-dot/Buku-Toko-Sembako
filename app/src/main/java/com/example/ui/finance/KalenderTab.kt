package com.example.ui.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActivityType
import com.example.model.CashTransaction
import com.example.model.Sale
import com.example.ui.components.MetricRowLine
import com.example.ui.components.StatusBadge
import com.example.ui.theme.NumericTextStyle
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess
import com.example.util.formatDateShort
import com.example.util.formatRupiah
import com.example.util.formatTimeOnly
import com.example.viewmodel.StoreDataBundle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class DayFinanceSummary(
  val dayOfMonth: Int,
  val salesOmzet: Long,
  val salesProfit: Long,
  val cashIn: Long,
  val cashOut: Long,
  val operationalOut: Long,
  val damagedLoss: Long,
  val netProfitLoss: Long,
  val sales: List<Sale>,
  val cashTxs: List<CashTransaction>,
)

@Composable
fun KalenderTab(bundle: StoreDataBundle) {
  val initialCal = remember { Calendar.getInstance() }
  var monthOffset by remember { mutableIntStateOf(0) }
  var selectedDay by remember { mutableIntStateOf(initialCal.get(Calendar.DAY_OF_MONTH)) }

  val monthCal =
    remember(monthOffset) {
      Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.MONTH, monthOffset)
      }
    }

  val targetYear = monthCal.get(Calendar.YEAR)
  val targetMonth = monthCal.get(Calendar.MONTH)
  val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
  val effectiveSelectedDay = selectedDay.coerceIn(1, daysInMonth)

  // Monday-first offset (0 = Senin ... 6 = Minggu)
  val firstDayOfWeek = monthCal.get(Calendar.DAY_OF_WEEK)
  val leadingEmpty =
    when (firstDayOfWeek) {
      Calendar.MONDAY -> 0
      Calendar.TUESDAY -> 1
      Calendar.WEDNESDAY -> 2
      Calendar.THURSDAY -> 3
      Calendar.FRIDAY -> 4
      Calendar.SATURDAY -> 5
      else -> 6
    }

  val monthTitle =
    remember(monthOffset) {
      val sdf = SimpleDateFormat("MMMM yyyy", Locale.Builder().setLanguage("in").setRegion("ID").build())
      sdf.format(monthCal.time).replaceFirstChar { it.uppercase() }
    }

  val dailySummaries =
    remember(bundle, targetYear, targetMonth, daysInMonth) {
      val cal = Calendar.getInstance()
      fun dayKey(millis: Long): Int? {
        cal.timeInMillis = millis
        return if (cal.get(Calendar.YEAR) == targetYear && cal.get(Calendar.MONTH) == targetMonth) {
          cal.get(Calendar.DAY_OF_MONTH)
        } else {
          null
        }
      }

      val salesByDay = bundle.sales.mapNotNull { s -> dayKey(s.occurredAtMillis)?.let { it to s } }.groupBy({ it.first }, { it.second })
      val cashByDay = bundle.cashTransactions.mapNotNull { c -> dayKey(c.occurredAtMillis)?.let { it to c } }.groupBy({ it.first }, { it.second })
      val damagedByDay =
        bundle.activities
          .filter { it.type == ActivityType.BARANG_RUSAK.code }
          .mapNotNull { a -> dayKey(a.occurredAtMillis)?.let { it to a.amount } }
          .groupBy({ it.first }, { it.second })

      (1..daysInMonth).associateWith { day ->
        val daySales = salesByDay[day].orEmpty()
        val dayCash = cashByDay[day].orEmpty()
        val salesOmzet = daySales.sumOf { it.totalAmount }
        val salesProfit = daySales.sumOf { it.totalProfit }
        val cashIn = dayCash.filter { it.direction == "MASUK" }.sumOf { it.amount }
        val cashOut = dayCash.filter { it.direction == "KELUAR" }.sumOf { it.amount }
        val opOut =
          dayCash
            .filter {
              it.direction == "KELUAR" && it.category !in listOf("Belanja Stok", "Pengambilan Uang Pribadi")
            }
            .sumOf { it.amount }
        val damagedLoss = damagedByDay[day].orEmpty().sum()
        val net = salesProfit - opOut - damagedLoss
        DayFinanceSummary(
          dayOfMonth = day,
          salesOmzet = salesOmzet,
          salesProfit = salesProfit,
          cashIn = cashIn,
          cashOut = cashOut,
          operationalOut = opOut,
          damagedLoss = damagedLoss,
          netProfitLoss = net,
          sales = daySales,
          cashTxs = dayCash,
        )
      }
    }

  val monthOmzet = dailySummaries.values.sumOf { it.salesOmzet }
  val monthGrossProfit = dailySummaries.values.sumOf { it.salesProfit }
  val monthOpLoss = dailySummaries.values.sumOf { it.operationalOut + it.damagedLoss }
  val monthNet = monthGrossProfit - monthOpLoss
  val selectedSummary = dailySummaries[effectiveSelectedDay]

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("kalender_tab_list"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconButton(
              onClick = { monthOffset -= 1 },
              modifier = Modifier.size(48.dp),
            ) {
              Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Bulan sebelumnya")
            }
            Text(monthTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(
              onClick = { monthOffset += 1 },
              modifier = Modifier.size(48.dp),
            ) {
              Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Bulan berikutnya")
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
              color = SemanticSuccess.copy(alpha = 0.1f),
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.weight(1f),
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text("Omzet Bulan Ini", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatRupiah(monthOmzet), style = NumericTextStyle.copy(fontSize = 14.sp), color = SemanticSuccess, maxLines = 1, overflow = TextOverflow.Ellipsis)
              }
            }
            Surface(
              color = SemanticError.copy(alpha = 0.1f),
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.weight(1f),
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text("Biaya & Rugi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatRupiah(monthOpLoss), style = NumericTextStyle.copy(fontSize = 14.sp), color = SemanticError, maxLines = 1, overflow = TextOverflow.Ellipsis)
              }
            }
          }

          Surface(
            color = if (monthNet >= 0L) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = if (monthNet >= 0L) "Laba Bersih Bulan Ini" else "Rugi Bersih Bulan Ini",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = formatRupiah(monthNet),
                style = NumericTextStyle.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize),
                color = if (monthNet >= 0L) SemanticSuccess else SemanticError,
              )
            }
          }
        }
      }
    }

    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(SemanticSuccess))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Untung", style = MaterialTheme.typography.labelSmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(SemanticError))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Rugi", style = MaterialTheme.typography.labelSmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Ada Kas", style = MaterialTheme.typography.labelSmall)
            }
          }

          Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min").forEach { d ->
              Text(
                text = d,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
              )
            }
          }

          val totalCells = leadingEmpty + daysInMonth
          val rowCount = (totalCells + 6) / 7
          for (r in 0 until rowCount) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              for (c in 0 until 7) {
                val cellIndex = r * 7 + c
                val dayNum = cellIndex - leadingEmpty + 1
                if (dayNum < 1 || dayNum > daysInMonth) {
                  Spacer(modifier = Modifier.weight(1f).height(52.dp))
                } else {
                  val summary = dailySummaries[dayNum]
                  val hasCashOnly = summary != null && (summary.cashIn > 0L || summary.cashOut > 0L) && summary.netProfitLoss == 0L
                  val isSelected = dayNum == effectiveSelectedDay
                  val cellBg =
                    when {
                      summary != null && summary.netProfitLoss > 0L -> SemanticSuccess.copy(alpha = 0.14f)
                      summary != null && summary.netProfitLoss < 0L -> SemanticError.copy(alpha = 0.14f)
                      hasCashOnly -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                      else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    }
                  val dotColor =
                    when {
                      summary != null && summary.netProfitLoss > 0L -> SemanticSuccess
                      summary != null && summary.netProfitLoss < 0L -> SemanticError
                      hasCashOnly -> MaterialTheme.colorScheme.primary
                      else -> Color.Transparent
                    }

                  Column(
                    modifier =
                      Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(cellBg)
                        .then(
                          if (isSelected) {
                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                          } else {
                            Modifier
                          }
                        )
                        .clickable { selectedDay = dayNum }
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(
                      text = dayNum.toString(),
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                    if (dotColor != Color.Transparent) {
                      Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(dotColor))
                    } else {
                      Spacer(modifier = Modifier.height(7.dp))
                    }
                    if (summary != null && summary.netProfitLoss != 0L) {
                      Text(
                        text = compactRupiahShort(summary.netProfitLoss),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = if (summary.netProfitLoss > 0) SemanticSuccess else SemanticError,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                      )
                    } else {
                      Spacer(modifier = Modifier.height(10.dp))
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    selectedSummary?.let { dayData ->
      val dayMillis =
        Calendar.getInstance().apply {
          set(targetYear, targetMonth, effectiveSelectedDay, 12, 0, 0)
        }.timeInMillis

      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = MaterialTheme.shapes.large,
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Rincian ${formatDateShort(dayMillis)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              StatusBadge(
                text =
                  when {
                    dayData.netProfitLoss > 0L -> "Untung"
                    dayData.netProfitLoss < 0L -> "Rugi"
                    else -> "Impas / Kosong"
                  },
                containerColor =
                  when {
                    dayData.netProfitLoss > 0L -> SemanticSuccess.copy(alpha = 0.15f)
                    dayData.netProfitLoss < 0L -> SemanticError.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                  },
                contentColor =
                  when {
                    dayData.netProfitLoss > 0L -> SemanticSuccess
                    dayData.netProfitLoss < 0L -> SemanticError
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                  },
              )
            }

            MetricRowLine("Omzet Penjualan (${dayData.sales.size}x)", formatRupiah(dayData.salesOmzet))
            MetricRowLine("Laba Penjualan", formatRupiah(dayData.salesProfit), valueColor = SemanticSuccess)
            MetricRowLine("Uang Masuk (Kas)", formatRupiah(dayData.cashIn), valueColor = MaterialTheme.colorScheme.primary)
            MetricRowLine("Uang Keluar (Kas)", formatRupiah(dayData.cashOut), valueColor = SemanticError)
            if (dayData.damagedLoss > 0L) {
              MetricRowLine("Kerugian Barang Rusak", formatRupiah(dayData.damagedLoss), valueColor = SemanticError)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
            MetricRowLine(
              label = if (dayData.netProfitLoss >= 0L) "Laba Bersih Hari Ini" else "Rugi Bersih Hari Ini",
              valueText = formatRupiah(dayData.netProfitLoss),
              valueColor = if (dayData.netProfitLoss >= 0L) SemanticSuccess else SemanticError,
              bold = true,
            )

            if (dayData.sales.isNotEmpty() || dayData.cashTxs.isNotEmpty()) {
              HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
              Text("Catatan Transaksi Tanggal Ini", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
              dayData.sales.forEach { sale ->
                MetricRowLine(
                  label = "${formatTimeOnly(sale.occurredAtMillis)} • ${sale.receiptNumber}" +
                    if (sale.customerName.isNotBlank()) " (${sale.customerName})" else "",
                  valueText = formatRupiah(sale.totalAmount),
                  valueColor = SemanticSuccess,
                )
              }
              dayData.cashTxs
                .filter { it.category != "Penjualan" }
                .forEach { tx ->
                  val isIn = tx.direction == "MASUK"
                  MetricRowLine(
                    label = "${formatTimeOnly(tx.occurredAtMillis)} • ${tx.category}",
                    valueText = "${if (isIn) "+" else "-"}${formatRupiah(tx.amount)}",
                    valueColor = if (isIn) SemanticSuccess else SemanticError,
                  )
                }
            }
          }
        }
      }
    }
  }
}

private fun compactRupiahShort(amount: Long): String {
  val sign = if (amount < 0) "-" else "+"
  val absVal = kotlin.math.abs(amount)
  return when {
    absVal >= 1_000_000 -> "${sign}${absVal / 1_000_000}jt"
    absVal >= 1_000 -> "${sign}${absVal / 1_000}rb"
    else -> "${sign}${absVal}"
  }
}

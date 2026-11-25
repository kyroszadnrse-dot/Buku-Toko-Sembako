package com.example.ui.activity

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ActivityEntry
import com.example.model.ActivityType
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SearchInputBar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.NumericTextStyle
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticWarning
import com.example.util.formatDateTime
import com.example.util.formatDayShort
import com.example.util.formatPercent
import com.example.util.formatRupiah
import com.example.util.startOfDaysAgoMillis
import com.example.util.startOfMonthMillis
import com.example.util.startOfTodayMillis

@Composable
fun AktivitasScreen(
  activities: List<ActivityEntry>,
  onDeleteActivity: (ActivityEntry) -> Unit,
) {
  var searchQuery by remember { mutableStateOf("") }
  var dateFilter by remember { mutableIntStateOf(0) } // 0=Semua, 1=Hari Ini, 2=7 Hari, 3=Bulan Ini
  var typeFilter by remember { mutableStateOf<String?>(null) }
  var showDiagram by remember { mutableStateOf(true) }
  var deleteTarget by remember { mutableStateOf<ActivityEntry?>(null) }

  val minTimestamp =
    when (dateFilter) {
      1 -> startOfTodayMillis()
      2 -> startOfDaysAgoMillis(6)
      3 -> startOfMonthMillis()
      else -> 0L
    }

  val filtered =
    activities.filter { act ->
      val matchDate = act.occurredAtMillis >= minTimestamp
      val matchType = typeFilter == null || act.type == typeFilter
      val matchQuery =
        searchQuery.isBlank() ||
          act.title.contains(searchQuery, ignoreCase = true) ||
          act.subtitle.contains(searchQuery, ignoreCase = true)
      matchDate && matchType && matchQuery
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    item {
      SearchInputBar(
        query = searchQuery,
        onQueryChange = { searchQuery = it },
        placeholder = "Cari riwayat aktivitas...",
      )
      Spacer(modifier = Modifier.height(10.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
          FilterChip(
            selected = showDiagram,
            onClick = { showDiagram = !showDiagram },
            leadingIcon = { Icon(Icons.Outlined.BarChart, contentDescription = null, modifier = Modifier.size(16.dp)) },
            label = { Text("Diagram") },
            modifier = Modifier.testTag("toggle_activity_diagram"),
          )
        }
        val dateLabels = listOf(0 to "Semua Tanggal", 1 to "Hari Ini", 2 to "7 Hari", 3 to "Bulan Ini")
        items(dateLabels) { (idx, label) ->
          FilterChip(
            selected = dateFilter == idx,
            onClick = { dateFilter = idx },
            label = { Text(label) },
          )
        }
      }
      Spacer(modifier = Modifier.height(4.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
          FilterChip(
            selected = typeFilter == null,
            onClick = { typeFilter = null },
            label = { Text("Semua Kategori") },
          )
        }
        items(ActivityType.entries) { type ->
          FilterChip(
            selected = typeFilter == type.code,
            onClick = { typeFilter = if (typeFilter == type.code) null else type.code },
            label = { Text(type.label) },
          )
        }
      }
    }

    if (showDiagram) {
      item {
        ActivityDiagramCard(activities = filtered)
      }
    }

    if (filtered.isEmpty()) {
      item {
        EmptyStateCard(
          title = "Belum Ada Aktivitas",
          description = "Semua penjualan, belanja stok, arus kas, dan perubahan stok akan tercatat otomatis di sini.",
        )
      }
    } else {
      items(filtered, key = { it.id }) { entry ->
        ActivityCard(
          entry = entry,
          onDeleteClick = { deleteTarget = entry },
        )
      }
    }
  }

  deleteTarget?.let { target ->
    AlertDialog(
      onDismissRequest = { deleteTarget = null },
      title = { Text("Hapus Catatan Aktivitas?", fontWeight = FontWeight.Bold) },
      text = {
        Text("Hapus aktivitas \"${target.title}\" dari riwayat? Tindakan ini tidak mengubah stok atau saldo kas yang sudah tercatat.")
      },
      confirmButton = {
        Button(
          colors = ButtonDefaults.buttonColors(containerColor = SemanticError),
          onClick = {
            onDeleteActivity(target)
            deleteTarget = null
          },
          modifier = Modifier.testTag("confirm_delete_activity_button"),
        ) {
          Text("Hapus")
        }
      },
      dismissButton = {
        TextButton(onClick = { deleteTarget = null }) { Text("Batal") }
      },
    )
  }
}

@Composable
private fun ActivityDiagramCard(activities: List<ActivityEntry>) {
  val dayBars =
    (6 downTo 0).map { daysAgo ->
      val dayStart = startOfDaysAgoMillis(daysAgo)
      val dayEnd = if (daysAgo == 0) Long.MAX_VALUE else startOfDaysAgoMillis(daysAgo - 1)
      val count = activities.count { it.occurredAtMillis in dayStart until dayEnd }
      formatDayShort(dayStart).take(3) to count
    }
  val maxDayCount = dayBars.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1

  val totalCount = activities.size
  val categoryCounts =
    activities
      .groupBy { ActivityType.fromCode(it.type) }
      .map { (type, list) -> type to list.size }
      .sortedByDescending { it.second }
      .take(5)

  val barColor = MaterialTheme.colorScheme.primary

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = MaterialTheme.shapes.large,
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth().testTag("activity_diagram_card"),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Diagram Aktivitas Toko", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        StatusBadge(
          text = "$totalCount aktivitas",
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }

      if (activities.isEmpty()) {
        Text(
          text = "Diagram aktivitas 7 hari & distribusi kategori akan muncul setelah ada aktivitas tercatat.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        Text(
          text = "Frekuensi 7 Hari Terakhir",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(modifier = Modifier.fillMaxWidth()) {
          dayBars.forEach { (_, count) ->
            Text(
              text = count.toString(),
              style = NumericTextStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
              color = if (count > 0) barColor else MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
              modifier = Modifier.weight(1f),
            )
          }
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(92.dp)) {
          val count = dayBars.size.coerceAtLeast(1)
          val slotWidth = size.width / count
          val barWidth = (slotWidth * 0.48f).coerceAtMost(32f)
          val bottomY = size.height - 6f
          val maxH = bottomY - 4f

          drawLine(
            color = Color.LightGray.copy(alpha = 0.4f),
            start = Offset(0f, bottomY),
            end = Offset(size.width, bottomY),
            strokeWidth = 2f,
          )

          dayBars.forEachIndexed { idx, (_, value) ->
            val x = slotWidth * idx + (slotWidth - barWidth) / 2f
            val h = if (value > 0) ((value.toFloat() / maxDayCount.toFloat()) * maxH).coerceAtLeast(8f) else 4f
            drawRoundRect(
              color = if (value > 0) barColor else barColor.copy(alpha = 0.18f),
              topLeft = Offset(x, bottomY - h),
              size = Size(barWidth, h),
              cornerRadius = CornerRadius(8f, 8f),
            )
          }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
          dayBars.forEach { (label, _) ->
            Text(
              text = label,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
              modifier = Modifier.weight(1f),
            )
          }
        }

        if (categoryCounts.isNotEmpty()) {
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Distribusi Jenis Aktivitas",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          categoryCounts.forEach { (type, count) ->
            val pct = (count.toFloat() / totalCount.coerceAtLeast(1).toFloat())
            val color = activityAccentColor(type)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(type.label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                Text(
                  text = "$count (${formatPercent(pct * 100.0)})",
                  style = NumericTextStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                  color = color,
                )
              }
              LinearProgressIndicator(
                progress = { pct.coerceIn(0f, 1f) },
                color = color,
                trackColor = color.copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun activityAccentColor(actType: ActivityType): Color =
  when (actType) {
    ActivityType.PENJUALAN, ActivityType.PEMASUKAN, ActivityType.TAMBAHAN_MODAL -> SemanticSuccess
    ActivityType.PEMBELIAN_STOK, ActivityType.BARANG_MASUK -> MaterialTheme.colorScheme.primary
    ActivityType.BARANG_KELUAR, ActivityType.PENGURANGAN_STOK -> SemanticWarning
    ActivityType.PENGELUARAN, ActivityType.BARANG_RUSAK -> SemanticError
    ActivityType.PEMBAYARAN_UTANG -> MaterialTheme.colorScheme.tertiary
  }

@Composable
private fun ActivityCard(
  entry: ActivityEntry,
  onDeleteClick: () -> Unit,
) {
  val actType = ActivityType.fromCode(entry.type)
  val accent = activityAccentColor(actType)
  val icon =
    when (actType) {
      ActivityType.PENJUALAN -> Icons.Outlined.PointOfSale
      ActivityType.PEMBELIAN_STOK -> Icons.Outlined.AddShoppingCart
      ActivityType.BARANG_MASUK, ActivityType.BARANG_KELUAR, ActivityType.PENGURANGAN_STOK -> Icons.Outlined.Inventory2
      ActivityType.PENGELUARAN -> Icons.AutoMirrored.Rounded.TrendingDown
      ActivityType.PEMASUKAN, ActivityType.TAMBAHAN_MODAL -> Icons.AutoMirrored.Rounded.TrendingUp
      ActivityType.PEMBAYARAN_UTANG -> Icons.Outlined.Payments
      ActivityType.BARANG_RUSAK -> Icons.Outlined.BrokenImage
    }

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = MaterialTheme.shapes.medium,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(icon, contentDescription = actType.label, tint = accent, modifier = Modifier.size(20.dp))
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = entry.title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (entry.subtitle.isNotBlank()) {
          Text(
            text = entry.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          StatusBadge(
            text = actType.label,
            containerColor = accent.copy(alpha = 0.12f),
            contentColor = accent,
          )
          Text(
            text = formatDateTime(entry.occurredAtMillis),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      if (entry.amount > 0L) {
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = formatRupiah(entry.amount),
          style = NumericTextStyle.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize),
          color = accent,
        )
      }
      IconButton(
        onClick = onDeleteClick,
        modifier = Modifier.size(48.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.DeleteOutline,
          contentDescription = "Hapus aktivitas ${entry.title}",
          tint = SemanticError,
          modifier = Modifier.size(20.dp),
        )
      }
    }
  }
}

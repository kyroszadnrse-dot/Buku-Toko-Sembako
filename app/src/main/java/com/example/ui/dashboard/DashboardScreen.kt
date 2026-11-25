package com.example.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.Product
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.StatusBadge
import com.example.ui.theme.NumericTextStyle
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticWarning
import com.example.util.formatNumber
import com.example.util.formatRupiah
import com.example.viewmodel.DailyChartPoint
import com.example.viewmodel.DashboardSnapshot
import com.example.viewmodel.FinancialHealthReport
import com.example.viewmodel.HealthLevel

@Composable
fun DashboardScreen(
  snapshot: DashboardSnapshot,
  onOpenKasir: () -> Unit,
  onOpenBelanjaStok: () -> Unit,
  onOpenBarang: () -> Unit,
  onOpenKeuangan: (Int) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      HeroBalanceCard(snapshot = snapshot)
    }

    item {
      QuickActionsRow(
        onOpenKasir = onOpenKasir,
        onOpenBelanjaStok = onOpenBelanjaStok,
        onOpenArusKas = { onOpenKeuangan(0) },
        onOpenLabaRugi = { onOpenKeuangan(3) },
        onOpenKalender = { onOpenKeuangan(4) },
        onOpenLaporan = { onOpenKeuangan(5) },
      )
    }

    item {
      CustomerShoppersCard(snapshot = snapshot, onClick = onOpenKasir)
    }

    item {
      DashboardHealthAdviceCard(
        report = snapshot.financialHealth,
        onClick = { onOpenKeuangan(3) },
      )
    }

    item {
      Text(
        text = "Ringkasan Bisnis",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(10.dp))
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          MetricTile(
            title = "Penjualan Hari Ini",
            value = formatRupiah(snapshot.todaySalesTotal),
            icon = Icons.Outlined.PointOfSale,
            accent = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
            onClick = onOpenKasir,
          )
          MetricTile(
            title = "Keuntungan Hari Ini",
            value = formatRupiah(snapshot.todayProfit),
            icon = Icons.AutoMirrored.Rounded.TrendingUp,
            accent = SemanticSuccess,
            modifier = Modifier.weight(1f),
            onClick = { onOpenKeuangan(3) },
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          MetricTile(
            title = "Keuntungan Bulan Ini",
            value = formatRupiah(snapshot.monthProfit),
            icon = Icons.Outlined.Assessment,
            accent = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
            onClick = { onOpenKeuangan(3) },
          )
          MetricTile(
            title = "Stok Barang (${snapshot.totalProductCount} jenis)",
            value = "${formatNumber(snapshot.totalStockUnits)} unit",
            icon = Icons.Outlined.Inventory2,
            accent = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
            onClick = onOpenBarang,
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          MetricTile(
            title = "Total Pemasukan",
            value = formatRupiah(snapshot.totalIncome),
            icon = Icons.AutoMirrored.Rounded.TrendingUp,
            accent = SemanticSuccess,
            modifier = Modifier.weight(1f),
            onClick = { onOpenKeuangan(0) },
          )
          MetricTile(
            title = "Total Pengeluaran",
            value = formatRupiah(snapshot.totalExpense),
            icon = Icons.AutoMirrored.Rounded.TrendingDown,
            accent = SemanticError,
            modifier = Modifier.weight(1f),
            onClick = { onOpenKeuangan(0) },
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          MetricTile(
            title = "Utang Pelanggan",
            value = formatRupiah(snapshot.totalCustomerDebt),
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            accent = SemanticWarning,
            modifier = Modifier.weight(1f),
            onClick = { onOpenKeuangan(1) },
          )
          MetricTile(
            title = "Utang Supplier",
            value = formatRupiah(snapshot.totalSupplierDebt),
            icon = Icons.Outlined.Storefront,
            accent = SemanticError,
            modifier = Modifier.weight(1f),
            onClick = { onOpenKeuangan(2) },
          )
        }
      }
    }

    item {
      WeeklyCashProfitChartCard(points = snapshot.chartPoints)
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = if (snapshot.lowStockProducts.isEmpty()) MaterialTheme.colorScheme.primary else SemanticWarning,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Barang Hampir Habis (${snapshot.lowStockProducts.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
        Text(
          text = "Lihat Semua",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onOpenBarang).padding(6.dp),
        )
      }
    }

    if (snapshot.totalProductCount == 0) {
      item {
        EmptyStateCard(
          title = "Belum Ada Barang di Toko",
          description = "Mulai tambahkan barang dagangan Anda seperti beras, minyak goreng, gula, dan telur agar kasir dan stok siap dipakai.",
          actionLabel = "Tambah Barang Pertama",
          onAction = onOpenBarang,
        )
      }
    } else if (snapshot.lowStockProducts.isEmpty()) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            StatusBadge(
              text = "Aman",
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "Semua stok barang masih di atas batas minimum.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      items(snapshot.lowStockProducts.take(6), key = { it.id }) { product ->
        LowStockRowCard(product = product, onRestock = onOpenBelanjaStok)
      }
    }

    item { Spacer(modifier = Modifier.height(24.dp)) }
  }
}

@Composable
private fun CustomerShoppersCard(snapshot: DashboardSnapshot, onClick: () -> Unit) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = MaterialTheme.shapes.large,
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).clickable(onClick = onClick).testTag("customer_shoppers_card"),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Outlined.Groups,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Pelanggan Sudah Belanja",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
        Text(
          text = "Kasir →",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.weight(1f),
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text("Hari Ini", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
              text = "${snapshot.todayCustomerCount} org",
              style = NumericTextStyle.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize),
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.weight(1f),
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text("Total Belanja", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
              text = "${snapshot.totalSalesCount}x",
              style = NumericTextStyle.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize),
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.weight(1f),
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text("Pelanggan Nama", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
              text = "${snapshot.uniqueNamedCustomers}",
              style = NumericTextStyle.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize),
              color = SemanticSuccess,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DashboardHealthAdviceCard(
  report: FinancialHealthReport,
  onClick: () -> Unit,
) {
  val accent =
    when (report.level) {
      HealthLevel.SEHAT -> SemanticSuccess
      HealthLevel.CUKUP -> MaterialTheme.colorScheme.primary
      HealthLevel.PERLU_PERHATIAN -> SemanticWarning
      HealthLevel.WASPADA -> SemanticError
    }

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = MaterialTheme.shapes.large,
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).clickable(onClick = onClick).testTag("dashboard_health_card"),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(modifier = Modifier.size(54.dp), contentAlignment = Alignment.Center) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 6.dp.toPx()
            drawArc(
              color = accent.copy(alpha = 0.16f),
              startAngle = -90f,
              sweepAngle = 360f,
              useCenter = false,
              style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
              color = accent,
              startAngle = -90f,
              sweepAngle = (report.score.coerceIn(0, 100) / 100f) * 360f,
              useCenter = false,
              style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
          }
          Text(
            text = report.score.toString(),
            style = NumericTextStyle.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize),
            color = accent,
          )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text("Kesehatan Keuangan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          }
          StatusBadge(
            text = "${report.statusLabel} (${report.score}/100)",
            containerColor = accent.copy(alpha = 0.14f),
            contentColor = accent,
          )
        }
        Text("Detail →", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
      }

      report.adviceList.take(2).forEach { advice ->
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(
                if (advice.priorityHigh) SemanticError.copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
              )
              .padding(10.dp),
          verticalAlignment = Alignment.Top,
        ) {
          Icon(
            imageVector = Icons.Outlined.Lightbulb,
            contentDescription = null,
            tint = if (advice.priorityHigh) SemanticError else SemanticWarning,
            modifier = Modifier.size(16.dp).padding(top = 1.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = advice.title,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = if (advice.priorityHigh) SemanticError else MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = advice.detail,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun HeroBalanceCard(snapshot: DashboardSnapshot) {
  Card(
    shape = MaterialTheme.shapes.large,
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(
            Brush.linearGradient(
              listOf(
                MaterialTheme.colorScheme.primary,
                Color(0xFF0D5333),
              )
            )
          )
          .padding(20.dp),
    ) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Outlined.AccountBalanceWallet,
              contentDescription = null,
              tint = Color.White.copy(alpha = 0.9f),
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Saldo Uang Tersedia",
              style = MaterialTheme.typography.labelLarge,
              color = Color.White.copy(alpha = 0.9f),
            )
          }
          Surface(
            color = Color.White.copy(alpha = 0.16f),
            shape = RoundedCornerShape(50),
          ) {
            Text(
              text = "${snapshot.lowStockProducts.size} Stok Menipis",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = formatRupiah(snapshot.availableBalance),
          style = NumericTextStyle.copy(fontSize = MaterialTheme.typography.headlineLarge.fontSize),
          color = Color.White,
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Column(
            modifier =
              Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .padding(10.dp),
          ) {
            Text(
              text = "Omzet Hari Ini",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White.copy(alpha = 0.8f),
            )
            Text(
              text = formatRupiah(snapshot.todaySalesTotal),
              style = NumericTextStyle.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize),
              color = Color.White,
            )
          }
          Column(
            modifier =
              Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .padding(10.dp),
          ) {
            Text(
              text = "Laba Hari Ini",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White.copy(alpha = 0.8f),
            )
            Text(
              text = formatRupiah(snapshot.todayProfit),
              style = NumericTextStyle.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize),
              color = Color(0xFFA7F3D0),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun QuickActionsRow(
  onOpenKasir: () -> Unit,
  onOpenBelanjaStok: () -> Unit,
  onOpenArusKas: () -> Unit,
  onOpenLabaRugi: () -> Unit,
  onOpenKalender: () -> Unit,
  onOpenLaporan: () -> Unit,
) {
  data class QuickAction(
    val label: String,
    val icon: ImageVector,
    val tag: String,
    val onClick: () -> Unit,
  )

  val actions =
    listOf(
      QuickAction("Kasir Cepat", Icons.Outlined.PointOfSale, "quick_kasir", onOpenKasir),
      QuickAction("Belanja Stok", Icons.Outlined.AddShoppingCart, "quick_belanja", onOpenBelanjaStok),
      QuickAction("Catat Kas", Icons.Outlined.AccountBalanceWallet, "quick_kas", onOpenArusKas),
      QuickAction("Laba Rugi", Icons.AutoMirrored.Rounded.TrendingUp, "quick_laba", onOpenLabaRugi),
      QuickAction("Kalender", Icons.Outlined.CalendarMonth, "quick_kalender", onOpenKalender),
      QuickAction("Laporan", Icons.Outlined.Assessment, "quick_laporan", onOpenLaporan),
    )

  LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    items(actions) { action ->
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        modifier =
          Modifier
            .width(104.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = action.onClick)
            .testTag(action.tag),
      ) {
        Column(
          modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Box(
            modifier =
              Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = action.icon,
              contentDescription = action.label,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = action.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
private fun MetricTile(
  title: String,
  value: String,
  icon: ImageVector,
  accent: Color,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = MaterialTheme.shapes.medium,
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
          modifier =
            Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(accent.copy(alpha = 0.12f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(16.dp),
          )
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = value,
        style = NumericTextStyle.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun WeeklyCashProfitChartCard(points: List<DailyChartPoint>) {
  val maxVal =
    points
      .flatMap { listOf(it.income, it.expense, it.profit.coerceAtLeast(0L)) }
      .maxOrNull()
      ?.coerceAtLeast(1L) ?: 1L

  val incomeColor = MaterialTheme.colorScheme.primary
  val expenseColor = SemanticError
  val profitColor = SemanticWarning

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = MaterialTheme.shapes.large,
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = "Perkembangan 7 Hari Terakhir",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        LegendDot(color = incomeColor, label = "Pemasukan")
        LegendDot(color = expenseColor, label = "Pengeluaran")
        LegendDot(color = profitColor, label = "Keuntungan")
      }
      Spacer(modifier = Modifier.height(16.dp))

      if (points.all { it.income == 0L && it.expense == 0L && it.profit == 0L }) {
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .height(120.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "Grafik akan muncul otomatis setelah ada transaksi penjualan atau kas.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
          )
        }
      } else {
        Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
          val groupCount = points.size.coerceAtLeast(1)
          val groupWidth = size.width / groupCount
          val barWidth = (groupWidth * 0.22f).coerceAtMost(18f)
          val gap = 4f
          val chartBottom = size.height - 8f
          val maxBarHeight = chartBottom - 8f

          drawLine(
            color = Color.LightGray.copy(alpha = 0.45f),
            start = Offset(0f, chartBottom),
            end = Offset(size.width, chartBottom),
            strokeWidth = 2f,
          )

          points.forEachIndexed { idx, pt ->
            val centerX = groupWidth * idx + groupWidth / 2f
            val vals =
              listOf(
                pt.income.coerceAtLeast(0L) to incomeColor,
                pt.expense.coerceAtLeast(0L) to expenseColor,
                pt.profit.coerceAtLeast(0L) to profitColor,
              )
            val totalCluster = barWidth * 3 + gap * 2
            val startX = centerX - totalCluster / 2f
            vals.forEachIndexed { bIdx, (v, col) ->
              val h = if (v > 0) ((v.toFloat() / maxVal.toFloat()) * maxBarHeight).coerceAtLeast(6f) else 3f
              val x = startX + bIdx * (barWidth + gap)
              drawRoundRect(
                color = col,
                topLeft = Offset(x, chartBottom - h),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(6f, 6f),
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
          points.forEach { pt ->
            Text(
              text = pt.label,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.weight(1f),
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun LegendDot(color: Color, label: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
    Spacer(modifier = Modifier.width(5.dp))
    Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun LowStockRowCard(product: Product, onRestock: () -> Unit) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = MaterialTheme.shapes.medium,
    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onRestock),
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ProductThumbnail(
        photoPath = product.photoPath,
        category = product.category,
        contentDescription = product.name,
        size = 46.dp,
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = product.name,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "Batas minimum: ${product.minStock} ${product.unit}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      StatusBadge(
        text = "Sisa ${product.stock} ${product.unit}",
        containerColor = if (product.stock == 0L) MaterialTheme.colorScheme.errorContainer else Color(0xFFFEF3C7),
        contentColor = if (product.stock == 0L) MaterialTheme.colorScheme.error else Color(0xFF92400E),
      )
    }
  }
}

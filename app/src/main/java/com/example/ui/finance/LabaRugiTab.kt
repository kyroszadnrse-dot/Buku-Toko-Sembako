package com.example.ui.finance

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MetricRowLine
import com.example.ui.components.StatusBadge
import com.example.ui.theme.NumericTextStyle
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticWarning
import com.example.util.formatPercent
import com.example.util.formatRupiah
import com.example.viewmodel.FinancialHealthReport
import com.example.viewmodel.HealthLevel
import com.example.viewmodel.ProfitLossSummary

@Composable
fun LabaRugiTab(
  summary: ProfitLossSummary,
  financialHealth: FinancialHealthReport,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      FinancialHealthCard(report = financialHealth)
    }

    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Perhitungan Keuntungan & Kerugian", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(4.dp))
          MetricRowLine("Total Omzet Penjualan", formatRupiah(summary.totalOmzet))
          MetricRowLine("Modal Barang Terjual (HPP)", "-${formatRupiah(summary.costOfGoodsSold)}")
          HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
          MetricRowLine(
            label = "Keuntungan Kotor",
            valueText = formatRupiah(summary.grossProfit),
            valueColor = MaterialTheme.colorScheme.primary,
            bold = true,
          )
          MetricRowLine("Pengeluaran Operasional", "-${formatRupiah(summary.operationalExpenses)}", valueColor = SemanticError)
          MetricRowLine("Kerugian Barang Rusak", "-${formatRupiah(summary.damagedGoodsLoss)}", valueColor = SemanticError)
          HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
          MetricRowLine(
            label = "Keuntungan Bersih",
            valueText = formatRupiah(summary.netProfit),
            valueColor = if (summary.netProfit >= 0) SemanticSuccess else SemanticError,
            bold = true,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.weight(1f),
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text("Margin Keuntungan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatPercent(summary.marginPercent), style = NumericTextStyle, color = MaterialTheme.colorScheme.primary)
                Text("Laba ÷ Harga Jual", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
            Surface(
              color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.weight(1f),
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text("Markup Keuntungan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatPercent(summary.markupPercent), style = NumericTextStyle, color = SemanticWarning)
                Text("Laba ÷ Harga Modal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }
    }

    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              "Keuntungan vs Uang Tersedia",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF065F46),
            )
          }
          Text(
            text = "Keuntungan bersih (${formatRupiah(summary.netProfit)}) dihitung dari selisih harga jual & modal dikurangi biaya. " +
              "Saldo uang tersedia saat ini (${formatRupiah(summary.availableBalance)}) dapat berbeda karena masih ada piutang pelanggan " +
              "(${formatRupiah(summary.unpaidReceivables)}) atau uang dipakai untuk belanja stok baru.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF064E3B),
          )
        }
      }
    }

    item {
      Text(
        text = "Produk Paling Menguntungkan",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    }

    if (summary.topProfitableProducts.isEmpty()) {
      item {
        EmptyStateCard(
          title = "Belum Ada Data Penjualan",
          description = "Produk paling menguntungkan akan muncul otomatis setelah ada penjualan di kasir.",
        )
      }
    } else {
      itemsIndexed(summary.topProfitableProducts) { idx, rank ->
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "#${idx + 1}",
              style = NumericTextStyle.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize),
              color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(rank.productName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              Text(
                text = "Terjual ${rank.qtySold} • Omzet ${formatRupiah(rank.revenue)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Text(
              text = formatRupiah(rank.profit),
              style = NumericTextStyle.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize),
              color = SemanticSuccess,
            )
          }
        }
      }
    }
  }
}

@Composable
fun FinancialHealthCard(report: FinancialHealthReport) {
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
    modifier = Modifier.fillMaxWidth().testTag("financial_health_card"),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Box(
          modifier = Modifier.size(72.dp),
          contentAlignment = Alignment.Center,
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
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
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = report.score.toString(),
              style = NumericTextStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize),
              color = accent,
            )
            Text(
              text = "/100",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("Kesehatan Keuangan Toko", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          StatusBadge(
            text = report.statusLabel,
            containerColor = accent.copy(alpha = 0.15f),
            contentColor = accent,
          )
        }
      }

      if (report.indicators.isNotEmpty()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          report.indicators.forEach { ind ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
              ) {
                Icon(
                  imageVector = if (ind.isHealthy) Icons.Outlined.CheckCircleOutline else Icons.Outlined.WarningAmber,
                  contentDescription = null,
                  tint = if (ind.isHealthy) SemanticSuccess else SemanticWarning,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(ind.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Text(
                text = ind.valueText,
                style = NumericTextStyle.copy(fontSize = MaterialTheme.typography.labelMedium.fontSize),
                color = if (ind.isHealthy) MaterialTheme.colorScheme.onSurface else SemanticWarning,
              )
            }
          }
        }
      }

      if (report.adviceList.isNotEmpty()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.Lightbulb,
            contentDescription = null,
            tint = SemanticWarning,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Saran Keuangan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        report.adviceList.forEach { item ->
          val boxBg =
            if (item.priorityHigh) SemanticError.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
          val titleCol = if (item.priorityHigh) SemanticError else MaterialTheme.colorScheme.primary
          Column(
            modifier =
              Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(boxBg)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            Text(item.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = titleCol)
            Text(item.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
          }
        }
      }
    }
  }
}

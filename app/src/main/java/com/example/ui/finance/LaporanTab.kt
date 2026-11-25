package com.example.ui.finance

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.MetricRowLine
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticWarning
import com.example.util.formatDateShort
import com.example.util.formatPercent
import com.example.util.formatRupiah
import com.example.util.shareCsvFile
import com.example.util.sharePlainText
import com.example.util.startOfDaysAgoMillis
import com.example.util.startOfMonthMillis
import com.example.util.startOfTodayMillis
import com.example.util.startOfYearMillis
import com.example.viewmodel.StoreDataBundle

@Composable
fun LaporanTab(bundle: StoreDataBundle) {
  val context = LocalContext.current
  var periodIdx by remember { mutableIntStateOf(0) } // 0=Harian, 1=Mingguan, 2=Bulanan, 3=Tahunan
  var reportIdx by remember { mutableIntStateOf(0) } // 0=Penjualan, 1=Keuntungan, 2=Pengeluaran, 3=Stok, 4=Utang Pelanggan, 5=Utang Supplier

  val periodLabels = listOf("Harian", "Mingguan", "Bulanan", "Tahunan")
  val reportTypes =
    listOf(
      "Penjualan",
      "Keuntungan",
      "Pengeluaran",
      "Stok",
      "Utang Pelanggan",
      "Utang Supplier",
    )

  val startMillis =
    when (periodIdx) {
      0 -> startOfTodayMillis()
      1 -> startOfDaysAgoMillis(6)
      2 -> startOfMonthMillis()
      else -> startOfYearMillis()
    }

  val periodSales = bundle.sales.filter { it.occurredAtMillis >= startMillis }
  val periodCashOut = bundle.cashTransactions.filter { it.direction == "KELUAR" && it.occurredAtMillis >= startMillis }
  val periodCashIn = bundle.cashTransactions.filter { it.direction == "MASUK" && it.occurredAtMillis >= startMillis }

  val reportTitle = "Laporan ${reportTypes[reportIdx]} (${periodLabels[periodIdx]})"

  val (shareSummary, csvContent) =
    remember(bundle, periodIdx, reportIdx) {
      buildReportOutputs(
        title = reportTitle,
        periodLabel = periodLabels[periodIdx],
        reportIdx = reportIdx,
        startMillis = startMillis,
        bundle = bundle,
      )
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Text("Periode Laporan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.height(6.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(periodLabels.indices.toList()) { idx ->
          FilterChip(
            selected = periodIdx == idx,
            onClick = { periodIdx = idx },
            label = { Text(periodLabels[idx]) },
          )
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
      Text("Jenis Laporan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.height(6.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(reportTypes.indices.toList()) { idx ->
          FilterChip(
            selected = reportIdx == idx,
            onClick = { reportIdx = idx },
            label = { Text(reportTypes[idx]) },
          )
        }
      }
    }

    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(reportTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Text(
            text = "Dicetak ${formatDateShort(System.currentTimeMillis())}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

          when (reportIdx) {
            0 -> {
              val omzet = periodSales.sumOf { it.totalAmount }
              val cash = periodSales.sumOf { minOf(it.paidAmount, it.totalAmount) }
              MetricRowLine("Jumlah Transaksi", "${periodSales.size} struk")
              MetricRowLine("Total Penjualan", formatRupiah(omzet), bold = true)
              MetricRowLine("Uang Diterima", formatRupiah(cash), valueColor = SemanticSuccess)
              HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
              periodSales.take(10).forEach { s ->
                MetricRowLine("${s.receiptNumber} (${s.items.size} item)", formatRupiah(s.totalAmount))
              }
            }
            1 -> {
              val omzet = periodSales.sumOf { it.totalAmount }
              val cogs = periodSales.sumOf { it.totalCost }
              val gross = omzet - cogs
              val ops = periodCashOut.filter { it.category !in listOf("Belanja Stok", "Pengambilan Uang Pribadi") }.sumOf { it.amount }
              val net = gross - ops
              val margin = if (omzet > 0) (gross.toDouble() / omzet.toDouble()) * 100.0 else 0.0
              MetricRowLine("Total Omzet", formatRupiah(omzet))
              MetricRowLine("Modal Terjual", formatRupiah(cogs))
              MetricRowLine("Keuntungan Kotor (${formatPercent(margin)})", formatRupiah(gross), valueColor = MaterialTheme.colorScheme.primary)
              MetricRowLine("Biaya Operasional", "-${formatRupiah(ops)}", valueColor = SemanticError)
              MetricRowLine("Keuntungan Bersih", formatRupiah(net), valueColor = if (net >= 0) SemanticSuccess else SemanticError, bold = true)
            }
            2 -> {
              val totalOut = periodCashOut.sumOf { it.amount }
              val totalIn = periodCashIn.sumOf { it.amount }
              MetricRowLine("Total Pemasukan", formatRupiah(totalIn), valueColor = SemanticSuccess)
              MetricRowLine("Total Pengeluaran", formatRupiah(totalOut), valueColor = SemanticError, bold = true)
              HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
              periodCashOut.groupBy { it.category }.forEach { (cat, list) ->
                MetricRowLine("$cat (${list.size}x)", formatRupiah(list.sumOf { it.amount }))
              }
            }
            3 -> {
              val totalUnit = bundle.products.sumOf { it.stock }
              val assetValue = bundle.products.sumOf { it.stock * it.costPrice }
              MetricRowLine("Total Jenis Barang", "${bundle.products.size} produk")
              MetricRowLine("Total Unit Stok", "$totalUnit unit")
              MetricRowLine("Nilai Modal Stok", formatRupiah(assetValue), bold = true)
              HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
              bundle.products.take(15).forEach { p ->
                MetricRowLine(
                  "${p.name} (${p.category})",
                  "${p.stock} ${p.unit}",
                  valueColor = if (p.lowStockWarning) SemanticWarning else MaterialTheme.colorScheme.onSurface,
                )
              }
            }
            4 -> {
              val unpaid = bundle.customerDebts.filter { it.status == "BELUM_LUNAS" }
              MetricRowLine("Pelanggan Belum Lunas", "${unpaid.size} orang")
              MetricRowLine("Total Sisa Piutang", formatRupiah(unpaid.sumOf { it.remainingAmount }), valueColor = SemanticWarning, bold = true)
              HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
              unpaid.take(15).forEach { d ->
                MetricRowLine(d.customerName, formatRupiah(d.remainingAmount))
              }
            }
            else -> {
              val unpaid = bundle.supplierDebts.filter { it.status == "BELUM_LUNAS" }
              MetricRowLine("Tagihan Belum Lunas", "${unpaid.size} faktur")
              MetricRowLine("Total Utang Supplier", formatRupiah(unpaid.sumOf { it.remainingAmount }), valueColor = SemanticError, bold = true)
              HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
              unpaid.take(15).forEach { d ->
                MetricRowLine("${d.supplierName} (${d.itemDescription})", formatRupiah(d.remainingAmount))
              }
            }
          }
        }
      }
    }

    item {
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
          onClick = { sharePlainText(context, reportTitle, shareSummary) },
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.weight(1f).height(50.dp),
        ) {
          Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Bagikan Teks")
        }
        Button(
          onClick = {
            val safeName = reportTypes[reportIdx].lowercase().replace(" ", "_")
            shareCsvFile(context, "laporan_${safeName}.csv", csvContent)
          },
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.weight(1f).height(50.dp),
        ) {
          Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Unduh / CSV")
        }
      }
    }
  }
}

private fun buildReportOutputs(
  title: String,
  periodLabel: String,
  reportIdx: Int,
  startMillis: Long,
  bundle: StoreDataBundle,
): Pair<String, String> {
  val periodSales = bundle.sales.filter { it.occurredAtMillis >= startMillis }
  val periodCashOut = bundle.cashTransactions.filter { it.direction == "KELUAR" && it.occurredAtMillis >= startMillis }

  val text =
    buildString {
      appendLine(title)
      appendLine("Tanggal: ${formatDateShort(System.currentTimeMillis())}")
      appendLine("==============================")
      when (reportIdx) {
        0 -> {
          appendLine("Jumlah Transaksi: ${periodSales.size}")
          appendLine("Total Penjualan: ${formatRupiah(periodSales.sumOf { it.totalAmount })}")
          periodSales.forEach { appendLine("- ${it.receiptNumber}: ${formatRupiah(it.totalAmount)}") }
        }
        1 -> {
          val omzet = periodSales.sumOf { it.totalAmount }
          val cogs = periodSales.sumOf { it.totalCost }
          val ops = periodCashOut.filter { it.category !in listOf("Belanja Stok", "Pengambilan Uang Pribadi") }.sumOf { it.amount }
          appendLine("Omzet: ${formatRupiah(omzet)}")
          appendLine("Modal: ${formatRupiah(cogs)}")
          appendLine("Laba Kotor: ${formatRupiah(omzet - cogs)}")
          appendLine("Operasional: ${formatRupiah(ops)}")
          appendLine("Laba Bersih: ${formatRupiah(omzet - cogs - ops)}")
        }
        2 -> {
          appendLine("Total Pengeluaran ($periodLabel): ${formatRupiah(periodCashOut.sumOf { it.amount })}")
          periodCashOut.forEach { appendLine("- ${it.category}: ${formatRupiah(it.amount)} (${it.description})") }
        }
        3 -> {
          appendLine("Total Produk: ${bundle.products.size}")
          bundle.products.forEach { appendLine("- ${it.name}: ${it.stock} ${it.unit} (Modal ${formatRupiah(it.costPrice)})") }
        }
        4 -> {
          val unpaid = bundle.customerDebts.filter { it.status == "BELUM_LUNAS" }
          appendLine("Total Piutang: ${formatRupiah(unpaid.sumOf { it.remainingAmount })}")
          unpaid.forEach { appendLine("- ${it.customerName}: ${formatRupiah(it.remainingAmount)}") }
        }
        else -> {
          val unpaid = bundle.supplierDebts.filter { it.status == "BELUM_LUNAS" }
          appendLine("Total Utang Supplier: ${formatRupiah(unpaid.sumOf { it.remainingAmount })}")
          unpaid.forEach { appendLine("- ${it.supplierName}: ${formatRupiah(it.remainingAmount)}") }
        }
      }
    }

  val csv =
    buildString {
      when (reportIdx) {
        0 -> {
          appendLine("No Struk,Pelanggan,Metode,Total,Modal,Keuntungan,Tanggal")
          periodSales.forEach {
            appendLine("${it.receiptNumber},\"${it.customerName}\",${it.paymentMethod},${it.totalAmount},${it.totalCost},${it.totalProfit},${formatDateShort(it.occurredAtMillis)}")
          }
        }
        1 -> {
          val omzet = periodSales.sumOf { it.totalAmount }
          val cogs = periodSales.sumOf { it.totalCost }
          val ops = periodCashOut.filter { it.category !in listOf("Belanja Stok", "Pengambilan Uang Pribadi") }.sumOf { it.amount }
          appendLine("Metrik,Nominal")
          appendLine("Omzet,$omzet")
          appendLine("Modal Terjual,$cogs")
          appendLine("Laba Kotor,${omzet - cogs}")
          appendLine("Biaya Operasional,$ops")
          appendLine("Laba Bersih,${omzet - cogs - ops}")
        }
        2 -> {
          appendLine("Kategori,Nominal,Metode,Keterangan,Tanggal")
          periodCashOut.forEach {
            appendLine("\"${it.category}\",${it.amount},${it.paymentMethod},\"${it.description}\",${formatDateShort(it.occurredAtMillis)}")
          }
        }
        3 -> {
          appendLine("Nama Barang,Kategori,Stok,Satuan,Harga Modal,Harga Jual")
          bundle.products.forEach {
            appendLine("\"${it.name}\",\"${it.category}\",${it.stock},${it.unit},${it.costPrice},${it.sellPrice}")
          }
        }
        4 -> {
          appendLine("Pelanggan,Total Utang,Dibayar,Sisa,Status")
          bundle.customerDebts.forEach {
            appendLine("\"${it.customerName}\",${it.totalDebt},${it.paidAmount},${it.remainingAmount},${it.status}")
          }
        }
        else -> {
          appendLine("Supplier,Barang,Total Utang,Dibayar,Sisa,Status")
          bundle.supplierDebts.forEach {
            appendLine("\"${it.supplierName}\",\"${it.itemDescription}\",${it.totalDebt},${it.paidAmount},${it.remainingAmount},${it.status}")
          }
        }
      }
    }

  return text to csv
}

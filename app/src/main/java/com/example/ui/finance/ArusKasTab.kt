package com.example.ui.finance

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CashTransaction
import com.example.model.PaymentMethod
import com.example.model.StoreConstants
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.RupiahNumberField
import com.example.ui.theme.NumericTextStyle
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess
import com.example.util.formatDateTime
import com.example.util.formatRupiah

@Composable
fun ArusKasTab(
  transactions: List<CashTransaction>,
  onRecordCash: (direction: String, category: String, amount: Long, method: PaymentMethod, description: String, () -> Unit) -> Unit,
  onDeleteCash: (CashTransaction) -> Unit,
) {
  var filterDirection by remember { mutableStateOf<String?>(null) }
  var dialogDirection by remember { mutableStateOf<String?>(null) }
  var deleteTarget by remember { mutableStateOf<CashTransaction?>(null) }

  val totalMasuk = transactions.filter { it.direction == "MASUK" }.sumOf { it.amount }
  val totalKeluar = transactions.filter { it.direction == "KELUAR" }.sumOf { it.amount }
  val saldo = totalMasuk - totalKeluar

  val filtered =
    transactions.filter { filterDirection == null || it.direction == filterDirection }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("Saldo Keuangan Toko", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            text = formatRupiah(saldo),
            style = NumericTextStyle.copy(fontSize = MaterialTheme.typography.headlineMedium.fontSize),
            color = if (saldo >= 0) MaterialTheme.colorScheme.primary else SemanticError,
          )
          Spacer(modifier = Modifier.height(12.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Total Pemasukan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(formatRupiah(totalMasuk), style = NumericTextStyle, color = SemanticSuccess)
            }
            Column(modifier = Modifier.weight(1f)) {
              Text("Total Pengeluaran", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(formatRupiah(totalKeluar), style = NumericTextStyle, color = SemanticError)
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
          onClick = { dialogDirection = "MASUK" },
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.weight(1f).height(48.dp).testTag("add_cash_in_button"),
        ) {
          Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Uang Masuk")
        }
        OutlinedButton(
          onClick = { dialogDirection = "KELUAR" },
          shape = MaterialTheme.shapes.medium,
          colors = ButtonDefaults.outlinedButtonColors(contentColor = SemanticError),
          modifier = Modifier.weight(1f).height(48.dp).testTag("add_cash_out_button"),
        ) {
          Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Uang Keluar")
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = filterDirection == null, onClick = { filterDirection = null }, label = { Text("Semua") })
        FilterChip(selected = filterDirection == "MASUK", onClick = { filterDirection = "MASUK" }, label = { Text("Uang Masuk") })
        FilterChip(selected = filterDirection == "KELUAR", onClick = { filterDirection = "KELUAR" }, label = { Text("Uang Keluar") })
      }
    }

    if (filtered.isEmpty()) {
      item {
        EmptyStateCard(
          title = "Belum Ada Catatan Kas",
          description = "Catat pemasukan modal, biaya listrik, transportasi, keperluan toko, atau pengeluaran pribadi di sini.",
        )
      }
    } else {
      items(filtered, key = { it.id }) { tx ->
        val isIn = tx.direction == "MASUK"
        val accent = if (isIn) SemanticSuccess else SemanticError
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
              modifier = Modifier.size(40.dp).clip(CircleShape).background(accent.copy(alpha = 0.14f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = if (isIn) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                contentDescription = null,
                tint = accent,
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(tx.category, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              if (tx.description.isNotBlank()) {
                Text(
                  tx.description,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Text(
                "${PaymentMethod.fromCode(tx.paymentMethod).label} • ${formatDateTime(tx.occurredAtMillis)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Text(
              text = "${if (isIn) "+" else "-"}${formatRupiah(tx.amount)}",
              style = NumericTextStyle.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize),
              color = accent,
            )
            IconButton(
              onClick = { deleteTarget = tx },
              modifier = Modifier.size(48.dp),
            ) {
              Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = "Hapus catatan kas ${tx.category}",
                tint = SemanticError,
                modifier = Modifier.size(20.dp),
              )
            }
          }
        }
      }
    }
  }

  dialogDirection?.let { dir ->
    CashFormDialog(
      direction = dir,
      onConfirm = { category, amount, method, desc ->
        onRecordCash(dir, category, amount, method, desc) { dialogDirection = null }
      },
      onDismiss = { dialogDirection = null },
    )
  }

  deleteTarget?.let { tx ->
    AlertDialog(
      onDismissRequest = { deleteTarget = null },
      title = { Text("Hapus Catatan Kas?", fontWeight = FontWeight.Bold) },
      text = {
        Text("Hapus catatan ${tx.category} (${formatRupiah(tx.amount)}) dari arus kas?")
      },
      confirmButton = {
        Button(
          colors = ButtonDefaults.buttonColors(containerColor = SemanticError),
          onClick = {
            onDeleteCash(tx)
            deleteTarget = null
          },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CashFormDialog(
  direction: String,
  onConfirm: (category: String, amount: Long, method: PaymentMethod, description: String) -> Unit,
  onDismiss: () -> Unit,
) {
  val categories =
    if (direction == "MASUK") StoreConstants.INCOME_CATEGORIES else StoreConstants.EXPENSE_CATEGORIES
  var selectedCategory by remember { mutableStateOf(categories.first()) }
  var catExpanded by remember { mutableStateOf(false) }
  var amountText by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var paymentMethod by remember { mutableStateOf(PaymentMethod.TUNAI) }

  val amountVal = amountText.toLongOrNull() ?: 0L

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        if (direction == "MASUK") "Catat Uang Masuk" else "Catat Uang Keluar",
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ExposedDropdownMenuBox(
          expanded = catExpanded,
          onExpandedChange = { catExpanded = !catExpanded },
        ) {
          OutlinedTextField(
            value = selectedCategory,
            onValueChange = {},
            readOnly = true,
            label = { Text("Kategori") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
          )
          ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
            categories.forEach { cat ->
              DropdownMenuItem(
                text = { Text(cat) },
                onClick = {
                  selectedCategory = cat
                  catExpanded = false
                },
              )
            }
          }
        }

        RupiahNumberField(
          value = amountText,
          onValueChange = { amountText = it },
          label = "Nominal",
        )

        OutlinedTextField(
          value = description,
          onValueChange = { description = it.take(250) },
          label = { Text("Keterangan") },
          placeholder = { Text("Contoh: Token listrik toko / Tambah modal") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        )

        Text("Metode", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(PaymentMethod.TUNAI, PaymentMethod.TRANSFER, PaymentMethod.QRIS).forEach { m ->
            FilterChip(
              selected = paymentMethod == m,
              onClick = { paymentMethod = m },
              label = { Text(m.label) },
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        enabled = amountVal > 0L,
        onClick = { onConfirm(selectedCategory, amountVal, paymentMethod, description) },
      ) {
        Text("Simpan")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Batal") }
    },
  )
}

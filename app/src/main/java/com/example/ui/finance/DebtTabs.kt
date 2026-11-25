package com.example.ui.finance

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.CustomerDebt
import com.example.model.DebtPayment
import com.example.model.PaymentMethod
import com.example.model.SupplierDebt
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MetricRowLine
import com.example.ui.components.RupiahNumberField
import com.example.ui.components.StatusBadge
import com.example.ui.theme.NumericTextStyle
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticWarning
import com.example.util.formatDateShort
import com.example.util.formatDateTime
import com.example.util.formatRupiah
import com.example.util.formatTimeOnly
import java.util.Calendar
import java.util.TimeZone

@Composable
fun UtangPelangganTab(
  debts: List<CustomerDebt>,
  onAddDebt: (name: String, phone: String, amount: Long, note: String, occurredAtMillis: Long, () -> Unit) -> Unit,
  onPayDebt: (debt: CustomerDebt, amount: Long, method: PaymentMethod, note: String, () -> Unit) -> Unit,
  onUpdateNote: (debtId: String, note: String, () -> Unit) -> Unit,
  onDeleteDebt: (CustomerDebt) -> Unit,
) {
  var statusFilter by remember { mutableIntStateOf(0) } // 0=Belum Lunas, 1=Lunas, 2=Semua
  var showAddDialog by remember { mutableStateOf(false) }
  var selectedDebt by remember { mutableStateOf<CustomerDebt?>(null) }
  var deleteTarget by remember { mutableStateOf<CustomerDebt?>(null) }

  val totalUnpaid = debts.filter { it.status == "BELUM_LUNAS" }.sumOf { it.remainingAmount }
  val filtered =
    debts.filter {
      when (statusFilter) {
        0 -> it.status == "BELUM_LUNAS"
        1 -> it.status == "LUNAS"
        else -> true
      }
    }

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
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text("Total Utang Pelanggan", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
              text = formatRupiah(totalUnpaid),
              style = NumericTextStyle.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize),
              color = SemanticWarning,
            )
          }
          Button(
            onClick = { showAddDialog = true },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.testTag("add_customer_debt_button"),
          ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Tambah")
          }
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = statusFilter == 0, onClick = { statusFilter = 0 }, label = { Text("Belum Lunas") })
        FilterChip(selected = statusFilter == 1, onClick = { statusFilter = 1 }, label = { Text("Lunas") })
        FilterChip(selected = statusFilter == 2, onClick = { statusFilter = 2 }, label = { Text("Semua") })
      }
    }

    if (filtered.isEmpty()) {
      item {
        EmptyStateCard(
          title = "Tidak Ada Catatan Utang Pelanggan",
          description = "Utang pelanggan dari kasir atau catatan manual akan tampil di sini beserta tanggal, waktu, catatan, dan cicilan.",
        )
      }
    } else {
      items(filtered, key = { it.id }) { debt ->
        DebtSummaryCard(
          title = debt.customerName,
          subtitle = if (debt.phone.isNotBlank()) "Telp: ${debt.phone}" else "",
          occurredAtMillis = debt.occurredAtMillis,
          note = debt.note,
          totalDebt = debt.totalDebt,
          paidAmount = debt.paidAmount,
          remainingAmount = debt.remainingAmount,
          isPaidOff = debt.status == "LUNAS",
          accentColor = SemanticWarning,
          onClick = { selectedDebt = debt },
          onDeleteClick = { deleteTarget = debt },
        )
      }
    }
  }

  if (showAddDialog) {
    AddCustomerDebtDialog(
      onConfirm = { name, phone, amount, note, occurredAtMillis ->
        onAddDebt(name, phone, amount, note, occurredAtMillis) { showAddDialog = false }
      },
      onDismiss = { showAddDialog = false },
    )
  }

  selectedDebt?.let { debt ->
    DebtDetailPayDialog(
      title = debt.customerName,
      subtitle = if (debt.phone.isNotBlank()) "Telp: ${debt.phone}" else "",
      occurredAtMillis = debt.occurredAtMillis,
      initialNote = debt.note,
      totalDebt = debt.totalDebt,
      paidAmount = debt.paidAmount,
      remainingAmount = debt.remainingAmount,
      isPaidOff = debt.status == "LUNAS",
      payments = debt.payments,
      onPay = { amount, method, note ->
        onPayDebt(debt, amount, method, note) { selectedDebt = null }
      },
      onUpdateNote = { updatedNote ->
        onUpdateNote(debt.id, updatedNote) {
          selectedDebt = debt.copy(note = updatedNote)
        }
      },
      onDelete = {
        selectedDebt = null
        deleteTarget = debt
      },
      onDismiss = { selectedDebt = null },
    )
  }

  deleteTarget?.let { debt ->
    AlertDialog(
      onDismissRequest = { deleteTarget = null },
      title = { Text("Hapus Utang Pelanggan?", fontWeight = FontWeight.Bold) },
      text = { Text("Hapus catatan utang \"${debt.customerName}\" (${formatRupiah(debt.totalDebt)})?") },
      confirmButton = {
        Button(
          colors = ButtonDefaults.buttonColors(containerColor = SemanticError),
          onClick = {
            onDeleteDebt(debt)
            deleteTarget = null
          },
        ) {
          Text("Hapus")
        }
      },
      dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Batal") } },
    )
  }
}

@Composable
fun UtangSupplierTab(
  debts: List<SupplierDebt>,
  onAddDebt: (supplierName: String, itemDesc: String, amount: Long, note: String, occurredAtMillis: Long, () -> Unit) -> Unit,
  onPayDebt: (debt: SupplierDebt, amount: Long, method: PaymentMethod, note: String, () -> Unit) -> Unit,
  onUpdateNote: (debtId: String, note: String, () -> Unit) -> Unit,
  onDeleteDebt: (SupplierDebt) -> Unit,
) {
  var statusFilter by remember { mutableIntStateOf(0) }
  var showAddDialog by remember { mutableStateOf(false) }
  var selectedDebt by remember { mutableStateOf<SupplierDebt?>(null) }
  var deleteTarget by remember { mutableStateOf<SupplierDebt?>(null) }

  val totalUnpaid = debts.filter { it.status == "BELUM_LUNAS" }.sumOf { it.remainingAmount }
  val filtered =
    debts.filter {
      when (statusFilter) {
        0 -> it.status == "BELUM_LUNAS"
        1 -> it.status == "LUNAS"
        else -> true
      }
    }

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
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text("Total Utang ke Supplier", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
              text = formatRupiah(totalUnpaid),
              style = NumericTextStyle.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize),
              color = SemanticError,
            )
          }
          Button(
            onClick = { showAddDialog = true },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.testTag("add_supplier_debt_button"),
          ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Tambah")
          }
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = statusFilter == 0, onClick = { statusFilter = 0 }, label = { Text("Belum Lunas") })
        FilterChip(selected = statusFilter == 1, onClick = { statusFilter = 1 }, label = { Text("Lunas") })
        FilterChip(selected = statusFilter == 2, onClick = { statusFilter = 2 }, label = { Text("Semua") })
      }
    }

    if (filtered.isEmpty()) {
      item {
        EmptyStateCard(
          title = "Tidak Ada Utang Supplier",
          description = "Catatan pembelian tempo / utang kepada pemasok akan tampil di sini.",
        )
      }
    } else {
      items(filtered, key = { it.id }) { debt ->
        DebtSummaryCard(
          title = debt.supplierName,
          subtitle = debt.itemDescription,
          occurredAtMillis = debt.occurredAtMillis,
          note = debt.note,
          totalDebt = debt.totalDebt,
          paidAmount = debt.paidAmount,
          remainingAmount = debt.remainingAmount,
          isPaidOff = debt.status == "LUNAS",
          accentColor = SemanticError,
          onClick = { selectedDebt = debt },
          onDeleteClick = { deleteTarget = debt },
        )
      }
    }
  }

  if (showAddDialog) {
    AddSupplierDebtDialog(
      onConfirm = { supplier, itemDesc, amount, note, occurredAtMillis ->
        onAddDebt(supplier, itemDesc, amount, note, occurredAtMillis) { showAddDialog = false }
      },
      onDismiss = { showAddDialog = false },
    )
  }

  selectedDebt?.let { debt ->
    DebtDetailPayDialog(
      title = debt.supplierName,
      subtitle = debt.itemDescription,
      occurredAtMillis = debt.occurredAtMillis,
      initialNote = debt.note,
      totalDebt = debt.totalDebt,
      paidAmount = debt.paidAmount,
      remainingAmount = debt.remainingAmount,
      isPaidOff = debt.status == "LUNAS",
      payments = debt.payments,
      onPay = { amount, method, note ->
        onPayDebt(debt, amount, method, note) { selectedDebt = null }
      },
      onUpdateNote = { updatedNote ->
        onUpdateNote(debt.id, updatedNote) {
          selectedDebt = debt.copy(note = updatedNote)
        }
      },
      onDelete = {
        selectedDebt = null
        deleteTarget = debt
      },
      onDismiss = { selectedDebt = null },
    )
  }

  deleteTarget?.let { debt ->
    AlertDialog(
      onDismissRequest = { deleteTarget = null },
      title = { Text("Hapus Utang Supplier?", fontWeight = FontWeight.Bold) },
      text = { Text("Hapus catatan utang ke \"${debt.supplierName}\" (${formatRupiah(debt.totalDebt)})?") },
      confirmButton = {
        Button(
          colors = ButtonDefaults.buttonColors(containerColor = SemanticError),
          onClick = {
            onDeleteDebt(debt)
            deleteTarget = null
          },
        ) {
          Text("Hapus")
        }
      },
      dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Batal") } },
    )
  }
}

@Composable
private fun DebtSummaryCard(
  title: String,
  subtitle: String,
  occurredAtMillis: Long,
  note: String,
  totalDebt: Long,
  paidAmount: Long,
  remainingAmount: Long,
  isPaidOff: Boolean,
  accentColor: Color,
  onClick: () -> Unit,
  onDeleteClick: () -> Unit,
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = MaterialTheme.shapes.medium,
    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          StatusBadge(
            text = if (isPaidOff) "Lunas" else "Belum Lunas",
            containerColor = if (isPaidOff) MaterialTheme.colorScheme.primaryContainer else Color(0xFFFEF3C7),
            contentColor = if (isPaidOff) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF92400E),
          )
          IconButton(
            onClick = onDeleteClick,
            modifier = Modifier.size(48.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.DeleteOutline,
              contentDescription = "Hapus utang $title",
              tint = SemanticError,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }
      if (subtitle.isNotBlank()) {
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.CalendarToday,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = formatDateShort(occurredAtMillis),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = formatTimeOnly(occurredAtMillis),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
      if (note.isNotBlank()) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Top,
          ) {
            Icon(
              imageVector = Icons.Outlined.Notes,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(15.dp).padding(top = 1.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Catatan: $note",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Total: ${formatRupiah(totalDebt)}", style = MaterialTheme.typography.labelSmall)
        Text("Dibayar: ${formatRupiah(paidAmount)}", style = MaterialTheme.typography.labelSmall, color = SemanticSuccess)
        Text(
          text = "Sisa: ${formatRupiah(remainingAmount)}",
          style = NumericTextStyle.copy(fontSize = MaterialTheme.typography.labelMedium.fontSize),
          color = if (isPaidOff) SemanticSuccess else accentColor,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtDateTimePickerRow(
  occurredAtMillis: Long,
  onChange: (Long) -> Unit,
) {
  var showDatePicker by remember { mutableStateOf(false) }
  var showTimePicker by remember { mutableStateOf(false) }

  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text("Tanggal & Waktu Utang", style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedButton(
        onClick = { showDatePicker = true },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.weight(1.2f).height(48.dp).testTag("debt_date_picker_button"),
      ) {
        Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(formatDateShort(occurredAtMillis), maxLines = 1)
      }
      OutlinedButton(
        onClick = { showTimePicker = true },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.weight(0.9f).height(48.dp).testTag("debt_time_picker_button"),
      ) {
        Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(formatTimeOnly(occurredAtMillis), maxLines = 1)
      }
    }
  }

  if (showDatePicker) {
    val localCal = remember(occurredAtMillis) {
      Calendar.getInstance().apply { timeInMillis = occurredAtMillis }
    }
    val initialUtcMillis = remember(occurredAtMillis) {
      Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(
          localCal.get(Calendar.YEAR),
          localCal.get(Calendar.MONTH),
          localCal.get(Calendar.DAY_OF_MONTH),
          0,
          0,
          0,
        )
        set(Calendar.MILLISECOND, 0)
      }.timeInMillis
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { utcMillis ->
              val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
              val merged =
                Calendar.getInstance().apply {
                  timeInMillis = occurredAtMillis
                  set(
                    utcCal.get(Calendar.YEAR),
                    utcCal.get(Calendar.MONTH),
                    utcCal.get(Calendar.DAY_OF_MONTH),
                  )
                }
              onChange(merged.timeInMillis)
            }
            showDatePicker = false
          },
        ) {
          Text("Pilih")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
      },
    ) {
      DatePicker(state = datePickerState)
    }
  }

  if (showTimePicker) {
    val currentCal = remember(occurredAtMillis) {
      Calendar.getInstance().apply { timeInMillis = occurredAtMillis }
    }
    val timeState =
      rememberTimePickerState(
        initialHour = currentCal.get(Calendar.HOUR_OF_DAY),
        initialMinute = currentCal.get(Calendar.MINUTE),
        is24Hour = true,
      )
    AlertDialog(
      onDismissRequest = { showTimePicker = false },
      title = { Text("Pilih Waktu Utang", fontWeight = FontWeight.Bold) },
      text = { TimePicker(state = timeState) },
      confirmButton = {
        Button(
          onClick = {
            val merged =
              Calendar.getInstance().apply {
                timeInMillis = occurredAtMillis
                set(Calendar.HOUR_OF_DAY, timeState.hour)
                set(Calendar.MINUTE, timeState.minute)
                set(Calendar.SECOND, 0)
              }
            onChange(merged.timeInMillis)
            showTimePicker = false
          },
        ) {
          Text("Pilih")
        }
      },
      dismissButton = {
        TextButton(onClick = { showTimePicker = false }) { Text("Batal") }
      },
    )
  }
}

@Composable
private fun AddCustomerDebtDialog(
  onConfirm: (name: String, phone: String, amount: Long, note: String, occurredAtMillis: Long) -> Unit,
  onDismiss: () -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var amountText by remember { mutableStateOf("") }
  var note by remember { mutableStateOf("") }
  var occurredAtMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  val amount = amountText.toLongOrNull() ?: 0L

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Catat Utang Pelanggan", fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it.take(100) },
          label = { Text("Nama Pelanggan") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it.take(30) },
          label = { Text("Nomor Telepon (Opsional)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        RupiahNumberField(value = amountText, onValueChange = { amountText = it }, label = "Total Utang")
        DebtDateTimePickerRow(occurredAtMillis = occurredAtMillis, onChange = { occurredAtMillis = it })
        OutlinedTextField(
          value = note,
          onValueChange = { note = it.take(300) },
          label = { Text("Catatan Utang") },
          placeholder = { Text("Contoh: Beras 5kg + gula, janji bayar akhir bulan") },
          minLines = 2,
          maxLines = 3,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        enabled = name.trim().isNotEmpty() && amount > 0L,
        onClick = { onConfirm(name, phone, amount, note, occurredAtMillis) },
      ) {
        Text("Simpan")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
  )
}

@Composable
private fun AddSupplierDebtDialog(
  onConfirm: (supplierName: String, itemDesc: String, amount: Long, note: String, occurredAtMillis: Long) -> Unit,
  onDismiss: () -> Unit,
) {
  var supplier by remember { mutableStateOf("") }
  var itemDesc by remember { mutableStateOf("") }
  var amountText by remember { mutableStateOf("") }
  var note by remember { mutableStateOf("") }
  var occurredAtMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  val amount = amountText.toLongOrNull() ?: 0L

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Catat Utang Supplier", fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = supplier,
          onValueChange = { supplier = it.take(100) },
          label = { Text("Nama Supplier") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = itemDesc,
          onValueChange = { itemDesc = it.take(200) },
          label = { Text("Barang yang Dibeli") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        RupiahNumberField(value = amountText, onValueChange = { amountText = it }, label = "Total Utang")
        DebtDateTimePickerRow(occurredAtMillis = occurredAtMillis, onChange = { occurredAtMillis = it })
        OutlinedTextField(
          value = note,
          onValueChange = { note = it.take(300) },
          label = { Text("Catatan Utang") },
          placeholder = { Text("Contoh: Jatuh tempo 14 hari / no faktur") },
          minLines = 2,
          maxLines = 3,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        enabled = supplier.trim().isNotEmpty() && itemDesc.trim().isNotEmpty() && amount > 0L,
        onClick = { onConfirm(supplier, itemDesc, amount, note, occurredAtMillis) },
      ) {
        Text("Simpan")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
  )
}

@Composable
private fun DebtDetailPayDialog(
  title: String,
  subtitle: String,
  occurredAtMillis: Long,
  initialNote: String,
  totalDebt: Long,
  paidAmount: Long,
  remainingAmount: Long,
  isPaidOff: Boolean,
  payments: List<DebtPayment>,
  onPay: (amount: Long, method: PaymentMethod, note: String) -> Unit,
  onUpdateNote: (String) -> Unit,
  onDelete: () -> Unit,
  onDismiss: () -> Unit,
) {
  var payText by remember { mutableStateOf(remainingAmount.toString()) }
  var paymentNote by remember { mutableStateOf("") }
  var debtNote by remember(initialNote) { mutableStateOf(initialNote) }
  var method by remember { mutableStateOf(PaymentMethod.TUNAI) }
  val payVal = payText.toLongOrNull() ?: 0L

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
          Icon(Icons.Outlined.DeleteOutline, contentDescription = "Hapus utang", tint = SemanticError)
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (subtitle.isNotBlank()) {
          Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        MetricRowLine("Tanggal Utang", formatDateShort(occurredAtMillis))
        MetricRowLine("Waktu Utang", formatTimeOnly(occurredAtMillis))
        MetricRowLine("Total Utang", formatRupiah(totalDebt))
        MetricRowLine("Sudah Dibayar", formatRupiah(paidAmount), valueColor = SemanticSuccess)
        MetricRowLine("Sisa Utang", formatRupiah(remainingAmount), valueColor = if (isPaidOff) SemanticSuccess else SemanticError, bold = true)

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Text("Catatan Utang", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        if (!isPaidOff) {
          OutlinedTextField(
            value = debtNote,
            onValueChange = { debtNote = it.take(300) },
            label = { Text("Catatan") },
            minLines = 2,
            maxLines = 3,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          )
          if (debtNote.trim() != initialNote.trim()) {
            OutlinedButton(
              onClick = { onUpdateNote(debtNote.trim()) },
              modifier = Modifier.align(Alignment.End),
            ) {
              Text("Simpan Catatan")
            }
          }
        } else {
          Text(
            text = initialNote.ifBlank { "Tidak ada catatan." },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        if (!isPaidOff) {
          HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
          Text("Catat Pembayaran / Cicilan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          RupiahNumberField(value = payText, onValueChange = { payText = it }, label = "Nominal Bayar")
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { payText = remainingAmount.toString() }) {
              Text("Lunasi Semua")
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(PaymentMethod.TUNAI, PaymentMethod.TRANSFER, PaymentMethod.QRIS).forEach { m ->
              FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m.label) })
            }
          }
          OutlinedTextField(
            value = paymentNote,
            onValueChange = { paymentNote = it.take(150) },
            label = { Text("Catatan Pembayaran") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }

        if (payments.isNotEmpty()) {
          HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
          Text("Riwayat Pembayaran (${payments.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          payments.reversed().forEach { p ->
            MetricRowLine(
              label = "${formatDateTime(p.paidAtMillis)}\n${p.note.ifBlank { PaymentMethod.fromCode(p.paymentMethod).label }}",
              valueText = formatRupiah(p.amount),
              valueColor = SemanticSuccess,
            )
          }
        }
      }
    },
    confirmButton = {
      if (!isPaidOff) {
        Button(
          enabled = payVal in 1L..remainingAmount,
          onClick = { onPay(payVal, method, paymentNote) },
        ) {
          Text("Simpan Pembayaran")
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Tutup") }
    },
  )
}

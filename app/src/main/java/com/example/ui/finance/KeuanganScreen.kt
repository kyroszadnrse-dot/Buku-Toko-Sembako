package com.example.ui.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.CashTransaction
import com.example.model.CustomerDebt
import com.example.model.PaymentMethod
import com.example.model.SupplierDebt
import com.example.viewmodel.FinancialHealthReport
import com.example.viewmodel.ProfitLossSummary
import com.example.viewmodel.StoreDataBundle

@Composable
fun KeuanganScreen(
  selectedSubTab: Int,
  onSelectSubTab: (Int) -> Unit,
  bundle: StoreDataBundle,
  profitLossSummary: ProfitLossSummary,
  financialHealth: FinancialHealthReport,
  onRecordCash: (String, String, Long, PaymentMethod, String, () -> Unit) -> Unit,
  onDeleteCash: (CashTransaction) -> Unit,
  onAddCustomerDebt: (String, String, Long, String, Long, () -> Unit) -> Unit,
  onPayCustomerDebt: (CustomerDebt, Long, PaymentMethod, String, () -> Unit) -> Unit,
  onUpdateCustomerDebtNote: (String, String, () -> Unit) -> Unit,
  onDeleteCustomerDebt: (CustomerDebt) -> Unit,
  onAddSupplierDebt: (String, String, Long, String, Long, () -> Unit) -> Unit,
  onPaySupplierDebt: (SupplierDebt, Long, PaymentMethod, String, () -> Unit) -> Unit,
  onUpdateSupplierDebtNote: (String, String, () -> Unit) -> Unit,
  onDeleteSupplierDebt: (SupplierDebt) -> Unit,
) {
  val tabs =
    listOf(
      "Arus Kas",
      "Utang Pelanggan",
      "Utang Supplier",
      "Laba Rugi",
      "Kalender",
      "Laporan",
    )

  Column(modifier = Modifier.fillMaxSize()) {
    LazyRow(
      modifier = Modifier.fillMaxWidth(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      itemsIndexed(tabs) { idx, title ->
        FilterChip(
          selected = selectedSubTab == idx,
          onClick = { onSelectSubTab(idx) },
          label = { Text(title) },
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primary,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
      }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

    when (selectedSubTab) {
      0 ->
        ArusKasTab(
          transactions = bundle.cashTransactions,
          onRecordCash = onRecordCash,
          onDeleteCash = onDeleteCash,
        )
      1 ->
        UtangPelangganTab(
          debts = bundle.customerDebts,
          onAddDebt = onAddCustomerDebt,
          onPayDebt = onPayCustomerDebt,
          onUpdateNote = onUpdateCustomerDebtNote,
          onDeleteDebt = onDeleteCustomerDebt,
        )
      2 ->
        UtangSupplierTab(
          debts = bundle.supplierDebts,
          onAddDebt = onAddSupplierDebt,
          onPayDebt = onPaySupplierDebt,
          onUpdateNote = onUpdateSupplierDebtNote,
          onDeleteDebt = onDeleteSupplierDebt,
        )
      3 ->
        LabaRugiTab(
          summary = profitLossSummary,
          financialHealth = financialHealth,
        )
      4 ->
        KalenderTab(bundle = bundle)
      else ->
        LaporanTab(bundle = bundle)
    }
  }
}

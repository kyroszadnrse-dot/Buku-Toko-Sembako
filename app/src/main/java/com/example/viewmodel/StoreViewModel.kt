package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.StoreRepository
import com.example.model.ActivityEntry
import com.example.model.ActivityType
import com.example.model.CashTransaction
import com.example.model.CustomerDebt
import com.example.model.PaymentMethod
import com.example.model.Product
import com.example.model.Sale
import com.example.model.StockChangeType
import com.example.model.StockLog
import com.example.model.StockPurchase
import com.example.model.SupplierDebt
import com.example.util.formatDayShort
import com.example.util.formatPercent
import com.example.util.formatRupiah
import com.example.util.startOfDaysAgoMillis
import com.example.util.startOfMonthMillis
import com.example.util.startOfTodayMillis
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

data class DailyChartPoint(
  val label: String,
  val income: Long,
  val expense: Long,
  val profit: Long,
)

data class ProductProfitRank(
  val productName: String,
  val qtySold: Long,
  val revenue: Long,
  val profit: Long,
)

enum class HealthLevel {
  SEHAT,
  CUKUP,
  PERLU_PERHATIAN,
  WASPADA,
}

data class FinancialIndicator(
  val label: String,
  val valueText: String,
  val isHealthy: Boolean,
)

data class FinancialAdvice(
  val title: String,
  val detail: String,
  val priorityHigh: Boolean,
)

data class FinancialHealthReport(
  val score: Int = 100,
  val statusLabel: String = "Sangat Sehat",
  val level: HealthLevel = HealthLevel.SEHAT,
  val indicators: List<FinancialIndicator> = emptyList(),
  val adviceList: List<FinancialAdvice> = emptyList(),
)

data class DashboardSnapshot(
  val todaySalesTotal: Long = 0L,
  val totalIncome: Long = 0L,
  val totalExpense: Long = 0L,
  val todayProfit: Long = 0L,
  val monthProfit: Long = 0L,
  val availableBalance: Long = 0L,
  val totalCustomerDebt: Long = 0L,
  val totalSupplierDebt: Long = 0L,
  val totalStockUnits: Long = 0L,
  val totalProductCount: Int = 0,
  val totalSalesCount: Int = 0,
  val todayCustomerCount: Int = 0,
  val uniqueNamedCustomers: Int = 0,
  val lowStockProducts: List<Product> = emptyList(),
  val chartPoints: List<DailyChartPoint> = emptyList(),
  val financialHealth: FinancialHealthReport = FinancialHealthReport(),
)

data class ProfitLossSummary(
  val totalOmzet: Long = 0L,
  val costOfGoodsSold: Long = 0L,
  val grossProfit: Long = 0L,
  val operationalExpenses: Long = 0L,
  val damagedGoodsLoss: Long = 0L,
  val netProfit: Long = 0L,
  val marginPercent: Double = 0.0,
  val markupPercent: Double = 0.0,
  val availableBalance: Long = 0L,
  val unpaidReceivables: Long = 0L,
  val topProfitableProducts: List<ProductProfitRank> = emptyList(),
)

data class StoreDataBundle(
  val products: List<Product> = emptyList(),
  val sales: List<Sale> = emptyList(),
  val purchases: List<StockPurchase> = emptyList(),
  val stockLogs: List<StockLog> = emptyList(),
  val cashTransactions: List<CashTransaction> = emptyList(),
  val customerDebts: List<CustomerDebt> = emptyList(),
  val supplierDebts: List<SupplierDebt> = emptyList(),
  val activities: List<ActivityEntry> = emptyList(),
)

class StoreViewModel(
  private val repository: StoreRepository,
  private val currentUserId: String,
) : ViewModel() {

  private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val messages = _messages.asSharedFlow()

  private fun <T> Flow<List<T>>.toUiStateFlow(tag: String, fallbackMsg: String): StateFlow<UiState<List<T>>> =
    this
      .map<List<T>, UiState<List<T>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(tag, fallbackMsg, rootError)
        emit(UiState.Error(rootError.message ?: fallbackMsg))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val productsState: StateFlow<UiState<List<Product>>> =
    repository.observeProducts(currentUserId).toUiStateFlow("StoreVM", "Gagal memuat daftar barang")

  val salesState: StateFlow<UiState<List<Sale>>> =
    repository.observeSales(currentUserId).toUiStateFlow("StoreVM", "Gagal memuat riwayat penjualan")

  val purchasesState: StateFlow<UiState<List<StockPurchase>>> =
    repository.observePurchases(currentUserId).toUiStateFlow("StoreVM", "Gagal memuat pembelian stok")

  val stockLogsState: StateFlow<UiState<List<StockLog>>> =
    repository.observeStockLogs(currentUserId).toUiStateFlow("StoreVM", "Gagal memuat riwayat stok")

  val cashState: StateFlow<UiState<List<CashTransaction>>> =
    repository.observeCashTransactions(currentUserId).toUiStateFlow("StoreVM", "Gagal memuat arus kas")

  val customerDebtsState: StateFlow<UiState<List<CustomerDebt>>> =
    repository.observeCustomerDebts(currentUserId).toUiStateFlow("StoreVM", "Gagal memuat utang pelanggan")

  val supplierDebtsState: StateFlow<UiState<List<SupplierDebt>>> =
    repository.observeSupplierDebts(currentUserId).toUiStateFlow("StoreVM", "Gagal memuat utang supplier")

  val activitiesState: StateFlow<UiState<List<ActivityEntry>>> =
    repository.observeActivities(currentUserId).toUiStateFlow("StoreVM", "Gagal memuat aktivitas")

  val storeBundleState: StateFlow<UiState<StoreDataBundle>> =
    combine(
      combine(productsState, salesState, purchasesState, stockLogsState) { p, s, pur, log ->
        listOf(p, s, pur, log)
      },
      combine(cashState, customerDebtsState, supplierDebtsState, activitiesState) { c, cd, sd, a ->
        listOf(c, cd, sd, a)
      },
    ) { firstHalf, secondHalf ->
      val allStates = firstHalf + secondHalf
      val firstError = allStates.filterIsInstance<UiState.Error>().firstOrNull()
      if (firstError != null) {
        UiState.Error(firstError.message)
      } else if (allStates.any { it is UiState.Loading }) {
        UiState.Loading
      } else {
        @Suppress("UNCHECKED_CAST")
        UiState.Success(
          StoreDataBundle(
            products = (firstHalf[0] as UiState.Success<List<Product>>).data,
            sales = (firstHalf[1] as UiState.Success<List<Sale>>).data,
            purchases = (firstHalf[2] as UiState.Success<List<StockPurchase>>).data,
            stockLogs = (firstHalf[3] as UiState.Success<List<StockLog>>).data,
            cashTransactions = (secondHalf[0] as UiState.Success<List<CashTransaction>>).data,
            customerDebts = (secondHalf[1] as UiState.Success<List<CustomerDebt>>).data,
            supplierDebts = (secondHalf[2] as UiState.Success<List<SupplierDebt>>).data,
            activities = (secondHalf[3] as UiState.Success<List<ActivityEntry>>).data,
          )
        )
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading,
    )

  fun computeDashboard(bundle: StoreDataBundle): DashboardSnapshot =
    Companion.computeDashboard(bundle)

  fun computeProfitLoss(bundle: StoreDataBundle): ProfitLossSummary =
    Companion.computeProfitLoss(bundle)

  fun computeFinancialHealth(bundle: StoreDataBundle): FinancialHealthReport =
    Companion.computeFinancialHealth(bundle)

  companion object {
    private const val STOP_TIMEOUT_MILLIS = 5000L

    fun computeDashboard(bundle: StoreDataBundle): DashboardSnapshot {
      val todayStart = startOfTodayMillis()
      val monthStart = startOfMonthMillis()

      val todaySales = bundle.sales.filter { it.occurredAtMillis >= todayStart }
    val monthSales = bundle.sales.filter { it.occurredAtMillis >= monthStart }

    val totalIncome = bundle.cashTransactions.filter { it.direction == "MASUK" }.sumOf { it.amount }
    val totalExpense = bundle.cashTransactions.filter { it.direction == "KELUAR" }.sumOf { it.amount }

    val totalCustomerDebt =
      bundle.customerDebts.filter { it.status == "BELUM_LUNAS" }.sumOf { it.remainingAmount }
    val totalSupplierDebt =
      bundle.supplierDebts.filter { it.status == "BELUM_LUNAS" }.sumOf { it.remainingAmount }

    val uniqueNamedCustomers =
      bundle.sales
        .mapNotNull { it.customerName.trim().lowercase().takeIf(String::isNotEmpty) }
        .distinct()
        .size

    val chartPoints =
      (6 downTo 0).map { daysAgo ->
        val dayStart = startOfDaysAgoMillis(daysAgo)
        val dayEnd = if (daysAgo == 0) Long.MAX_VALUE else startOfDaysAgoMillis(daysAgo - 1)
        val income =
          bundle.cashTransactions
            .filter { it.direction == "MASUK" && it.occurredAtMillis in dayStart until dayEnd }
            .sumOf { it.amount }
        val expense =
          bundle.cashTransactions
            .filter { it.direction == "KELUAR" && it.occurredAtMillis in dayStart until dayEnd }
            .sumOf { it.amount }
        val profit =
          bundle.sales
            .filter { it.occurredAtMillis in dayStart until dayEnd }
            .sumOf { it.totalProfit }
        DailyChartPoint(
          label = formatDayShort(dayStart).take(3),
          income = income,
          expense = expense,
          profit = profit,
        )
      }

    val health = computeFinancialHealth(bundle)

    return DashboardSnapshot(
      todaySalesTotal = todaySales.sumOf { it.totalAmount },
      totalIncome = totalIncome,
      totalExpense = totalExpense,
      todayProfit = todaySales.sumOf { it.totalProfit },
      monthProfit = monthSales.sumOf { it.totalProfit },
      availableBalance = totalIncome - totalExpense,
      totalCustomerDebt = totalCustomerDebt,
      totalSupplierDebt = totalSupplierDebt,
      totalStockUnits = bundle.products.sumOf { it.stock },
      totalProductCount = bundle.products.size,
      totalSalesCount = bundle.sales.size,
      todayCustomerCount = todaySales.size,
      uniqueNamedCustomers = uniqueNamedCustomers,
      lowStockProducts = bundle.products.filter { it.lowStockWarning }.sortedBy { it.stock },
      chartPoints = chartPoints,
      financialHealth = health,
    )
  }

  fun computeProfitLoss(bundle: StoreDataBundle): ProfitLossSummary {
    val totalOmzet = bundle.sales.sumOf { it.totalAmount }
    val totalCogs = bundle.sales.sumOf { it.totalCost }
    val grossProfit = totalOmzet - totalCogs

    val operationalExpenses =
      bundle.cashTransactions
        .filter {
          it.direction == "KELUAR" &&
            it.category !in listOf("Belanja Stok", "Pengambilan Uang Pribadi")
        }
        .sumOf { it.amount }

    val damagedLoss =
      bundle.activities
        .filter { it.type == ActivityType.BARANG_RUSAK.code }
        .sumOf { it.amount }

    val netProfit = grossProfit - operationalExpenses - damagedLoss
    val marginPct = if (totalOmzet > 0) (grossProfit.toDouble() / totalOmzet.toDouble()) * 100.0 else 0.0
    val markupPct = if (totalCogs > 0) (grossProfit.toDouble() / totalCogs.toDouble()) * 100.0 else 0.0

    val totalIncome = bundle.cashTransactions.filter { it.direction == "MASUK" }.sumOf { it.amount }
    val totalExpense = bundle.cashTransactions.filter { it.direction == "KELUAR" }.sumOf { it.amount }
    val unpaidReceivables =
      bundle.customerDebts.filter { it.status == "BELUM_LUNAS" }.sumOf { it.remainingAmount }

    val ranked =
      bundle.sales
        .flatMap { it.items }
        .groupBy { it.productName }
        .map { (name, items) ->
          ProductProfitRank(
            productName = name,
            qtySold = items.sumOf { it.quantity },
            revenue = items.sumOf { it.subtotal },
            profit = items.sumOf { it.profit },
          )
        }
        .sortedByDescending { it.profit }
        .take(5)

    return ProfitLossSummary(
      totalOmzet = totalOmzet,
      costOfGoodsSold = totalCogs,
      grossProfit = grossProfit,
      operationalExpenses = operationalExpenses,
      damagedGoodsLoss = damagedLoss,
      netProfit = netProfit,
      marginPercent = marginPct,
      markupPercent = markupPct,
      availableBalance = totalIncome - totalExpense,
      unpaidReceivables = unpaidReceivables,
      topProfitableProducts = ranked,
    )
  }

  fun computeFinancialHealth(bundle: StoreDataBundle): FinancialHealthReport {
    val pl = computeProfitLoss(bundle)
    val totalIncome = bundle.cashTransactions.filter { it.direction == "MASUK" }.sumOf { it.amount }
    val totalExpense = bundle.cashTransactions.filter { it.direction == "KELUAR" }.sumOf { it.amount }
    val balance = totalIncome - totalExpense
    val supplierDebt = bundle.supplierDebts.filter { it.status == "BELUM_LUNAS" }.sumOf { it.remainingAmount }
    val customerDebt = bundle.customerDebts.filter { it.status == "BELUM_LUNAS" }.sumOf { it.remainingAmount }
    val personalDraw =
      bundle.cashTransactions
        .filter { it.direction == "KELUAR" && it.category == "Pengambilan Uang Pribadi" }
        .sumOf { it.amount }
    val lowStockCount = bundle.products.count { it.lowStockWarning }
    val safeStockPct =
      if (bundle.products.isEmpty()) 100.0
      else ((bundle.products.size - lowStockCount).toDouble() / bundle.products.size.toDouble()) * 100.0

    var score = 100
    val advice = mutableListOf<FinancialAdvice>()

    // 1. Cash balance check
    if (balance < 0L) {
      score -= 30
      advice.add(
        FinancialAdvice(
          title = "Saldo Kas Defisit",
          detail = "Pengeluaran lebih besar dari pemasukan sebesar ${formatRupiah(-balance)}. Tahan pengeluaran non-pokok dan prioritaskan penjualan tunai.",
          priorityHigh = true,
        )
      )
    } else if (balance == 0L && bundle.sales.isNotEmpty()) {
      score -= 10
    }

    // 2. Net profit & margin check
    if (pl.totalOmzet > 0L) {
      if (pl.netProfit < 0L) {
        score -= 25
        advice.add(
          FinancialAdvice(
            title = "Usaha Mengalami Rugi Bersih",
            detail = "Laba kotor belum menutup biaya operasional & kerusakan. Evaluasi biaya rutin serta harga jual barang utama.",
            priorityHigh = true,
          )
        )
      } else if (pl.marginPercent < 10.0) {
        score -= 10
        advice.add(
          FinancialAdvice(
            title = "Margin Keuntungan Tipis (${formatPercent(pl.marginPercent)})",
            detail = "Pertimbangkan menaikkan harga jual sedikit pada produk laris atau mencari pemasok dengan harga modal lebih hemat.",
            priorityHigh = false,
          )
        )
      }
    }

    // 3. Supplier debt coverage
    if (supplierDebt > 0L && supplierDebt > balance.coerceAtLeast(0L)) {
      score -= 20
      advice.add(
        FinancialAdvice(
          title = "Utang Supplier Melebihi Kas",
          detail = "Utang supplier (${formatRupiah(supplierDebt)}) lebih besar dari kas tersedia (${formatRupiah(balance)}). Sisihkan 30-40% omzet harian untuk cicilan.",
          priorityHigh = true,
        )
      )
    }

    // 4. Customer receivables risk
    if (customerDebt > 0L && (customerDebt > balance.coerceAtLeast(1L) * 0.5 || (pl.totalOmzet > 0 && customerDebt > pl.totalOmzet * 0.35))) {
      score -= 15
      advice.add(
        FinancialAdvice(
          title = "Piutang Pelanggan Cukup Tinggi",
          detail = "Uang tertahan di utang pelanggan sebesar ${formatRupiah(customerDebt)}. Lakukan penagihan berkala dan batasi tempo baru.",
          priorityHigh = true,
        )
      )
    }

    // 5. Personal withdrawal check
    if (personalDraw > 0L && pl.netProfit > 0L && personalDraw > pl.netProfit * 0.6) {
      score -= 10
      advice.add(
        FinancialAdvice(
          title = "Pengambilan Pribadi Besar",
          detail = "Pengambilan pribadi (${formatRupiah(personalDraw)}) melebihi 60% laba bersih. Sisakan lebih banyak laba agar modal barang terus berputar.",
          priorityHigh = false,
        )
      )
    }

    // 6. Low stock / damaged goods check
    if (lowStockCount > 0) {
      score -= 5
      advice.add(
        FinancialAdvice(
          title = "$lowStockCount Barang Stok Menipis",
          detail = "Segera belanja stok untuk barang yang hampir habis agar pembeli tidak beralih ke toko lain.",
          priorityHigh = false,
        )
      )
    }
    if (pl.damagedGoodsLoss > 0L) {
      score -= 5
      advice.add(
        FinancialAdvice(
          title = "Kurangi Kerugian Barang Rusak",
          detail = "Tercatat kerugian barang rusak ${formatRupiah(pl.damagedGoodsLoss)}. Terapkan metode stok masuk duluan keluar duluan (FIFO).",
          priorityHigh = false,
        )
      )
    }

    if (advice.isEmpty()) {
      advice.add(
        FinancialAdvice(
          title = "Keuangan Toko Terjaga Baik",
          detail = if (bundle.sales.isEmpty()) {
            "Mulai catat barang, penjualan harian, dan pengeluaran untuk memantau skor kesehatan keuangan secara real-time."
          } else {
            "Kas mencukupi kewajiban dan margin laba sehat. Pertahankan pencatatan disiplin dan sisihkan dana cadangan toko 10% dari laba."
          },
          priorityHigh = false,
        )
      )
    }

    val clampedScore = score.coerceIn(0, 100)
    val (level, label) =
      when {
        clampedScore >= 80 -> HealthLevel.SEHAT to "Sangat Sehat"
        clampedScore >= 60 -> HealthLevel.CUKUP to "Cukup Sehat"
        clampedScore >= 40 -> HealthLevel.PERLU_PERHATIAN to "Perlu Perhatian"
        else -> HealthLevel.WASPADA to "Waspada"
      }

    val indicators =
      listOf(
        FinancialIndicator(
          label = "Saldo Kas vs Utang Supplier",
          valueText = "${formatRupiah(balance)} / ${formatRupiah(supplierDebt)}",
          isHealthy = balance >= supplierDebt,
        ),
        FinancialIndicator(
          label = "Margin Keuntungan Kotor",
          valueText = formatPercent(pl.marginPercent),
          isHealthy = pl.totalOmzet == 0L || pl.marginPercent >= 10.0,
        ),
        FinancialIndicator(
          label = "Piutang Pelanggan Aktif",
          valueText = formatRupiah(customerDebt),
          isHealthy = customerDebt <= balance.coerceAtLeast(0L),
        ),
        FinancialIndicator(
          label = "Ketersediaan Stok Aman",
          valueText = formatPercent(safeStockPct),
          isHealthy = lowStockCount == 0,
        ),
      )

    return FinancialHealthReport(
      score = clampedScore,
      statusLabel = label,
      level = level,
      indicators = indicators,
      adviceList = advice,
    )
    }
  }

  fun saveProduct(existing: Product?, updated: Product, onSuccess: () -> Unit = {}) {
    viewModelScope.launch {
      val res =
        if (existing == null) repository.addProduct(updated).map { }
        else repository.updateProduct(existing, updated)
      res
        .onSuccess {
          _messages.emit(if (existing == null) "Barang berhasil ditambahkan" else "Data barang diperbarui")
          onSuccess()
        }
        .onFailure { _messages.emit(it.message ?: "Gagal menyimpan barang") }
    }
  }

  fun deleteProduct(product: Product) {
    viewModelScope.launch {
      repository
        .deleteProduct(product.id)
        .onSuccess { _messages.emit("${product.name} dihapus") }
        .onFailure { _messages.emit(it.message ?: "Gagal menghapus barang") }
    }
  }

  fun adjustStock(product: Product, delta: Long, type: StockChangeType, note: String, onSuccess: () -> Unit = {}) {
    viewModelScope.launch {
      repository
        .adjustStock(product, delta, type, note)
        .onSuccess {
          _messages.emit("Stok ${product.name} diperbarui")
          onSuccess()
        }
        .onFailure { _messages.emit(it.message ?: "Gagal mengubah stok") }
    }
  }

  fun checkoutSale(
    cartLines: List<Pair<Product, Long>>,
    customerName: String,
    paymentMethod: PaymentMethod,
    paidAmount: Long,
    onCompleted: (Sale) -> Unit,
  ) {
    viewModelScope.launch {
      repository
        .recordSale(cartLines, customerName, paymentMethod, paidAmount)
        .onSuccess { sale ->
          _messages.emit("Penjualan ${sale.receiptNumber} tersimpan")
          onCompleted(sale)
        }
        .onFailure { _messages.emit(it.message ?: "Gagal menyimpan penjualan") }
    }
  }

  fun recordStockPurchase(
    product: Product,
    quantity: Long,
    buyPricePerUnit: Long,
    supplierName: String,
    paymentMethod: PaymentMethod,
    isPaidOff: Boolean,
    paidAmountInput: Long,
    notePhotoPath: String?,
    onSuccess: () -> Unit,
  ) {
    viewModelScope.launch {
      repository
        .recordStockPurchase(
          product = product,
          quantity = quantity,
          buyPricePerUnit = buyPricePerUnit,
          supplierName = supplierName,
          paymentMethod = paymentMethod,
          isPaidOff = isPaidOff,
          paidAmountInput = paidAmountInput,
          notePhotoPath = notePhotoPath,
        )
        .onSuccess {
          _messages.emit("Belanja stok ${product.name} tercatat & stok bertambah")
          onSuccess()
        }
        .onFailure { _messages.emit(it.message ?: "Gagal mencatat belanja stok") }
    }
  }

  fun recordCashTransaction(
    direction: String,
    category: String,
    amount: Long,
    paymentMethod: PaymentMethod,
    description: String,
    onSuccess: () -> Unit,
  ) {
    viewModelScope.launch {
      repository
        .recordCashTransaction(direction, category, amount, paymentMethod, description)
        .onSuccess {
          _messages.emit("Catatan keuangan tersimpan")
          onSuccess()
        }
        .onFailure { _messages.emit(it.message ?: "Gagal mencatat keuangan") }
    }
  }

  fun addCustomerDebt(
    name: String,
    phone: String,
    amount: Long,
    note: String,
    occurredAtMillis: Long,
    onSuccess: () -> Unit,
  ) {
    viewModelScope.launch {
      repository
        .addCustomerDebt(name, phone, amount, note, occurredAtMillis)
        .onSuccess {
          _messages.emit("Utang pelanggan $name dicatat")
          onSuccess()
        }
        .onFailure { _messages.emit(it.message ?: "Gagal mencatat utang pelanggan") }
    }
  }

  fun payCustomerDebt(
    debt: CustomerDebt,
    amount: Long,
    method: PaymentMethod,
    note: String,
    onSuccess: () -> Unit,
  ) {
    viewModelScope.launch {
      repository
        .payCustomerDebt(debt, amount, method, note)
        .onSuccess {
          _messages.emit("Pembayaran utang ${debt.customerName} tersimpan")
          onSuccess()
        }
        .onFailure { _messages.emit(it.message ?: "Gagal mencatat pembayaran") }
    }
  }

  fun updateCustomerDebtNote(debtId: String, note: String, onSuccess: () -> Unit) {
    viewModelScope.launch {
      repository
        .updateCustomerDebtNote(debtId, note)
        .onSuccess {
          _messages.emit("Catatan utang diperbarui")
          onSuccess()
        }
        .onFailure { _messages.emit(it.message ?: "Gagal memperbarui catatan utang") }
    }
  }

  fun addSupplierDebt(
    supplierName: String,
    itemDesc: String,
    amount: Long,
    note: String,
    occurredAtMillis: Long,
    onSuccess: () -> Unit,
  ) {
    viewModelScope.launch {
      repository
        .addSupplierDebt(supplierName, itemDesc, amount, note, occurredAtMillis)
        .onSuccess {
          _messages.emit("Utang supplier $supplierName dicatat")
          onSuccess()
        }
        .onFailure { _messages.emit(it.message ?: "Gagal mencatat utang supplier") }
    }
  }

  fun paySupplierDebt(
    debt: SupplierDebt,
    amount: Long,
    method: PaymentMethod,
    note: String,
    onSuccess: () -> Unit,
  ) {
    viewModelScope.launch {
      repository
        .paySupplierDebt(debt, amount, method, note)
        .onSuccess {
          _messages.emit("Pembayaran ke ${debt.supplierName} tersimpan")
          onSuccess()
        }
        .onFailure { _messages.emit(it.message ?: "Gagal mencatat cicilan supplier") }
    }
  }

  fun updateSupplierDebtNote(debtId: String, note: String, onSuccess: () -> Unit) {
    viewModelScope.launch {
      repository
        .updateSupplierDebtNote(debtId, note)
        .onSuccess {
          _messages.emit("Catatan utang supplier diperbarui")
          onSuccess()
        }
        .onFailure { _messages.emit(it.message ?: "Gagal memperbarui catatan") }
    }
  }

  fun deleteActivity(activityId: String) {
    viewModelScope.launch {
      repository
        .deleteActivity(activityId)
        .onSuccess { _messages.emit("Aktivitas dihapus") }
        .onFailure { _messages.emit(it.message ?: "Gagal menghapus aktivitas") }
    }
  }

  fun deleteSale(saleId: String) {
    viewModelScope.launch {
      repository
        .deleteSale(saleId)
        .onSuccess { _messages.emit("Riwayat penjualan dihapus") }
        .onFailure { _messages.emit(it.message ?: "Gagal menghapus riwayat penjualan") }
    }
  }

  fun deletePurchase(purchaseId: String) {
    viewModelScope.launch {
      repository
        .deletePurchase(purchaseId)
        .onSuccess { _messages.emit("Riwayat belanja dihapus") }
        .onFailure { _messages.emit(it.message ?: "Gagal menghapus riwayat belanja") }
    }
  }

  fun deleteStockLog(logId: String) {
    viewModelScope.launch {
      repository
        .deleteStockLog(logId)
        .onSuccess { _messages.emit("Riwayat mutasi stok dihapus") }
        .onFailure { _messages.emit(it.message ?: "Gagal menghapus mutasi stok") }
    }
  }

  fun deleteCashTransaction(txId: String) {
    viewModelScope.launch {
      repository
        .deleteCashTransaction(txId)
        .onSuccess { _messages.emit("Catatan kas dihapus") }
        .onFailure { _messages.emit(it.message ?: "Gagal menghapus catatan kas") }
    }
  }

  fun deleteCustomerDebt(debtId: String) {
    viewModelScope.launch {
      repository
        .deleteCustomerDebt(debtId)
        .onSuccess { _messages.emit("Catatan utang pelanggan dihapus") }
        .onFailure { _messages.emit(it.message ?: "Gagal menghapus catatan utang") }
    }
  }

  fun deleteSupplierDebt(debtId: String) {
    viewModelScope.launch {
      repository
        .deleteSupplierDebt(debtId)
        .onSuccess { _messages.emit("Catatan utang supplier dihapus") }
        .onFailure { _messages.emit(it.message ?: "Gagal menghapus catatan utang") }
    }
  }

  fun resetStoreData(includeProducts: Boolean, onSuccess: () -> Unit = {}) {
    viewModelScope.launch {
      repository
        .resetStoreData(includeProducts)
        .onSuccess { count ->
          _messages.emit(
            if (includeProducts) "Semua data toko berhasil direset ($count catatan dihapus)"
            else "Riwayat & keuangan direset ($count catatan dihapus)",
          )
          onSuccess()
        }
        .onFailure { _messages.emit(it.message ?: "Gagal mereset data toko") }
    }
  }
}

package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.StoreRepository
import com.example.ui.activity.AktivitasScreen
import com.example.ui.auth.signOut
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.finance.KeuanganScreen
import com.example.ui.inventory.BarangScreen
import com.example.ui.inventory.BelanjaStokModalSheet
import com.example.ui.pos.KasirScreen
import com.example.ui.theme.SemanticError
import com.example.viewmodel.StoreViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private data class NavDestination(
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val tag: String,
)

private val MAIN_DESTINATIONS =
  listOf(
    NavDestination(R.string.nav_beranda, Icons.Filled.Home, Icons.Outlined.Home, "nav_beranda"),
    NavDestination(R.string.nav_kasir, Icons.Filled.PointOfSale, Icons.Outlined.PointOfSale, "nav_kasir"),
    NavDestination(R.string.nav_barang, Icons.Filled.Inventory2, Icons.Outlined.Inventory2, "nav_barang"),
    NavDestination(R.string.nav_aktivitas, Icons.Filled.History, Icons.Outlined.History, "nav_aktivitas"),
    NavDestination(R.string.nav_keuangan, Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_keuangan"),
  )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(currentUserId: String) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

  val viewModel: StoreViewModel =
    viewModel(
      key = currentUserId,
      factory =
        viewModelFactory {
          initializer {
            val app =
              checkNotNull(this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) {
                "APPLICATION_KEY missing from CreationExtras"
              }
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            StoreViewModel(StoreRepository(db), currentUserId)
          }
        },
    )

  val bundleState by viewModel.storeBundleState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(viewModel) {
    viewModel.messages.collect { msg ->
      snackbarHostState.showSnackbar(msg)
    }
  }

  var currentTab by remember { mutableIntStateOf(0) }
  var keuanganSubTab by remember { mutableIntStateOf(0) }
  var showBelanjaStokSheet by remember { mutableStateOf(false) }
  var showAccountMenu by remember { mutableStateOf(false) }
  var showSignOutConfirm by remember { mutableStateOf(false) }
  var showResetDialog by remember { mutableStateOf(false) }

  if (currentTab != 0) {
    BackHandler { currentTab = 0 }
  }

  val userEmail = Firebase.auth.currentUser?.email ?: "Pemilik Toko"

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 680.dp

    Scaffold(
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      topBar = {
        TopAppBar(
          title = {
            Column {
              Text(
                text = stringResource(MAIN_DESTINATIONS[currentTab].labelRes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Buku Sembako",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          },
          actions = {
            IconButton(
              onClick = { showBelanjaStokSheet = true },
              modifier = Modifier.testTag("topbar_belanja_stok"),
            ) {
              Icon(
                imageVector = Icons.Outlined.AddShoppingCart,
                contentDescription = "Catat Barang Masuk / Belanja Stok",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
            Box {
              IconButton(
                onClick = { showAccountMenu = true },
                modifier = Modifier.testTag("account_menu_button"),
              ) {
                Box(
                  modifier =
                    Modifier
                      .size(34.dp)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.primaryContainer),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Akun toko",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
              DropdownMenu(
                expanded = showAccountMenu,
                onDismissRequest = { showAccountMenu = false },
              ) {
                DropdownMenuItem(
                  text = { Text(userEmail, style = MaterialTheme.typography.bodySmall) },
                  onClick = { showAccountMenu = false },
                )
                HorizontalDivider()
                DropdownMenuItem(
                  leadingIcon = {
                    Icon(
                      imageVector = Icons.Outlined.RestartAlt,
                      contentDescription = null,
                      tint = SemanticError,
                    )
                  },
                  text = { Text("Reset Data Toko", color = SemanticError) },
                  onClick = {
                    showAccountMenu = false
                    showResetDialog = true
                  },
                  modifier = Modifier.testTag("menu_reset_store"),
                )
                DropdownMenuItem(
                  leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
                  text = { Text(stringResource(R.string.sign_out)) },
                  onClick = {
                    showAccountMenu = false
                    showSignOutConfirm = true
                  },
                )
              }
            }
          },
          colors =
            TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.surface,
            ),
        )
      },
      bottomBar = {
        if (!useRail) {
          NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            MAIN_DESTINATIONS.forEachIndexed { idx, dest ->
              NavigationBarItem(
                selected = currentTab == idx,
                onClick = { currentTab = idx },
                icon = {
                  Icon(
                    imageVector = if (currentTab == idx) dest.selectedIcon else dest.unselectedIcon,
                    contentDescription = stringResource(dest.labelRes),
                  )
                },
                label = { Text(stringResource(dest.labelRes), maxLines = 1) },
                modifier = Modifier.testTag(dest.tag),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (useRail) {
          NavigationRail(
            modifier = Modifier.fillMaxHeight(),
            containerColor = MaterialTheme.colorScheme.surface,
          ) {
            Spacer(modifier = Modifier.height(12.dp))
            MAIN_DESTINATIONS.forEachIndexed { idx, dest ->
              NavigationRailItem(
                selected = currentTab == idx,
                onClick = { currentTab = idx },
                icon = {
                  Icon(
                    imageVector = if (currentTab == idx) dest.selectedIcon else dest.unselectedIcon,
                    contentDescription = stringResource(dest.labelRes),
                  )
                },
                label = { Text(stringResource(dest.labelRes)) },
                modifier = Modifier.testTag(dest.tag),
              )
            }
          }
        }

        Box(
          modifier = Modifier.weight(1f).fillMaxHeight(),
          contentAlignment = Alignment.TopCenter,
        ) {
          Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxSize()) {
            when (val state = bundleState) {
              UiState.Loading -> {
                Column(
                  modifier = Modifier.fillMaxSize(),
                  verticalArrangement = Arrangement.Center,
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  CircularProgressIndicator()
                  Spacer(modifier = Modifier.height(12.dp))
                  Text("Memuat data toko...", style = MaterialTheme.typography.bodyMedium)
                }
              }

              is UiState.Error -> {
                Column(
                  modifier = Modifier.fillMaxSize().padding(24.dp),
                  verticalArrangement = Arrangement.Center,
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Text(
                    text = "Terjadi kendala memuat data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                  )
                }
              }

              is UiState.Success -> {
                val bundle = state.data
                val dashboard = remember(bundle) { viewModel.computeDashboard(bundle) }
                val profitLoss = remember(bundle) { viewModel.computeProfitLoss(bundle) }

                when (currentTab) {
                  0 ->
                    DashboardScreen(
                      snapshot = dashboard,
                      onOpenKasir = { currentTab = 1 },
                      onOpenBelanjaStok = { showBelanjaStokSheet = true },
                      onOpenBarang = { currentTab = 2 },
                      onOpenKeuangan = { subIdx ->
                        keuanganSubTab = subIdx
                        currentTab = 4
                      },
                    )

                  1 ->
                    KasirScreen(
                      products = bundle.products,
                      sales = bundle.sales,
                      onCheckoutSale = { lines, customer, method, paid, onDone ->
                        viewModel.checkoutSale(lines, customer, method, paid, onDone)
                      },
                      onDeleteSale = { viewModel.deleteSale(it.id) },
                      onGoToAddProduct = { currentTab = 2 },
                    )

                  2 ->
                    BarangScreen(
                      products = bundle.products,
                      stockLogs = bundle.stockLogs,
                      purchases = bundle.purchases,
                      onSaveProduct = { existing, updated, onDone ->
                        viewModel.saveProduct(existing, updated, onDone)
                      },
                      onDeleteProduct = { viewModel.deleteProduct(it) },
                      onDeleteStockLog = { viewModel.deleteStockLog(it.id) },
                      onDeletePurchase = { viewModel.deletePurchase(it.id) },
                      onAdjustStock = { prod, delta, type, note, onDone ->
                        viewModel.adjustStock(prod, delta, type, note, onDone)
                      },
                      onOpenBelanjaStok = { showBelanjaStokSheet = true },
                    )

                  3 ->
                    AktivitasScreen(
                      activities = bundle.activities,
                      onDeleteActivity = { viewModel.deleteActivity(it.id) },
                    )

                  4 ->
                    KeuanganScreen(
                      selectedSubTab = keuanganSubTab,
                      onSelectSubTab = { keuanganSubTab = it },
                      bundle = bundle,
                      profitLossSummary = profitLoss,
                      financialHealth = dashboard.financialHealth,
                      onRecordCash = { dir, cat, amt, method, desc, onDone ->
                        viewModel.recordCashTransaction(dir, cat, amt, method, desc, onDone)
                      },
                      onDeleteCash = { viewModel.deleteCashTransaction(it.id) },
                      onAddCustomerDebt = { name, phone, amt, note, occurredAtMillis, onDone ->
                        viewModel.addCustomerDebt(name, phone, amt, note, occurredAtMillis, onDone)
                      },
                      onPayCustomerDebt = { debt, amt, method, note, onDone ->
                        viewModel.payCustomerDebt(debt, amt, method, note, onDone)
                      },
                      onUpdateCustomerDebtNote = { id, note, onDone ->
                        viewModel.updateCustomerDebtNote(id, note, onDone)
                      },
                      onDeleteCustomerDebt = { viewModel.deleteCustomerDebt(it.id) },
                      onAddSupplierDebt = { sup, desc, amt, note, occurredAtMillis, onDone ->
                        viewModel.addSupplierDebt(sup, desc, amt, note, occurredAtMillis, onDone)
                      },
                      onPaySupplierDebt = { debt, amt, method, note, onDone ->
                        viewModel.paySupplierDebt(debt, amt, method, note, onDone)
                      },
                      onUpdateSupplierDebtNote = { id, note, onDone ->
                        viewModel.updateSupplierDebtNote(id, note, onDone)
                      },
                      onDeleteSupplierDebt = { viewModel.deleteSupplierDebt(it.id) },
                    )
                }

                if (showBelanjaStokSheet) {
                  BelanjaStokModalSheet(
                    products = bundle.products,
                    onConfirmPurchase = { prod, qty, buyPrice, supplier, method, paidOff, paidAmt, photoPath ->
                      viewModel.recordStockPurchase(
                        product = prod,
                        quantity = qty,
                        buyPricePerUnit = buyPrice,
                        supplierName = supplier,
                        paymentMethod = method,
                        isPaidOff = paidOff,
                        paidAmountInput = paidAmt,
                        notePhotoPath = photoPath,
                        onSuccess = { showBelanjaStokSheet = false },
                      )
                    },
                    onDismiss = { showBelanjaStokSheet = false },
                    onNeedProduct = {
                      showBelanjaStokSheet = false
                      currentTab = 2
                    },
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (showResetDialog) {
    ResetStoreDialog(
      onConfirmReset = { includeProducts ->
        viewModel.resetStoreData(includeProducts) {
          showResetDialog = false
          currentTab = 0
        }
      },
      onDismiss = { showResetDialog = false },
    )
  }

  if (showSignOutConfirm) {
    AlertDialog(
      onDismissRequest = { showSignOutConfirm = false },
      title = { Text("Keluar dari Akun?") },
      text = { Text("Data toko Anda tetap tersimpan aman di akun Google ($userEmail).") },
      confirmButton = {
        Button(
          onClick = {
            showSignOutConfirm = false
            signOut(credentialManager, scope)
          },
        ) {
          Text("Keluar")
        }
      },
      dismissButton = {
        TextButton(onClick = { showSignOutConfirm = false }) { Text("Batal") }
      },
    )
  }
}

@Composable
private fun ResetStoreDialog(
  onConfirmReset: (includeProducts: Boolean) -> Unit,
  onDismiss: () -> Unit,
) {
  var includeProducts by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Outlined.RestartAlt,
        contentDescription = null,
        tint = SemanticError,
        modifier = Modifier.size(32.dp),
      )
    },
    title = { Text("Reset Data Toko", fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Pilih jenis reset data yang diinginkan. Data yang sudah dihapus tidak dapat dikembalikan.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          shape = RoundedCornerShape(12.dp),
          modifier =
            Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable { includeProducts = false },
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top,
          ) {
            RadioButton(selected = !includeProducts, onClick = { includeProducts = false })
            Spacer(modifier = Modifier.width(4.dp))
            Column {
              Text("Reset Riwayat & Keuangan Saja", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
              Text(
                text = "Menghapus transaksi penjualan, belanja, mutasi stok, kas, utang, & aktivitas. Daftar barang dipertahankan (stok di-nol-kan).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        Surface(
          color = SemanticError.copy(alpha = 0.08f),
          shape = RoundedCornerShape(12.dp),
          modifier =
            Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable { includeProducts = true },
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top,
          ) {
            RadioButton(selected = includeProducts, onClick = { includeProducts = true })
            Spacer(modifier = Modifier.width(4.dp))
            Column {
              Text("Reset Semua Data Toko", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = SemanticError)
              Text(
                text = "Menghapus seluruh daftar barang, riwayat transaksi, arus kas, utang, dan aktivitas hingga kosong.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        colors = ButtonDefaults.buttonColors(containerColor = SemanticError),
        onClick = { onConfirmReset(includeProducts) },
        modifier = Modifier.testTag("confirm_reset_store_button"),
      ) {
        Text("Ya, Reset Data")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Batal") }
    },
  )
}

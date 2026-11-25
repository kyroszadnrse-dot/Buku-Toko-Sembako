package com.example.ui.inventory

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.PaymentMethod
import com.example.model.Product
import com.example.model.StockChangeType
import com.example.model.StockLog
import com.example.model.StockPurchase
import com.example.model.StoreConstants
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MetricRowLine
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.RupiahNumberField
import com.example.ui.components.SearchInputBar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.NumericTextStyle
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticWarning
import com.example.util.formatDateTime
import com.example.util.formatPercent
import com.example.util.formatRupiah
import com.example.util.savePickedImageToInternalStorage
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarangScreen(
  products: List<Product>,
  stockLogs: List<StockLog>,
  purchases: List<StockPurchase>,
  onSaveProduct: (existing: Product?, updated: Product, onSuccess: () -> Unit) -> Unit,
  onDeleteProduct: (Product) -> Unit,
  onDeleteStockLog: (StockLog) -> Unit,
  onDeletePurchase: (StockPurchase) -> Unit,
  onAdjustStock: (Product, Long, StockChangeType, String, () -> Unit) -> Unit,
  onOpenBelanjaStok: () -> Unit,
) {
  var subTab by remember { mutableIntStateOf(0) } // 0 = Daftar Barang, 1 = Riwayat Stok & Harga, 2 = Riwayat Belanja Modal
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("Semua") }
  var onlyLowStock by remember { mutableStateOf(false) }

  var editingProduct by remember { mutableStateOf<Product?>(null) }
  var showAddEditDialog by remember { mutableStateOf(false) }
  var adjustingProduct by remember { mutableStateOf<Product?>(null) }
  var deleteTarget by remember { mutableStateOf<Product?>(null) }
  var deleteStockLogTarget by remember { mutableStateOf<StockLog?>(null) }
  var deletePurchaseTarget by remember { mutableStateOf<StockPurchase?>(null) }

  val lowStockCount = products.count { it.lowStockWarning }

  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Button(
        onClick = {
          editingProduct = null
          showAddEditDialog = true
        },
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.weight(1f).height(48.dp).testTag("add_product_button"),
      ) {
        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Tambah Barang", maxLines = 1)
      }
      OutlinedButton(
        onClick = onOpenBelanjaStok,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.weight(1f).height(48.dp).testTag("open_belanja_stok_button"),
      ) {
        Icon(Icons.Outlined.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Belanja Stok", maxLines = 1)
      }
    }

    SingleChoiceSegmentedButtonRow(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
      SegmentedButton(
        selected = subTab == 0,
        onClick = { subTab = 0 },
        shape = SegmentedButtonDefaults.itemShape(0, 3),
      ) {
        Text("Barang (${products.size})", maxLines = 1)
      }
      SegmentedButton(
        selected = subTab == 1,
        onClick = { subTab = 1 },
        shape = SegmentedButtonDefaults.itemShape(1, 3),
      ) {
        Text("Mutasi Stok", maxLines = 1)
      }
      SegmentedButton(
        selected = subTab == 2,
        onClick = { subTab = 2 },
        shape = SegmentedButtonDefaults.itemShape(2, 3),
      ) {
        Text("Belanja", maxLines = 1)
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    when (subTab) {
      0 -> {
        val filtered =
          products.filter { p ->
            val matchCat = selectedCategory == "Semua" || p.category == selectedCategory
            val matchSearch = searchQuery.isBlank() || p.name.contains(searchQuery, ignoreCase = true)
            val matchLow = !onlyLowStock || p.lowStockWarning
            matchCat && matchSearch && matchLow
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
              placeholder = "Cari nama barang sembako...",
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              item {
                FilterChip(
                  selected = onlyLowStock,
                  onClick = { onlyLowStock = !onlyLowStock },
                  label = { Text("Hampir Habis ($lowStockCount)") },
                )
              }
              items(listOf("Semua") + StoreConstants.PRODUCT_CATEGORIES) { cat ->
                FilterChip(
                  selected = selectedCategory == cat,
                  onClick = { selectedCategory = cat },
                  label = { Text(cat) },
                )
              }
            }
          }

          if (products.isEmpty()) {
            item {
              EmptyStateCard(
                title = "Daftar Barang Masih Kosong",
                description = "Tambahkan barang sembako Anda seperti Beras, Minyak Goreng, Gula, Telur, atau Mi Instan.",
                actionLabel = "Tambah Barang Pertama",
                onAction = {
                  editingProduct = null
                  showAddEditDialog = true
                },
              )
            }
          } else if (filtered.isEmpty()) {
            item {
              EmptyStateCard(
                title = "Tidak Ada Barang Cocok",
                description = "Coba ubah filter kategori atau kata kunci pencarian.",
              )
            }
          } else {
            items(filtered, key = { it.id }) { product ->
              ProductItemCard(
                product = product,
                onEdit = {
                  editingProduct = product
                  showAddEditDialog = true
                },
                onAdjustStock = { adjustingProduct = product },
                onDelete = { deleteTarget = product },
              )
            }
          }
        }
      }

      1 -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          if (stockLogs.isEmpty()) {
            item {
              EmptyStateCard(
                title = "Belum Ada Riwayat Stok",
                description = "Setiap barang masuk, penjualan, penyesuaian stok, dan perubahan harga modal tercatat di sini.",
              )
            }
          } else {
            items(stockLogs, key = { it.id }) { log ->
              StockLogCard(
                log = log,
                onDeleteClick = { deleteStockLogTarget = log },
              )
            }
          }
        }
      }

      2 -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          if (purchases.isEmpty()) {
            item {
              EmptyStateCard(
                title = "Belum Ada Catatan Belanja Modal",
                description = "Catat pembelian stok dari agen/supplier agar stok bertambah dan harga modal otomatis terbarui.",
                actionLabel = "Catat Belanja Stok",
                onAction = onOpenBelanjaStok,
              )
            }
          } else {
            items(purchases, key = { it.id }) { pur ->
              PurchaseHistoryCard(
                purchase = pur,
                onDeleteClick = { deletePurchaseTarget = pur },
              )
            }
          }
        }
      }
    }
  }

  if (showAddEditDialog) {
    ModalBottomSheet(
      onDismissRequest = { showAddEditDialog = false },
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
      ProductFormSheet(
        existing = editingProduct,
        onSave = { updated ->
          onSaveProduct(editingProduct, updated) { showAddEditDialog = false }
        },
        onCancel = { showAddEditDialog = false },
      )
    }
  }

  adjustingProduct?.let { prod ->
    AdjustStockDialog(
      product = prod,
      onConfirm = { delta, type, note ->
        onAdjustStock(prod, delta, type, note) { adjustingProduct = null }
      },
      onDismiss = { adjustingProduct = null },
    )
  }

  deleteTarget?.let { prod ->
    AlertDialog(
      onDismissRequest = { deleteTarget = null },
      title = { Text("Hapus Barang?") },
      text = { Text("Hapus \"${prod.name}\" dari daftar barang toko?") },
      confirmButton = {
        Button(
          onClick = {
            onDeleteProduct(prod)
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

  deleteStockLogTarget?.let { log ->
    AlertDialog(
      onDismissRequest = { deleteStockLogTarget = null },
      title = { Text("Hapus Riwayat Mutasi Stok?", fontWeight = FontWeight.Bold) },
      text = { Text("Hapus catatan mutasi stok \"${log.productName}\"?") },
      confirmButton = {
        Button(
          colors = ButtonDefaults.buttonColors(containerColor = SemanticError),
          onClick = {
            onDeleteStockLog(log)
            deleteStockLogTarget = null
          },
        ) {
          Text("Hapus")
        }
      },
      dismissButton = { TextButton(onClick = { deleteStockLogTarget = null }) { Text("Batal") } },
    )
  }

  deletePurchaseTarget?.let { pur ->
    AlertDialog(
      onDismissRequest = { deletePurchaseTarget = null },
      title = { Text("Hapus Riwayat Belanja?", fontWeight = FontWeight.Bold) },
      text = { Text("Hapus catatan belanja \"${pur.productName}\" (${formatRupiah(pur.totalCost)})?") },
      confirmButton = {
        Button(
          colors = ButtonDefaults.buttonColors(containerColor = SemanticError),
          onClick = {
            onDeletePurchase(pur)
            deletePurchaseTarget = null
          },
        ) {
          Text("Hapus")
        }
      },
      dismissButton = { TextButton(onClick = { deletePurchaseTarget = null }) { Text("Batal") } },
    )
  }
}

@Composable
private fun ProductItemCard(
  product: Product,
  onEdit: () -> Unit,
  onAdjustStock: () -> Unit,
  onDelete: () -> Unit,
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = MaterialTheme.shapes.medium,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        ProductThumbnail(
          photoPath = product.photoPath,
          category = product.category,
          contentDescription = product.name,
          size = 58.dp,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = product.name,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(6.dp))
            StatusBadge(
              text = product.category,
              containerColor = MaterialTheme.colorScheme.surfaceVariant,
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Jual ${formatRupiah(product.sellPrice)} • Modal ${formatRupiah(product.costPrice)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "Laba ${formatRupiah(product.profitPerUnit)}/${product.unit} (${formatPercent(product.marginPercent)})",
            style = MaterialTheme.typography.labelSmall,
            color = if (product.profitPerUnit >= 0) SemanticSuccess else SemanticError,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        StatusBadge(
          text = "Stok: ${product.stock} ${product.unit}" + if (product.lowStockWarning) " (Menipis)" else "",
          containerColor =
            if (product.stock == 0L) MaterialTheme.colorScheme.errorContainer
            else if (product.lowStockWarning) Color(0xFFFEF3C7)
            else MaterialTheme.colorScheme.primaryContainer,
          contentColor =
            if (product.stock == 0L) MaterialTheme.colorScheme.error
            else if (product.lowStockWarning) Color(0xFF92400E)
            else MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Row {
          IconButton(onClick = onAdjustStock) {
            Icon(Icons.Outlined.Tune, contentDescription = "Atur stok ${product.name}", tint = MaterialTheme.colorScheme.primary)
          }
          IconButton(onClick = onEdit) {
            Icon(Icons.Outlined.Edit, contentDescription = "Edit ${product.name}")
          }
          IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Hapus ${product.name}", tint = SemanticError)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductFormSheet(
  existing: Product?,
  onSave: (Product) -> Unit,
  onCancel: () -> Unit,
) {
  val context = LocalContext.current
  var name by remember { mutableStateOf(existing?.name ?: "") }
  var category by remember { mutableStateOf(existing?.category ?: StoreConstants.PRODUCT_CATEGORIES.first()) }
  var unit by remember { mutableStateOf(existing?.unit ?: StoreConstants.PRODUCT_UNITS.first()) }
  var costText by remember { mutableStateOf(existing?.costPrice?.toString() ?: "") }
  var sellText by remember { mutableStateOf(existing?.sellPrice?.toString() ?: "") }
  var stockText by remember { mutableStateOf(existing?.stock?.toString() ?: "0") }
  var minStockText by remember { mutableStateOf(existing?.minStock?.toString() ?: "5") }
  var photoPath by remember { mutableStateOf(existing?.photoPath) }

  var catExpanded by remember { mutableStateOf(false) }
  var unitExpanded by remember { mutableStateOf(false) }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      uri?.let {
        val saved = savePickedImageToInternalStorage(context, it, "product_photos")
        if (saved != null) photoPath = saved
      }
    }

  val costVal = costText.toLongOrNull() ?: 0L
  val sellVal = sellText.toLongOrNull() ?: 0L
  val profitUnit = sellVal - costVal
  val marginPct = if (sellVal > 0) (profitUnit.toDouble() / sellVal.toDouble()) * 100.0 else 0.0
  val isValid = name.trim().isNotEmpty() && sellText.isNotBlank() && costText.isNotBlank()

  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      text = if (existing == null) "Tambah Barang Baru" else "Edit Barang",
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
    )

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      ProductThumbnail(
        photoPath = photoPath,
        category = category,
        contentDescription = "Foto barang",
        size = 68.dp,
      )
      OutlinedButton(
        onClick = {
          photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        shape = MaterialTheme.shapes.medium,
      ) {
        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(if (photoPath == null) "Pilih Foto Barang" else "Ganti Foto")
      }
    }

    OutlinedTextField(
      value = name,
      onValueChange = { name = it.take(100) },
      label = { Text("Nama Barang") },
      placeholder = { Text("Contoh: Minyak Goreng 1L") },
      singleLine = true,
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.fillMaxWidth(),
    )

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      ExposedDropdownMenuBox(
        expanded = catExpanded,
        onExpandedChange = { catExpanded = !catExpanded },
        modifier = Modifier.weight(1.3f),
      ) {
        OutlinedTextField(
          value = category,
          onValueChange = {},
          readOnly = true,
          label = { Text("Kategori") },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
          StoreConstants.PRODUCT_CATEGORIES.forEach { option ->
            DropdownMenuItem(
              text = { Text(option) },
              onClick = {
                category = option
                catExpanded = false
              },
            )
          }
        }
      }

      ExposedDropdownMenuBox(
        expanded = unitExpanded,
        onExpandedChange = { unitExpanded = !unitExpanded },
        modifier = Modifier.weight(1f),
      ) {
        OutlinedTextField(
          value = unit,
          onValueChange = {},
          readOnly = true,
          label = { Text("Satuan") },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
          StoreConstants.PRODUCT_UNITS.forEach { option ->
            DropdownMenuItem(
              text = { Text(option) },
              onClick = {
                unit = option
                unitExpanded = false
              },
            )
          }
        }
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      RupiahNumberField(
        value = costText,
        onValueChange = { costText = it },
        label = "Harga Modal",
        modifier = Modifier.weight(1f),
      )
      RupiahNumberField(
        value = sellText,
        onValueChange = { sellText = it },
        label = "Harga Jual",
        modifier = Modifier.weight(1f),
      )
    }

    if (sellVal > 0 || costVal > 0) {
      MetricRowLine(
        label = "Keuntungan per $unit",
        valueText = "${formatRupiah(profitUnit)} (${formatPercent(marginPct)})",
        valueColor = if (profitUnit >= 0) SemanticSuccess else SemanticError,
        bold = true,
      )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      OutlinedTextField(
        value = stockText,
        onValueChange = { stockText = it.filter { c -> c.isDigit() }.take(8) },
        label = { Text("Stok Tersedia") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.weight(1f),
      )
      OutlinedTextField(
        value = minStockText,
        onValueChange = { minStockText = it.filter { c -> c.isDigit() }.take(6) },
        label = { Text("Batas Minimum") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.weight(1f),
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(50.dp)) {
        Text("Batal")
      }
      Button(
        onClick = {
          onSave(
            Product(
              id = existing?.id ?: "",
              name = name.trim(),
              category = category,
              costPrice = costVal,
              sellPrice = sellVal,
              stock = stockText.toLongOrNull() ?: 0L,
              unit = unit,
              minStock = minStockText.toLongOrNull() ?: 5L,
              photoPath = photoPath,
            )
          )
        },
        enabled = isValid,
        modifier = Modifier.weight(1.4f).height(50.dp).testTag("save_product_button"),
      ) {
        Text("Simpan Barang")
      }
    }
  }
}

@Composable
private fun AdjustStockDialog(
  product: Product,
  onConfirm: (delta: Long, type: StockChangeType, note: String) -> Unit,
  onDismiss: () -> Unit,
) {
  var selectedType by remember { mutableStateOf(StockChangeType.TAMBAH_MANUAL) }
  var qtyText by remember { mutableStateOf("1") }
  var note by remember { mutableStateOf("") }
  val qtyVal = qtyText.toLongOrNull() ?: 0L

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Atur Stok • ${product.name}", fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Stok saat ini: ${product.stock} ${product.unit}", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(
            StockChangeType.TAMBAH_MANUAL to "+ Tambah",
            StockChangeType.KURANG_MANUAL to "- Kurangi",
            StockChangeType.BARANG_RUSAK to "Rusak",
          ).forEach { (type, label) ->
            FilterChip(
              selected = selectedType == type,
              onClick = { selectedType = type },
              label = { Text(label) },
            )
          }
        }
        OutlinedTextField(
          value = qtyText,
          onValueChange = { qtyText = it.filter { c -> c.isDigit() }.take(7) },
          label = { Text("Jumlah (${product.unit})") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = note,
          onValueChange = { note = it.take(200) },
          label = { Text("Keterangan (Opsional)") },
          placeholder = {
            Text(if (selectedType == StockChangeType.BARANG_RUSAK) "Contoh: Kemasan bocor / kadaluarsa" else "Contoh: Koreksi hitung stok")
          },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        if (selectedType == StockChangeType.BARANG_RUSAK && qtyVal > 0L) {
          Text(
            text = "Kerugian tercatat: ${formatRupiah(qtyVal * product.costPrice)}",
            style = MaterialTheme.typography.labelMedium,
            color = SemanticError,
          )
        }
      }
    },
    confirmButton = {
      Button(
        enabled = qtyVal > 0L && (selectedType == StockChangeType.TAMBAH_MANUAL || qtyVal <= product.stock),
        onClick = {
          val delta = if (selectedType == StockChangeType.TAMBAH_MANUAL) qtyVal else -qtyVal
          onConfirm(delta, selectedType, note)
        },
      ) {
        Text("Simpan")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Batal") }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BelanjaStokModalSheet(
  products: List<Product>,
  onConfirmPurchase: (
    product: Product,
    quantity: Long,
    buyPrice: Long,
    supplierName: String,
    method: PaymentMethod,
    isPaidOff: Boolean,
    paidAmount: Long,
    notePhotoPath: String?,
  ) -> Unit,
  onDismiss: () -> Unit,
  onNeedProduct: () -> Unit,
) {
  val context = LocalContext.current
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
  ) {
    if (products.isEmpty()) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text("Belum Ada Barang", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
          "Tambahkan barang di menu Barang terlebih dahulu sebelum mencatat pembelian stok.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onNeedProduct) { Text("Ke Menu Barang") }
      }
      return@ModalBottomSheet
    }

    var selectedProduct by remember { mutableStateOf(products.first()) }
    var productMenuExpanded by remember { mutableStateOf(false) }
    var qtyText by remember { mutableStateOf("10") }
    var buyPriceText by remember { mutableStateOf(selectedProduct.costPrice.toString()) }
    var supplierName by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.TUNAI) }
    var isPaidOff by remember { mutableStateOf(true) }
    var paidAmountText by remember { mutableStateOf("") }
    var notePhotoPath by remember { mutableStateOf<String?>(null) }

    val qty = qtyText.toLongOrNull() ?: 0L
    val buyPrice = buyPriceText.toLongOrNull() ?: 0L
    val totalModal = qty * buyPrice
    val effectivePaid = if (isPaidOff && paymentMethod != PaymentMethod.UTANG) totalModal else (paidAmountText.toLongOrNull() ?: 0L)
    val needsSupplier = (!isPaidOff || paymentMethod == PaymentMethod.UTANG) && supplierName.trim().isEmpty()

    val notaPickerLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
          val saved = savePickedImageToInternalStorage(context, it, "purchase_notes")
          if (saved != null) notePhotoPath = saved
        }
      }

    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text("Barang Masuk / Belanja Modal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

      ExposedDropdownMenuBox(
        expanded = productMenuExpanded,
        onExpandedChange = { productMenuExpanded = !productMenuExpanded },
      ) {
        OutlinedTextField(
          value = "${selectedProduct.name} (Stok: ${selectedProduct.stock} ${selectedProduct.unit})",
          onValueChange = {},
          readOnly = true,
          label = { Text("Pilih Barang") },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productMenuExpanded) },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(
          expanded = productMenuExpanded,
          onDismissRequest = { productMenuExpanded = false },
        ) {
          products.forEach { item ->
            DropdownMenuItem(
              text = { Text("${item.name} • Modal ${formatRupiah(item.costPrice)}") },
              onClick = {
                selectedProduct = item
                buyPriceText = item.costPrice.toString()
                productMenuExpanded = false
              },
            )
          }
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = qtyText,
          onValueChange = { qtyText = it.filter { c -> c.isDigit() }.take(7) },
          label = { Text("Jumlah (${selectedProduct.unit})") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f),
        )
        RupiahNumberField(
          value = buyPriceText,
          onValueChange = { buyPriceText = it },
          label = "Harga Beli / ${selectedProduct.unit}",
          modifier = Modifier.weight(1.3f),
        )
      }

      if (buyPrice != selectedProduct.costPrice && buyPrice > 0L) {
        Text(
          text = "Harga modal barang akan diperbarui: ${formatRupiah(selectedProduct.costPrice)} → ${formatRupiah(buyPrice)}",
          style = MaterialTheme.typography.labelMedium,
          color = SemanticWarning,
        )
      }

      Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          MetricRowLine("Total Modal Belanja", formatRupiah(totalModal), bold = true)
        }
      }

      OutlinedTextField(
        value = supplierName,
        onValueChange = { supplierName = it.take(100) },
        label = { Text(if (!isPaidOff || paymentMethod == PaymentMethod.UTANG) "Nama Supplier (Wajib)" else "Supplier / Tempat Beli") },
        placeholder = { Text("Contoh: Agen Sembako Jaya") },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
      )

      Text("Metode Pembayaran", style = MaterialTheme.typography.labelLarge)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PaymentMethod.entries.forEach { method ->
          FilterChip(
            selected = paymentMethod == method,
            onClick = {
              paymentMethod = method
              if (method == PaymentMethod.UTANG) isPaidOff = false
            },
            label = { Text(method.label) },
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Status Pembayaran: ${if (isPaidOff) "Lunas" else "Utang"}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Switch(
          checked = isPaidOff,
          onCheckedChange = {
            isPaidOff = it
            if (!it && paymentMethod != PaymentMethod.UTANG) {
              paidAmountText = "0"
            }
          },
        )
      }

      if (!isPaidOff) {
        RupiahNumberField(
          value = paidAmountText,
          onValueChange = { paidAmountText = it },
          label = "Uang Muka / DP (Opsional)",
          supportingText = "Sisa ${formatRupiah((totalModal - effectivePaid).coerceAtLeast(0L))} masuk ke Utang Supplier",
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
          onClick = {
            notaPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
          },
          shape = MaterialTheme.shapes.medium,
        ) {
          Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (notePhotoPath == null) "Foto Nota (Opsional)" else "Ganti Foto Nota")
        }
        notePhotoPath?.let { path ->
          val file = File(path)
          if (file.exists()) {
            AsyncImage(
              model = file,
              contentDescription = "Foto nota belanja",
              contentScale = ContentScale.Crop,
              modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
            )
          }
        }
      }

      if (needsSupplier) {
        Text(
          text = "Isi nama supplier untuk mencatat utang pembelian.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(50.dp)) {
          Text("Batal")
        }
        Button(
          onClick = {
            onConfirmPurchase(
              selectedProduct,
              qty,
              buyPrice,
              supplierName,
              paymentMethod,
              isPaidOff,
              effectivePaid,
              notePhotoPath,
            )
          },
          enabled = qty > 0L && !needsSupplier,
          modifier = Modifier.weight(1.4f).height(50.dp).testTag("confirm_purchase_button"),
        ) {
          Text("Simpan Belanja")
        }
      }
    }
  }
}

@Composable
private fun StockLogCard(
  log: StockLog,
  onDeleteClick: () -> Unit,
) {
  val type = StockChangeType.fromCode(log.changeType)
  val isPlus = log.quantityDelta > 0L
  val badgeColor =
    when (type) {
      StockChangeType.MASUK, StockChangeType.TAMBAH_MANUAL -> SemanticSuccess
      StockChangeType.BARANG_RUSAK -> SemanticError
      StockChangeType.PERUBAHAN_HARGA -> MaterialTheme.colorScheme.tertiary
      else -> SemanticWarning
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
      Icon(Icons.Outlined.History, contentDescription = null, tint = badgeColor)
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(log.productName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(log.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatDateTime(log.occurredAtMillis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Column(horizontalAlignment = Alignment.End) {
        if (log.quantityDelta != 0L) {
          Text(
            text = "${if (isPlus) "+" else ""}${log.quantityDelta}",
            style = NumericTextStyle.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize),
            color = badgeColor,
          )
        }
        Text("Stok: ${log.stockAfter}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      IconButton(onClick = onDeleteClick, modifier = Modifier.size(48.dp)) {
        Icon(
          imageVector = Icons.Outlined.DeleteOutline,
          contentDescription = "Hapus mutasi stok ${log.productName}",
          tint = SemanticError,
          modifier = Modifier.size(20.dp),
        )
      }
    }
  }
}

@Composable
private fun PurchaseHistoryCard(
  purchase: StockPurchase,
  onDeleteClick: () -> Unit,
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = MaterialTheme.shapes.medium,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "${purchase.productName} (+${purchase.quantity} ${purchase.unit})",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = (purchase.supplierName.ifBlank { "Belanja Toko" }) + " • ${formatDateTime(purchase.occurredAtMillis)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = "Harga beli ${formatRupiah(purchase.buyPrice)}/${purchase.unit}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = formatRupiah(purchase.totalCost),
          style = NumericTextStyle.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize),
        )
        Spacer(modifier = Modifier.height(4.dp))
        StatusBadge(
          text = if (purchase.paymentStatus == "LUNAS") "Lunas" else "Utang",
          containerColor = if (purchase.paymentStatus == "LUNAS") MaterialTheme.colorScheme.primaryContainer else Color(0xFFFEF3C7),
          contentColor = if (purchase.paymentStatus == "LUNAS") MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF92400E),
        )
      }
      IconButton(onClick = onDeleteClick, modifier = Modifier.size(48.dp)) {
        Icon(
          imageVector = Icons.Outlined.DeleteOutline,
          contentDescription = "Hapus riwayat belanja ${purchase.productName}",
          tint = SemanticError,
          modifier = Modifier.size(20.dp),
        )
      }
    }
  }
}

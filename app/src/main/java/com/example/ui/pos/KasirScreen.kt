package com.example.ui.pos

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ShoppingCartCheckout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.PaymentMethod
import com.example.model.Product
import com.example.model.Sale
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
import com.example.util.sharePlainText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KasirScreen(
  products: List<Product>,
  sales: List<Sale>,
  onCheckoutSale: (
    cartLines: List<Pair<Product, Long>>,
    customerName: String,
    paymentMethod: PaymentMethod,
    paidAmount: Long,
    onCompleted: (Sale) -> Unit,
  ) -> Unit,
  onDeleteSale: (Sale) -> Unit,
  onGoToAddProduct: () -> Unit,
) {
  var subTab by remember { mutableIntStateOf(0) } // 0 = Kasir, 1 = Riwayat Penjualan
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("Semua") }
  val cartQuantities = remember { mutableStateMapOf<String, Long>() }
  var showCheckoutSheet by remember { mutableStateOf(false) }
  var receiptSale by remember { mutableStateOf<Sale?>(null) }
  var deleteSaleTarget by remember { mutableStateOf<Sale?>(null) }

  val cartLines =
    products.mapNotNull { product ->
      val qty = cartQuantities[product.id] ?: 0L
      if (qty > 0L) product to qty.coerceAtMost(product.stock) else null
    }
  val cartTotal = cartLines.sumOf { (p, q) -> p.sellPrice * q }
  val cartCost = cartLines.sumOf { (p, q) -> p.costPrice * q }
  val cartProfit = cartTotal - cartCost
  val cartCount = cartLines.sumOf { it.second }

  Column(modifier = Modifier.fillMaxSize()) {
    SingleChoiceSegmentedButtonRow(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
      SegmentedButton(
        selected = subTab == 0,
        onClick = { subTab = 0 },
        shape = SegmentedButtonDefaults.itemShape(0, 2),
      ) {
        Text("Kasir Penjualan")
      }
      SegmentedButton(
        selected = subTab == 1,
        onClick = { subTab = 1 },
        shape = SegmentedButtonDefaults.itemShape(1, 2),
      ) {
        Text("Riwayat (${sales.size})")
      }
    }

    if (subTab == 0) {
      Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        val filteredProducts =
          products.filter { product ->
            val matchCat = selectedCategory == "Semua" || product.category == selectedCategory
            val matchQuery =
              searchQuery.isBlank() || product.name.contains(searchQuery, ignoreCase = true)
            matchCat && matchQuery
          }

        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = if (cartCount > 0) 96.dp else 16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          item {
            SearchInputBar(
              query = searchQuery,
              onQueryChange = { searchQuery = it },
              placeholder = "Cari barang untuk dijual...",
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                title = "Belum Ada Barang untuk Dijual",
                description = "Tambahkan daftar barang sembako terlebih dahulu agar bisa dipilih di kasir.",
                actionLabel = "Tambah Barang",
                onAction = onGoToAddProduct,
              )
            }
          } else if (filteredProducts.isEmpty()) {
            item {
              EmptyStateCard(
                title = "Barang Tidak Ditemukan",
                description = "Coba kata kunci atau kategori lain.",
              )
            }
          } else {
            items(filteredProducts, key = { it.id }) { product ->
              val qty = cartQuantities[product.id] ?: 0L
              KasirProductRow(
                product = product,
                qtyInCart = qty,
                onIncrease = {
                  if (qty < product.stock) {
                    cartQuantities[product.id] = qty + 1L
                  }
                },
                onDecrease = {
                  if (qty > 1L) cartQuantities[product.id] = qty - 1L
                  else cartQuantities.remove(product.id)
                },
              )
            }
          }
        }

        if (cartCount > 0L) {
          Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = MaterialTheme.shapes.large,
            shadowElevation = 6.dp,
            modifier =
              Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .clip(MaterialTheme.shapes.large)
                .clickable { showCheckoutSheet = true }
                .testTag("kasir_checkout_bar"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column {
                Text(
                  text = "$cartCount barang • Laba ${formatRupiah(cartProfit)}",
                  style = MaterialTheme.typography.labelMedium,
                  color = Color.White.copy(alpha = 0.88f),
                )
                Text(
                  text = formatRupiah(cartTotal),
                  style = NumericTextStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize),
                  color = Color.White,
                )
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "Bayar",
                  style = MaterialTheme.typography.titleMedium,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                  imageVector = Icons.Outlined.ShoppingCartCheckout,
                  contentDescription = "Lanjut pembayaran",
                  tint = Color.White,
                )
              }
            }
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (sales.isEmpty()) {
          item {
            EmptyStateCard(
              title = "Belum Ada Transaksi Penjualan",
              description = "Semua transaksi kasir dan struk akan tersimpan di halaman ini.",
            )
          }
        } else {
          items(sales, key = { it.id }) { sale ->
            SaleHistoryCard(
              sale = sale,
              onClick = { receiptSale = sale },
              onDeleteClick = { deleteSaleTarget = sale },
            )
          }
        }
      }
    }
  }

  if (showCheckoutSheet && cartLines.isNotEmpty()) {
    ModalBottomSheet(
      onDismissRequest = { showCheckoutSheet = false },
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
      CheckoutSheetContent(
        cartLines = cartLines,
        totalAmount = cartTotal,
        totalCost = cartCost,
        totalProfit = cartProfit,
        onConfirm = { customerName, method, paidAmount ->
          onCheckoutSale(cartLines, customerName, method, paidAmount) { completedSale ->
            cartQuantities.clear()
            showCheckoutSheet = false
            receiptSale = completedSale
          }
        },
        onCancel = { showCheckoutSheet = false },
      )
    }
  }

  receiptSale?.let { sale ->
    ReceiptDialog(sale = sale, onDismiss = { receiptSale = null })
  }

  deleteSaleTarget?.let { target ->
    AlertDialog(
      onDismissRequest = { deleteSaleTarget = null },
      title = { Text("Hapus Riwayat Penjualan?", fontWeight = FontWeight.Bold) },
      text = {
        Text("Hapus struk ${target.receiptNumber} (${formatRupiah(target.totalAmount)}) dari riwayat penjualan?")
      },
      confirmButton = {
        Button(
          colors = ButtonDefaults.buttonColors(containerColor = SemanticError),
          onClick = {
            onDeleteSale(target)
            deleteSaleTarget = null
          },
        ) {
          Text("Hapus")
        }
      },
      dismissButton = {
        TextButton(onClick = { deleteSaleTarget = null }) { Text("Batal") }
      },
    )
  }
}

@Composable
private fun KasirProductRow(
  product: Product,
  qtyInCart: Long,
  onIncrease: () -> Unit,
  onDecrease: () -> Unit,
) {
  val outOfStock = product.stock <= 0L
  Card(
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (qtyInCart > 0L) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
          else MaterialTheme.colorScheme.surface,
      ),
    border =
      if (qtyInCart > 0L) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
      else null,
    shape = MaterialTheme.shapes.medium,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ProductThumbnail(
        photoPath = product.photoPath,
        category = product.category,
        contentDescription = product.name,
        size = 54.dp,
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = product.name,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${formatRupiah(product.sellPrice)} / ${product.unit}",
          style = NumericTextStyle.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize),
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = "Modal ${formatRupiah(product.costPrice)} • Stok ${product.stock} ${product.unit}",
          style = MaterialTheme.typography.bodySmall,
          color = if (outOfStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (outOfStock) {
        StatusBadge(
          text = "Habis",
          containerColor = MaterialTheme.colorScheme.errorContainer,
          contentColor = MaterialTheme.colorScheme.error,
        )
      } else if (qtyInCart == 0L) {
        Surface(
          color = MaterialTheme.colorScheme.primary,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onIncrease),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Rounded.Add, contentDescription = "Tambah ${product.name}", tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Pilih", style = MaterialTheme.typography.labelLarge, color = Color.White)
          }
        }
      } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onDecrease,
            modifier =
              Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
          ) {
            Icon(Icons.Rounded.Remove, contentDescription = "Kurangi ${product.name}")
          }
          Text(
            text = "$qtyInCart",
            style = NumericTextStyle.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize),
            modifier = Modifier.padding(horizontal = 10.dp),
          )
          IconButton(
            onClick = onIncrease,
            enabled = qtyInCart < product.stock,
            modifier =
              Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
          ) {
            Icon(Icons.Rounded.Add, contentDescription = "Tambah ${product.name}", tint = Color.White)
          }
        }
      }
    }
  }
}

@Composable
private fun CheckoutSheetContent(
  cartLines: List<Pair<Product, Long>>,
  totalAmount: Long,
  totalCost: Long,
  totalProfit: Long,
  onConfirm: (customerName: String, method: PaymentMethod, paidAmount: Long) -> Unit,
  onCancel: () -> Unit,
) {
  var customerName by remember { mutableStateOf("") }
  var selectedMethod by remember { mutableStateOf(PaymentMethod.TUNAI) }
  var paidText by remember { mutableStateOf(totalAmount.toString()) }

  val paidVal =
    if (selectedMethod == PaymentMethod.UTANG && paidText.isBlank()) 0L
    else paidText.toLongOrNull() ?: 0L
  val changeVal = (paidVal - totalAmount).coerceAtLeast(0L)
  val remainingDebt = (totalAmount - minOf(paidVal, totalAmount)).coerceAtLeast(0L)
  val marginPct = if (totalAmount > 0) (totalProfit.toDouble() / totalAmount.toDouble()) * 100.0 else 0.0
  val needsCustomerName = remainingDebt > 0L && customerName.trim().isEmpty()

  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text("Ringkasan Penjualan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
      shape = MaterialTheme.shapes.medium,
    ) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        cartLines.forEach { (p, qty) ->
          MetricRowLine(
            label = "${p.name} ($qty ${p.unit} × ${formatRupiah(p.sellPrice)})",
            valueText = formatRupiah(p.sellPrice * qty),
          )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
        MetricRowLine("Total Belanja", formatRupiah(totalAmount), bold = true)
        MetricRowLine("Total Modal", formatRupiah(totalCost))
        MetricRowLine(
          label = "Keuntungan (${formatPercent(marginPct)})",
          valueText = formatRupiah(totalProfit),
          valueColor = SemanticSuccess,
          bold = true,
        )
      }
    }

    Text("Metode Pembayaran", style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      PaymentMethod.entries.forEach { method ->
        FilterChip(
          selected = selectedMethod == method,
          onClick = {
            selectedMethod = method
            paidText = if (method == PaymentMethod.UTANG) "0" else totalAmount.toString()
          },
          label = { Text(method.label) },
        )
      }
    }

    OutlinedTextField(
      value = customerName,
      onValueChange = { customerName = it.take(100) },
      label = { Text(if (remainingDebt > 0L) "Nama Pelanggan (Wajib untuk Utang)" else "Nama Pelanggan (Opsional)") },
      singleLine = true,
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.fillMaxWidth(),
    )

    RupiahNumberField(
      value = paidText,
      onValueChange = { paidText = it },
      label = "Uang yang Dibayarkan",
      supportingText = "Bisa isi sebagian untuk mencatat pembayaran bertahap / DP",
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      listOf(totalAmount, 50_000L, 100_000L).distinct().forEach { preset ->
        OutlinedButton(onClick = { paidText = preset.toString() }) {
          Text(formatRupiah(preset))
        }
      }
    }

    Surface(
      color =
        if (remainingDebt > 0L) Color(0xFFFEF3C7)
        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
      shape = MaterialTheme.shapes.medium,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        if (remainingDebt > 0L) {
          MetricRowLine(
            label = "Sisa Masuk Utang Pelanggan",
            valueText = formatRupiah(remainingDebt),
            valueColor = SemanticWarning,
            bold = true,
          )
        } else {
          MetricRowLine(
            label = "Kembalian Pelanggan",
            valueText = formatRupiah(changeVal),
            valueColor = MaterialTheme.colorScheme.primary,
            bold = true,
          )
        }
      }
    }

    if (needsCustomerName) {
      Text(
        text = "Isi nama pelanggan karena transaksi ini memiliki sisa utang.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      OutlinedButton(
        onClick = onCancel,
        modifier = Modifier.weight(1f).height(52.dp),
        shape = MaterialTheme.shapes.medium,
      ) {
        Text("Batal")
      }
      Button(
        onClick = { onConfirm(customerName, selectedMethod, paidVal) },
        enabled = !needsCustomerName,
        modifier = Modifier.weight(1.4f).height(52.dp).testTag("confirm_sale_button"),
        shape = MaterialTheme.shapes.medium,
      ) {
        Text("Simpan Transaksi")
      }
    }
  }
}

@Composable
private fun SaleHistoryCard(
  sale: Sale,
  onClick: () -> Unit,
  onDeleteClick: () -> Unit,
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = MaterialTheme.shapes.medium,
    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = sale.receiptNumber + if (sale.customerName.isNotBlank()) " • ${sale.customerName}" else "",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${sale.items.size} jenis • ${PaymentMethod.fromCode(sale.paymentMethod).label} • ${formatDateTime(sale.occurredAtMillis)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = "Laba: ${formatRupiah(sale.totalProfit)} (${formatPercent(sale.profitMarginPercent)})",
          style = MaterialTheme.typography.labelSmall,
          color = SemanticSuccess,
        )
      }
      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = formatRupiah(sale.totalAmount),
          style = NumericTextStyle.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize),
        )
        Spacer(modifier = Modifier.height(4.dp))
        StatusBadge(
          text = when (sale.paymentStatus) {
            "LUNAS" -> "Lunas"
            "SEBAGIAN" -> "Sebagian"
            else -> "Utang"
          },
          containerColor = if (sale.paymentStatus == "LUNAS") MaterialTheme.colorScheme.primaryContainer else Color(0xFFFEF3C7),
          contentColor = if (sale.paymentStatus == "LUNAS") MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF92400E),
        )
      }
      IconButton(onClick = onDeleteClick, modifier = Modifier.size(48.dp)) {
        Icon(
          imageVector = Icons.Outlined.DeleteOutline,
          contentDescription = "Hapus penjualan ${sale.receiptNumber}",
          tint = SemanticError,
          modifier = Modifier.size(20.dp),
        )
      }
    }
  }
}

@Composable
fun ReceiptDialog(sale: Sale, onDismiss: () -> Unit) {
  val context = LocalContext.current
  val receiptText =
    buildString {
      appendLine("STRUK TOKO SEMBAKO")
      appendLine("No: ${sale.receiptNumber}")
      appendLine("Tanggal: ${formatDateTime(sale.occurredAtMillis)}")
      if (sale.customerName.isNotBlank()) appendLine("Pelanggan: ${sale.customerName}")
      appendLine("--------------------------------")
      sale.items.forEach { item ->
        appendLine("${item.productName}")
        appendLine("  ${item.quantity} ${item.unit} × ${formatRupiah(item.sellPrice)} = ${formatRupiah(item.subtotal)}")
      }
      appendLine("--------------------------------")
      appendLine("Total      : ${formatRupiah(sale.totalAmount)}")
      appendLine("Dibayar    : ${formatRupiah(sale.paidAmount)} (${PaymentMethod.fromCode(sale.paymentMethod).label})")
      if (sale.changeAmount > 0) appendLine("Kembalian  : ${formatRupiah(sale.changeAmount)}")
      if (sale.remainingDebt > 0) appendLine("Sisa Utang : ${formatRupiah(sale.remainingDebt)}")
      appendLine("Terima kasih telah berbelanja!")
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Rounded.CheckCircle,
        contentDescription = null,
        tint = SemanticSuccess,
        modifier = Modifier.size(36.dp),
      )
    },
    title = { Text("Struk ${sale.receiptNumber}", fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = formatDateTime(sale.occurredAtMillis) + if (sale.customerName.isNotBlank()) " • ${sale.customerName}" else "",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider()
        sale.items.forEach { item ->
          MetricRowLine(
            label = "${item.productName}\n${item.quantity} ${item.unit} × ${formatRupiah(item.sellPrice)}",
            valueText = formatRupiah(item.subtotal),
          )
        }
        HorizontalDivider()
        MetricRowLine("Total", formatRupiah(sale.totalAmount), bold = true)
        MetricRowLine("Bayar (${PaymentMethod.fromCode(sale.paymentMethod).label})", formatRupiah(sale.paidAmount))
        if (sale.changeAmount > 0) {
          MetricRowLine("Kembalian", formatRupiah(sale.changeAmount), valueColor = MaterialTheme.colorScheme.primary, bold = true)
        }
        if (sale.remainingDebt > 0) {
          MetricRowLine("Sisa Utang", formatRupiah(sale.remainingDebt), valueColor = SemanticWarning, bold = true)
        }
        MetricRowLine(
          "Keuntungan Transaksi",
          "${formatRupiah(sale.totalProfit)} (${formatPercent(sale.profitMarginPercent)})",
          valueColor = SemanticSuccess,
        )
      }
    },
    confirmButton = {
      Button(onClick = { sharePlainText(context, "Struk ${sale.receiptNumber}", receiptText) }) {
        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Bagikan Struk")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Tutup") }
    },
  )
}

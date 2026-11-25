package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

object StoreConstants {
  val PRODUCT_CATEGORIES =
    listOf(
      "Beras",
      "Minyak",
      "Gula",
      "Telur",
      "Mi Instan",
      "Kopi",
      "Minuman",
      "Makanan Ringan",
      "Kebutuhan Rumah Tangga",
      "Lainnya",
    )

  val PRODUCT_UNITS = listOf("pcs", "kg", "liter", "dus", "renceng", "sak", "bungkus", "botol")

  val INCOME_CATEGORIES =
    listOf("Penjualan", "Pembayaran Utang Pelanggan", "Tambahan Modal", "Pendapatan Lainnya")

  val EXPENSE_CATEGORIES =
    listOf(
      "Belanja Stok",
      "Biaya Operasional",
      "Transportasi",
      "Listrik",
      "Keperluan Toko",
      "Pengambilan Uang Pribadi",
      "Pengeluaran Lainnya",
    )
}

enum class PaymentMethod(val code: String, val label: String) {
  TUNAI("TUNAI", "Tunai"),
  TRANSFER("TRANSFER", "Transfer"),
  QRIS("QRIS", "QRIS"),
  UTANG("UTANG", "Utang");

  companion object {
    fun fromCode(raw: String?): PaymentMethod =
      entries.firstOrNull { it.code == raw } ?: TUNAI
  }
}

enum class StockChangeType(val code: String, val label: String) {
  MASUK("MASUK", "Barang Masuk"),
  KELUAR_PENJUALAN("KELUAR_PENJUALAN", "Terjual"),
  TAMBAH_MANUAL("TAMBAH_MANUAL", "Tambah Stok"),
  KURANG_MANUAL("KURANG_MANUAL", "Kurangi Stok"),
  BARANG_RUSAK("BARANG_RUSAK", "Barang Rusak"),
  PERUBAHAN_HARGA("PERUBAHAN_HARGA", "Harga Modal Berubah");

  companion object {
    fun fromCode(raw: String?): StockChangeType =
      entries.firstOrNull { it.code == raw } ?: MASUK
  }
}

enum class ActivityType(val code: String, val label: String) {
  PENJUALAN("PENJUALAN", "Penjualan"),
  PEMBELIAN_STOK("PEMBELIAN_STOK", "Pembelian Stok"),
  BARANG_MASUK("BARANG_MASUK", "Barang Masuk"),
  BARANG_KELUAR("BARANG_KELUAR", "Barang Keluar"),
  PENGELUARAN("PENGELUARAN", "Pengeluaran"),
  PEMASUKAN("PEMASUKAN", "Pemasukan"),
  PEMBAYARAN_UTANG("PEMBAYARAN_UTANG", "Pembayaran Utang"),
  TAMBAHAN_MODAL("TAMBAHAN_MODAL", "Tambahan Modal"),
  PENGURANGAN_STOK("PENGURANGAN_STOK", "Pengurangan Stok"),
  BARANG_RUSAK("BARANG_RUSAK", "Barang Rusak");

  companion object {
    fun fromCode(raw: String?): ActivityType =
      entries.firstOrNull { it.code == raw } ?: PENJUALAN
  }
}

@Keep
@IgnoreExtraProperties
data class Product(
  @DocumentId val id: String = "",
  val name: String = "",
  val category: String = "Beras",
  val costPrice: Long = 0L,
  val sellPrice: Long = 0L,
  val stock: Long = 0L,
  val unit: String = "pcs",
  val minStock: Long = 5L,
  val photoPath: String? = null,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
) {
  val profitPerUnit: Long
    get() = sellPrice - costPrice

  val marginPercent: Double
    get() = if (sellPrice > 0) (profitPerUnit.toDouble() / sellPrice.toDouble()) * 100.0 else 0.0

  val markupPercent: Double
    get() = if (costPrice > 0) (profitPerUnit.toDouble() / costPrice.toDouble()) * 100.0 else 0.0

  val lowStockWarning: Boolean
    get() = stock <= minStock

  fun toCreateMap(): Map<String, Any> =
    mapOf(
      "name" to name.trim().take(100),
      "category" to category.trim().take(50),
      "costPrice" to costPrice.coerceAtLeast(0L),
      "sellPrice" to sellPrice.coerceAtLeast(0L),
      "stock" to stock.coerceAtLeast(0L),
      "unit" to unit.trim().take(30),
      "minStock" to minStock.coerceAtLeast(0L),
      "photoPath" to photoPath?.take(500),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class SaleLineItem(
  val productId: String = "",
  val productName: String = "",
  val unit: String = "pcs",
  val quantity: Long = 0L,
  val costPrice: Long = 0L,
  val sellPrice: Long = 0L,
  val subtotal: Long = 0L,
  val profit: Long = 0L,
) {
  fun toMap(): Map<String, Any> =
    mapOf(
      "productId" to productId,
      "productName" to productName,
      "unit" to unit,
      "quantity" to quantity,
      "costPrice" to costPrice,
      "sellPrice" to sellPrice,
      "subtotal" to subtotal,
      "profit" to profit,
    )
}

@Keep
@IgnoreExtraProperties
data class Sale(
  @DocumentId val id: String = "",
  val receiptNumber: String = "",
  val customerName: String = "",
  val paymentMethod: String = "TUNAI",
  val paymentStatus: String = "LUNAS",
  val items: List<SaleLineItem> = emptyList(),
  val totalAmount: Long = 0L,
  val totalCost: Long = 0L,
  val totalProfit: Long = 0L,
  val paidAmount: Long = 0L,
  val changeAmount: Long = 0L,
  val remainingDebt: Long = 0L,
  val occurredAtMillis: Long = 0L,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
) {
  val profitMarginPercent: Double
    get() = if (totalAmount > 0) (totalProfit.toDouble() / totalAmount.toDouble()) * 100.0 else 0.0

  fun toCreateMap(): Map<String, Any> =
    mapOf(
      "receiptNumber" to receiptNumber.take(40),
      "customerName" to customerName.trim().take(100),
      "paymentMethod" to paymentMethod,
      "paymentStatus" to paymentStatus,
      "items" to items.take(50).map { it.toMap() },
      "totalAmount" to totalAmount.coerceAtLeast(0L),
      "totalCost" to totalCost.coerceAtLeast(0L),
      "totalProfit" to totalProfit,
      "paidAmount" to paidAmount.coerceAtLeast(0L),
      "changeAmount" to changeAmount.coerceAtLeast(0L),
      "remainingDebt" to remainingDebt.coerceAtLeast(0L),
      "occurredAtMillis" to occurredAtMillis,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class StockPurchase(
  @DocumentId val id: String = "",
  val productId: String = "",
  val productName: String = "",
  val unit: String = "pcs",
  val quantity: Long = 0L,
  val buyPrice: Long = 0L,
  val totalCost: Long = 0L,
  val paidAmount: Long = 0L,
  val supplierName: String = "",
  val paymentMethod: String = "TUNAI",
  val paymentStatus: String = "LUNAS",
  val notePhotoPath: String? = null,
  val occurredAtMillis: Long = 0L,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> =
    mapOf(
      "productId" to productId.take(128),
      "productName" to productName.trim().take(100),
      "unit" to unit.trim().take(30),
      "quantity" to quantity.coerceAtLeast(1L),
      "buyPrice" to buyPrice.coerceAtLeast(0L),
      "totalCost" to totalCost.coerceAtLeast(0L),
      "paidAmount" to paidAmount.coerceAtLeast(0L),
      "supplierName" to supplierName.trim().take(100),
      "paymentMethod" to paymentMethod,
      "paymentStatus" to paymentStatus,
      "notePhotoPath" to notePhotoPath?.take(500),
      "occurredAtMillis" to occurredAtMillis,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class StockLog(
  @DocumentId val id: String = "",
  val productId: String = "",
  val productName: String = "",
  val changeType: String = "MASUK",
  val quantityDelta: Long = 0L,
  val stockBefore: Long = 0L,
  val stockAfter: Long = 0L,
  val oldCostPrice: Long? = null,
  val newCostPrice: Long? = null,
  val note: String = "",
  val occurredAtMillis: Long = 0L,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> =
    mapOf(
      "productId" to productId.take(128),
      "productName" to productName.trim().take(100),
      "changeType" to changeType,
      "quantityDelta" to quantityDelta,
      "stockBefore" to stockBefore.coerceAtLeast(0L),
      "stockAfter" to stockAfter.coerceAtLeast(0L),
      "oldCostPrice" to oldCostPrice?.coerceAtLeast(0L),
      "newCostPrice" to newCostPrice?.coerceAtLeast(0L),
      "note" to note.trim().take(300),
      "occurredAtMillis" to occurredAtMillis,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class CashTransaction(
  @DocumentId val id: String = "",
  val direction: String = "MASUK",
  val category: String = "",
  val amount: Long = 0L,
  val paymentMethod: String = "TUNAI",
  val description: String = "",
  val occurredAtMillis: Long = 0L,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> =
    mapOf(
      "direction" to direction,
      "category" to category.trim().take(60),
      "amount" to amount.coerceAtLeast(1L),
      "paymentMethod" to paymentMethod,
      "description" to description.trim().take(300),
      "occurredAtMillis" to occurredAtMillis,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class DebtPayment(
  val amount: Long = 0L,
  val paidAtMillis: Long = 0L,
  val paymentMethod: String = "TUNAI",
  val note: String = "",
) {
  fun toMap(): Map<String, Any> =
    mapOf(
      "amount" to amount,
      "paidAtMillis" to paidAtMillis,
      "paymentMethod" to paymentMethod,
      "note" to note.take(150),
    )
}

@Keep
@IgnoreExtraProperties
data class CustomerDebt(
  @DocumentId val id: String = "",
  val customerName: String = "",
  val phone: String = "",
  val totalDebt: Long = 0L,
  val paidAmount: Long = 0L,
  val remainingAmount: Long = 0L,
  val status: String = "BELUM_LUNAS",
  val note: String = "",
  val payments: List<DebtPayment> = emptyList(),
  val occurredAtMillis: Long = 0L,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> =
    mapOf(
      "customerName" to customerName.trim().take(100),
      "phone" to phone.trim().take(30),
      "totalDebt" to totalDebt.coerceAtLeast(1L),
      "paidAmount" to paidAmount.coerceAtLeast(0L),
      "remainingAmount" to remainingAmount.coerceAtLeast(0L),
      "status" to status,
      "note" to note.trim().take(300),
      "payments" to payments.take(100).map { it.toMap() },
      "occurredAtMillis" to occurredAtMillis,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class SupplierDebt(
  @DocumentId val id: String = "",
  val supplierName: String = "",
  val itemDescription: String = "",
  val totalDebt: Long = 0L,
  val paidAmount: Long = 0L,
  val remainingAmount: Long = 0L,
  val status: String = "BELUM_LUNAS",
  val note: String = "",
  val payments: List<DebtPayment> = emptyList(),
  val occurredAtMillis: Long = 0L,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> =
    mapOf(
      "supplierName" to supplierName.trim().take(100),
      "itemDescription" to itemDescription.trim().take(200),
      "totalDebt" to totalDebt.coerceAtLeast(1L),
      "paidAmount" to paidAmount.coerceAtLeast(0L),
      "remainingAmount" to remainingAmount.coerceAtLeast(0L),
      "status" to status,
      "note" to note.trim().take(300),
      "payments" to payments.take(100).map { it.toMap() },
      "occurredAtMillis" to occurredAtMillis,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class ActivityEntry(
  @DocumentId val id: String = "",
  val type: String = "PENJUALAN",
  val title: String = "",
  val subtitle: String = "",
  val amount: Long = 0L,
  val occurredAtMillis: Long = 0L,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> =
    mapOf(
      "type" to type,
      "title" to title.trim().take(140),
      "subtitle" to subtitle.trim().take(300),
      "amount" to amount,
      "occurredAtMillis" to occurredAtMillis,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

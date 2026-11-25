package com.example.data

import android.content.Context
import com.example.R
import com.example.model.ActivityEntry
import com.example.model.ActivityType
import com.example.model.CashTransaction
import com.example.model.CustomerDebt
import com.example.model.DebtPayment
import com.example.model.PaymentMethod
import com.example.model.Product
import com.example.model.Sale
import com.example.model.SaleLineItem
import com.example.model.StockChangeType
import com.example.model.StockLog
import com.example.model.StockPurchase
import com.example.model.SupplierDebt
import com.example.util.formatRupiah
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StoreRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("Pengguna harus masuk dengan Google sebelum mengakses data toko.")

  private fun userDoc(uid: String) = db.collection("users").document(uid)

  private inline fun <reified T> observeUserCollection(
    userId: String,
    collectionName: String,
    orderByField: String = "occurredAtMillis",
    direction: Query.Direction = Query.Direction.DESCENDING,
  ): Flow<List<T>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Sesi akun berubah; akses dibatalkan." }
    val path = "users/$uid/$collectionName"
    emitAll(
      userDoc(uid)
        .collection(collectionName)
        .orderBy(orderByField, direction)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(T::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  fun observeProducts(userId: String): Flow<List<Product>> =
    observeUserCollection(userId, "products", orderByField = "name", direction = Query.Direction.ASCENDING)

  fun observeSales(userId: String): Flow<List<Sale>> =
    observeUserCollection(userId, "sales")

  fun observePurchases(userId: String): Flow<List<StockPurchase>> =
    observeUserCollection(userId, "purchases")

  fun observeStockLogs(userId: String): Flow<List<StockLog>> =
    observeUserCollection(userId, "stock_logs")

  fun observeCashTransactions(userId: String): Flow<List<CashTransaction>> =
    observeUserCollection(userId, "cash_transactions")

  fun observeCustomerDebts(userId: String): Flow<List<CustomerDebt>> =
    observeUserCollection(userId, "customer_debts")

  fun observeSupplierDebts(userId: String): Flow<List<SupplierDebt>> =
    observeUserCollection(userId, "supplier_debts")

  fun observeActivities(userId: String): Flow<List<ActivityEntry>> =
    observeUserCollection(userId, "activities")

  suspend fun addProduct(product: Product): Result<String> {
    var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/products"
    return try {
      val uid = requireUserId()
      val trimmedName = product.name.trim()
      require(trimmedName.isNotEmpty() && trimmedName.length <= 100) { "Nama barang wajib 1-100 karakter." }
      require(product.costPrice >= 0 && product.sellPrice >= 0 && product.stock >= 0 && product.minStock >= 0) {
        "Harga dan stok tidak boleh bernilai negatif."
      }
      val docRef = userDoc(uid).collection("products").document()
      docPath = docRef.path
      val now = System.currentTimeMillis()
      val batch = db.batch()
      batch.set(docRef, product.copy(name = trimmedName).toCreateMap())
      if (product.stock > 0) {
        val logRef = userDoc(uid).collection("stock_logs").document()
        batch.set(
          logRef,
          StockLog(
            productId = docRef.id,
            productName = trimmedName,
            changeType = StockChangeType.MASUK.code,
            quantityDelta = product.stock,
            stockBefore = 0L,
            stockAfter = product.stock,
            newCostPrice = product.costPrice,
            note = "Stok awal barang baru",
            occurredAtMillis = now,
          ).toCreateMap(),
        )
        val actRef = userDoc(uid).collection("activities").document()
        batch.set(
          actRef,
          ActivityEntry(
            type = ActivityType.BARANG_MASUK.code,
            title = "Barang Baru: $trimmedName",
            subtitle = "Stok awal ${product.stock} ${product.unit}",
            amount = product.stock * product.costPrice,
            occurredAtMillis = now,
          ).toCreateMap(),
        )
      }
      batch.commit().await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun updateProduct(existing: Product, updated: Product): Result<Unit> {
    var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/products/${existing.id}"
    return try {
      val uid = requireUserId()
      require(existing.id.isNotBlank()) { "ID barang tidak valid." }
      val trimmedName = updated.name.trim()
      require(trimmedName.isNotEmpty() && trimmedName.length <= 100) { "Nama barang wajib 1-100 karakter." }
      require(updated.costPrice >= 0 && updated.sellPrice >= 0 && updated.stock >= 0 && updated.minStock >= 0) {
        "Harga dan stok tidak boleh bernilai negatif."
      }
      val docRef = userDoc(uid).collection("products").document(existing.id)
      docPath = docRef.path
      val now = System.currentTimeMillis()
      val batch = db.batch()

      val updatePayload =
        mutableMapOf<String, Any>(
          "name" to trimmedName,
          "category" to updated.category.trim().take(50),
          "costPrice" to updated.costPrice,
          "sellPrice" to updated.sellPrice,
          "stock" to updated.stock,
          "unit" to updated.unit.trim().take(30),
          "minStock" to updated.minStock,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (!updated.photoPath.isNullOrBlank()) {
        updatePayload["photoPath"] = updated.photoPath.take(500)
      }
      batch.update(docRef, updatePayload)

      if (updated.costPrice != existing.costPrice) {
        val priceLogRef = userDoc(uid).collection("stock_logs").document()
        batch.set(
          priceLogRef,
          StockLog(
            productId = existing.id,
            productName = trimmedName,
            changeType = StockChangeType.PERUBAHAN_HARGA.code,
            quantityDelta = 0L,
            stockBefore = updated.stock,
            stockAfter = updated.stock,
            oldCostPrice = existing.costPrice,
            newCostPrice = updated.costPrice,
            note = "Harga modal diubah dari ${formatRupiah(existing.costPrice)} ke ${formatRupiah(updated.costPrice)}",
            occurredAtMillis = now,
          ).toCreateMap(),
        )
      }

      if (updated.stock != existing.stock) {
        val delta = updated.stock - existing.stock
        val changeType = if (delta > 0) StockChangeType.TAMBAH_MANUAL else StockChangeType.KURANG_MANUAL
        val stockLogRef = userDoc(uid).collection("stock_logs").document()
        batch.set(
          stockLogRef,
          StockLog(
            productId = existing.id,
            productName = trimmedName,
            changeType = changeType.code,
            quantityDelta = delta,
            stockBefore = existing.stock,
            stockAfter = updated.stock,
            note = "Penyesuaian stok saat edit barang",
            occurredAtMillis = now,
          ).toCreateMap(),
        )
      }

      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun deleteProduct(productId: String): Result<Unit> {
    var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/products/$productId"
    return try {
      val uid = requireUserId()
      require(productId.isNotBlank()) { "ID barang kosong." }
      val docRef = userDoc(uid).collection("products").document(productId)
      docPath = docRef.path
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }

  suspend fun adjustStock(
    product: Product,
    delta: Long,
    changeType: StockChangeType,
    note: String,
  ): Result<Unit> {
    var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/products/${product.id}"
    return try {
      val uid = requireUserId()
      require(product.id.isNotBlank()) { "ID barang tidak valid." }
      require(delta != 0L) { "Jumlah perubahan stok harus lebih dari 0." }
      val newStock = (product.stock + delta).coerceAtLeast(0L)
      val actualDelta = newStock - product.stock
      val now = System.currentTimeMillis()
      val prodRef = userDoc(uid).collection("products").document(product.id)
      docPath = prodRef.path
      val batch = db.batch()
      batch.update(
        prodRef,
        mapOf("stock" to newStock, "updatedAt" to FieldValue.serverTimestamp()),
      )

      val logRef = userDoc(uid).collection("stock_logs").document()
      batch.set(
        logRef,
        StockLog(
          productId = product.id,
          productName = product.name,
          changeType = changeType.code,
          quantityDelta = actualDelta,
          stockBefore = product.stock,
          stockAfter = newStock,
          note = note.ifBlank { changeType.label },
          occurredAtMillis = now,
        ).toCreateMap(),
      )

      val actType =
        when (changeType) {
          StockChangeType.BARANG_RUSAK -> ActivityType.BARANG_RUSAK
          StockChangeType.TAMBAH_MANUAL, StockChangeType.MASUK -> ActivityType.BARANG_MASUK
          else -> ActivityType.PENGURANGAN_STOK
        }
      val lossAmount =
        if (changeType == StockChangeType.BARANG_RUSAK) {
          kotlin.math.abs(actualDelta) * product.costPrice
        } else {
          0L
        }
      val actRef = userDoc(uid).collection("activities").document()
      batch.set(
        actRef,
        ActivityEntry(
          type = actType.code,
          title = "${changeType.label}: ${product.name}",
          subtitle = "${if (actualDelta >= 0) "+" else ""}$actualDelta ${product.unit} • Stok akhir $newStock ${product.unit}" +
            if (note.isNotBlank()) " ($note)" else "",
          amount = lossAmount,
          occurredAtMillis = now,
        ).toCreateMap(),
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun recordSale(
    cartLines: List<Pair<Product, Long>>,
    customerName: String,
    paymentMethod: PaymentMethod,
    paidAmountInput: Long,
  ): Result<Sale> {
    var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/sales"
    return try {
      val uid = requireUserId()
      require(cartLines.isNotEmpty()) { "Pilih minimal 1 barang untuk dijual." }
      require(cartLines.size <= 50) { "Maksimal 50 jenis barang per struk." }

      val lineItems =
        cartLines.map { (product, qty) ->
          require(qty > 0) { "Jumlah barang harus lebih dari 0." }
          require(qty <= product.stock) { "Stok ${product.name} tidak mencukupi (tersisa ${product.stock})." }
          val subtotal = product.sellPrice * qty
          val profit = (product.sellPrice - product.costPrice) * qty
          SaleLineItem(
            productId = product.id,
            productName = product.name,
            unit = product.unit,
            quantity = qty,
            costPrice = product.costPrice,
            sellPrice = product.sellPrice,
            subtotal = subtotal,
            profit = profit,
          )
        }

      val totalAmount = lineItems.sumOf { it.subtotal }
      val totalCost = lineItems.sumOf { it.costPrice * it.quantity }
      val totalProfit = lineItems.sumOf { it.profit }

      val actualPaid =
        if (paymentMethod == PaymentMethod.UTANG && paidAmountInput <= 0L) 0L else paidAmountInput.coerceAtLeast(0L)
      val changeAmount = if (actualPaid > totalAmount) actualPaid - totalAmount else 0L
      val settledCash = minOf(actualPaid, totalAmount)
      val remainingDebt = (totalAmount - settledCash).coerceAtLeast(0L)

      if (remainingDebt > 0L) {
        require(customerName.trim().isNotEmpty()) {
          "Nama pelanggan wajib diisi jika pembayaran kurang / utang."
        }
      }

      val paymentStatus =
        when {
          remainingDebt == 0L -> "LUNAS"
          settledCash > 0L -> "SEBAGIAN"
          else -> "UTANG"
        }

      val now = System.currentTimeMillis()
      val receiptNo =
        "TRX-" + SimpleDateFormat("yyMMdd-HHmmss", Locale.US).format(Date(now))
      val saleRef = userDoc(uid).collection("sales").document()
      docPath = saleRef.path

      val sale =
        Sale(
          id = saleRef.id,
          receiptNumber = receiptNo,
          customerName = customerName.trim(),
          paymentMethod = paymentMethod.code,
          paymentStatus = paymentStatus,
          items = lineItems,
          totalAmount = totalAmount,
          totalCost = totalCost,
          totalProfit = totalProfit,
          paidAmount = actualPaid,
          changeAmount = changeAmount,
          remainingDebt = remainingDebt,
          occurredAtMillis = now,
        )

      val batch = db.batch()
      batch.set(saleRef, sale.toCreateMap())

      cartLines.forEach { (product, qty) ->
        val newStock = (product.stock - qty).coerceAtLeast(0L)
        val prodRef = userDoc(uid).collection("products").document(product.id)
        batch.update(
          prodRef,
          mapOf("stock" to newStock, "updatedAt" to FieldValue.serverTimestamp()),
        )
        val stockLogRef = userDoc(uid).collection("stock_logs").document()
        batch.set(
          stockLogRef,
          StockLog(
            productId = product.id,
            productName = product.name,
            changeType = StockChangeType.KELUAR_PENJUALAN.code,
            quantityDelta = -qty,
            stockBefore = product.stock,
            stockAfter = newStock,
            note = "Terjual pada struk $receiptNo",
            occurredAtMillis = now,
          ).toCreateMap(),
        )
      }

      if (settledCash > 0L) {
        val cashMethod =
          if (paymentMethod == PaymentMethod.UTANG) PaymentMethod.TUNAI.code else paymentMethod.code
        val cashRef = userDoc(uid).collection("cash_transactions").document()
        batch.set(
          cashRef,
          CashTransaction(
            direction = "MASUK",
            category = "Penjualan",
            amount = settledCash,
            paymentMethod = cashMethod,
            description =
              "Penjualan $receiptNo" +
                if (customerName.isNotBlank()) " (${customerName.trim()})" else "",
            occurredAtMillis = now,
          ).toCreateMap(),
        )
      }

      if (remainingDebt > 0L) {
        val debtRef = userDoc(uid).collection("customer_debts").document()
        val initialPayments =
          if (settledCash > 0L) {
            listOf(
              DebtPayment(
                amount = settledCash,
                paidAtMillis = now,
                paymentMethod =
                  if (paymentMethod == PaymentMethod.UTANG) PaymentMethod.TUNAI.code else paymentMethod.code,
                note = "DP penjualan $receiptNo",
              )
            )
          } else {
            emptyList()
          }
        batch.set(
          debtRef,
          CustomerDebt(
            customerName = customerName.trim(),
            phone = "",
            totalDebt = totalAmount,
            paidAmount = settledCash,
            remainingAmount = remainingDebt,
            status = "BELUM_LUNAS",
            note = "Belanja $receiptNo (${lineItems.joinToString { "${it.productName} ${it.quantity} ${it.unit}" }})",
            payments = initialPayments,
            occurredAtMillis = now,
          ).toCreateMap(),
        )
      }

      val actRef = userDoc(uid).collection("activities").document()
      batch.set(
        actRef,
        ActivityEntry(
          type = ActivityType.PENJUALAN.code,
          title = "Penjualan $receiptNo (${lineItems.size} item)",
          subtitle =
            buildString {
              if (customerName.isNotBlank()) append("${customerName.trim()} • ")
              append(paymentMethod.label)
              if (remainingDebt > 0L) append(" • Sisa utang ${formatRupiah(remainingDebt)}")
            },
          amount = totalAmount,
          occurredAtMillis = now,
        ).toCreateMap(),
      )

      batch.commit().await()
      Result.success(sale)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun recordStockPurchase(
    product: Product,
    quantity: Long,
    buyPricePerUnit: Long,
    supplierName: String,
    paymentMethod: PaymentMethod,
    isPaidOff: Boolean,
    paidAmountInput: Long,
    notePhotoPath: String?,
    occurredAtMillis: Long = System.currentTimeMillis(),
  ): Result<Unit> {
    var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/purchases"
    return try {
      val uid = requireUserId()
      require(product.id.isNotBlank()) { "Barang wajib dipilih." }
      require(quantity > 0L) { "Jumlah pembelian harus lebih dari 0." }
      require(buyPricePerUnit >= 0L) { "Harga beli tidak valid." }

      val totalCost = quantity * buyPricePerUnit
      val settledAmount =
        if (isPaidOff && paymentMethod != PaymentMethod.UTANG) {
          totalCost
        } else {
          paidAmountInput.coerceIn(0L, totalCost)
        }
      val remainingDebt = (totalCost - settledAmount).coerceAtLeast(0L)
      val status = if (remainingDebt == 0L) "LUNAS" else "UTANG"
      if (remainingDebt > 0L) {
        require(supplierName.trim().isNotEmpty()) {
          "Nama supplier wajib diisi jika status pembayaran utang."
        }
      }

      val purchaseRef = userDoc(uid).collection("purchases").document()
      docPath = purchaseRef.path
      val prodRef = userDoc(uid).collection("products").document(product.id)
      val newStock = product.stock + quantity

      val batch = db.batch()
      batch.set(
        purchaseRef,
        StockPurchase(
          productId = product.id,
          productName = product.name,
          unit = product.unit,
          quantity = quantity,
          buyPrice = buyPricePerUnit,
          totalCost = totalCost,
          paidAmount = settledAmount,
          supplierName = supplierName.trim(),
          paymentMethod = paymentMethod.code,
          paymentStatus = status,
          notePhotoPath = notePhotoPath,
          occurredAtMillis = occurredAtMillis,
        ).toCreateMap(),
      )

      val prodUpdate =
        mutableMapOf<String, Any>(
          "stock" to newStock,
          "costPrice" to buyPricePerUnit,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      batch.update(prodRef, prodUpdate)

      val priceChanged = buyPricePerUnit != product.costPrice
      val stockLogRef = userDoc(uid).collection("stock_logs").document()
      batch.set(
        stockLogRef,
        StockLog(
          productId = product.id,
          productName = product.name,
          changeType = StockChangeType.MASUK.code,
          quantityDelta = quantity,
          stockBefore = product.stock,
          stockAfter = newStock,
          oldCostPrice = if (priceChanged) product.costPrice else null,
          newCostPrice = if (priceChanged) buyPricePerUnit else null,
          note =
            buildString {
              append("Belanja stok +$quantity ${product.unit}")
              if (supplierName.isNotBlank()) append(" dari ${supplierName.trim()}")
              if (priceChanged) {
                append(" • Modal: ${formatRupiah(product.costPrice)} → ${formatRupiah(buyPricePerUnit)}")
              }
            },
          occurredAtMillis = occurredAtMillis,
        ).toCreateMap(),
      )

      if (settledAmount > 0L) {
        val cashMethod =
          if (paymentMethod == PaymentMethod.UTANG) PaymentMethod.TUNAI.code else paymentMethod.code
        val cashRef = userDoc(uid).collection("cash_transactions").document()
        batch.set(
          cashRef,
          CashTransaction(
            direction = "KELUAR",
            category = "Belanja Stok",
            amount = settledAmount,
            paymentMethod = cashMethod,
            description =
              "Belanja ${product.name} ($quantity ${product.unit})" +
                if (supplierName.isNotBlank()) " - ${supplierName.trim()}" else "",
            occurredAtMillis = occurredAtMillis,
          ).toCreateMap(),
        )
      }

      if (remainingDebt > 0L) {
        val supDebtRef = userDoc(uid).collection("supplier_debts").document()
        val initialPayments =
          if (settledAmount > 0L) {
            listOf(
              DebtPayment(
                amount = settledAmount,
                paidAtMillis = occurredAtMillis,
                paymentMethod =
                  if (paymentMethod == PaymentMethod.UTANG) PaymentMethod.TUNAI.code else paymentMethod.code,
                note = "DP pembelian ${product.name}",
              )
            )
          } else {
            emptyList()
          }
        batch.set(
          supDebtRef,
          SupplierDebt(
            supplierName = supplierName.trim(),
            itemDescription = "${product.name} ($quantity ${product.unit})",
            totalDebt = totalCost,
            paidAmount = settledAmount,
            remainingAmount = remainingDebt,
            status = "BELUM_LUNAS",
            note = "Belanja stok modal ${formatRupiah(buyPricePerUnit)}/${product.unit}",
            payments = initialPayments,
            occurredAtMillis = occurredAtMillis,
          ).toCreateMap(),
        )
      }

      val actRef = userDoc(uid).collection("activities").document()
      batch.set(
        actRef,
        ActivityEntry(
          type = ActivityType.PEMBELIAN_STOK.code,
          title = "Belanja Stok: ${product.name} (+$quantity ${product.unit})",
          subtitle =
            buildString {
              if (supplierName.isNotBlank()) append("${supplierName.trim()} • ")
              append(if (remainingDebt == 0L) "Lunas" else "Utang ${formatRupiah(remainingDebt)}")
            },
          amount = totalCost,
          occurredAtMillis = occurredAtMillis,
        ).toCreateMap(),
      )

      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun recordCashTransaction(
    direction: String,
    category: String,
    amount: Long,
    paymentMethod: PaymentMethod,
    description: String,
    occurredAtMillis: Long = System.currentTimeMillis(),
  ): Result<Unit> {
    var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/cash_transactions"
    return try {
      val uid = requireUserId()
      require(amount > 0L) { "Nominal harus lebih dari Rp0." }
      require(category.isNotBlank()) { "Kategori wajib dipilih." }
      val methodCode =
        if (paymentMethod == PaymentMethod.UTANG) PaymentMethod.TUNAI.code else paymentMethod.code

      val txRef = userDoc(uid).collection("cash_transactions").document()
      docPath = txRef.path
      val batch = db.batch()
      batch.set(
        txRef,
        CashTransaction(
          direction = direction,
          category = category,
          amount = amount,
          paymentMethod = methodCode,
          description = description.trim(),
          occurredAtMillis = occurredAtMillis,
        ).toCreateMap(),
      )

      val actType =
        when {
          direction == "MASUK" && category == "Tambahan Modal" -> ActivityType.TAMBAHAN_MODAL
          direction == "MASUK" -> ActivityType.PEMASUKAN
          else -> ActivityType.PENGELUARAN
        }
      val actRef = userDoc(uid).collection("activities").document()
      batch.set(
        actRef,
        ActivityEntry(
          type = actType.code,
          title = "${if (direction == "MASUK") "Uang Masuk" else "Uang Keluar"}: $category",
          subtitle = description.trim().ifBlank { PaymentMethod.fromCode(methodCode).label },
          amount = amount,
          occurredAtMillis = occurredAtMillis,
        ).toCreateMap(),
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun addCustomerDebt(
    customerName: String,
    phone: String,
    totalDebt: Long,
    note: String,
    occurredAtMillis: Long = System.currentTimeMillis(),
  ): Result<Unit> {
    var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/customer_debts"
    return try {
      val uid = requireUserId()
      require(customerName.trim().isNotEmpty()) { "Nama pelanggan wajib diisi." }
      require(totalDebt > 0L) { "Nominal utang harus lebih dari Rp0." }

      val debtRef = userDoc(uid).collection("customer_debts").document()
      docPath = debtRef.path
      val batch = db.batch()
      batch.set(
        debtRef,
        CustomerDebt(
          customerName = customerName.trim(),
          phone = phone.trim(),
          totalDebt = totalDebt,
          paidAmount = 0L,
          remainingAmount = totalDebt,
          status = "BELUM_LUNAS",
          note = note.trim(),
          payments = emptyList(),
          occurredAtMillis = occurredAtMillis,
        ).toCreateMap(),
      )
      val actRef = userDoc(uid).collection("activities").document()
      batch.set(
        actRef,
        ActivityEntry(
          type = ActivityType.PEMBAYARAN_UTANG.code,
          title = "Utang Baru Pelanggan: ${customerName.trim()}",
          subtitle = note.trim().ifBlank { "Catatan utang pelanggan" },
          amount = totalDebt,
          occurredAtMillis = occurredAtMillis,
        ).toCreateMap(),
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun payCustomerDebt(
    debt: CustomerDebt,
    paymentAmount: Long,
    paymentMethod: PaymentMethod,
    note: String,
  ): Result<Unit> {
    var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/customer_debts/${debt.id}"
    return try {
      val uid = requireUserId()
      require(debt.id.isNotBlank()) { "ID utang tidak valid." }
      require(debt.status == "BELUM_LUNAS") { "Utang ini sudah lunas." }
      require(paymentAmount > 0L && paymentAmount <= debt.remainingAmount) {
        "Nominal bayar harus antara Rp1 sampai ${formatRupiah(debt.remainingAmount)}."
      }

      val now = System.currentTimeMillis()
      val methodCode =
        if (paymentMethod == PaymentMethod.UTANG) PaymentMethod.TUNAI.code else paymentMethod.code
      val newPaid = debt.paidAmount + paymentAmount
      val newRemaining = debt.totalDebt - newPaid
      val newStatus = if (newRemaining == 0L) "LUNAS" else "BELUM_LUNAS"
      val newPayments =
        debt.payments +
          DebtPayment(
            amount = paymentAmount,
            paidAtMillis = now,
            paymentMethod = methodCode,
            note = note.trim().ifBlank { if (newRemaining == 0L) "Pelunasan" else "Cicilan" },
          )

      val debtRef = userDoc(uid).collection("customer_debts").document(debt.id)
      docPath = debtRef.path
      val batch = db.batch()
      batch.update(
        debtRef,
        mapOf(
          "paidAmount" to newPaid,
          "remainingAmount" to newRemaining,
          "status" to newStatus,
          "payments" to newPayments.take(100).map { it.toMap() },
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )

      val cashRef = userDoc(uid).collection("cash_transactions").document()
      batch.set(
        cashRef,
        CashTransaction(
          direction = "MASUK",
          category = "Pembayaran Utang Pelanggan",
          amount = paymentAmount,
          paymentMethod = methodCode,
          description = "Bayar utang ${debt.customerName}" + if (newRemaining == 0L) " (Lunas)" else "",
          occurredAtMillis = now,
        ).toCreateMap(),
      )

      val actRef = userDoc(uid).collection("activities").document()
      batch.set(
        actRef,
        ActivityEntry(
          type = ActivityType.PEMBAYARAN_UTANG.code,
          title = "Pelanggan Bayar Utang: ${debt.customerName}",
          subtitle = "Dibayar ${formatRupiah(paymentAmount)} • Sisa ${formatRupiah(newRemaining)}",
          amount = paymentAmount,
          occurredAtMillis = now,
        ).toCreateMap(),
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun addSupplierDebt(
    supplierName: String,
    itemDescription: String,
    totalDebt: Long,
    note: String,
    occurredAtMillis: Long = System.currentTimeMillis(),
  ): Result<Unit> {
    var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/supplier_debts"
    return try {
      val uid = requireUserId()
      require(supplierName.trim().isNotEmpty()) { "Nama supplier wajib diisi." }
      require(itemDescription.trim().isNotEmpty()) { "Barang yang dibeli wajib diisi." }
      require(totalDebt > 0L) { "Nominal utang harus lebih dari Rp0." }

      val debtRef = userDoc(uid).collection("supplier_debts").document()
      docPath = debtRef.path
      val batch = db.batch()
      batch.set(
        debtRef,
        SupplierDebt(
          supplierName = supplierName.trim(),
          itemDescription = itemDescription.trim(),
          totalDebt = totalDebt,
          paidAmount = 0L,
          remainingAmount = totalDebt,
          status = "BELUM_LUNAS",
          note = note.trim(),
          payments = emptyList(),
          occurredAtMillis = occurredAtMillis,
        ).toCreateMap(),
      )
      val actRef = userDoc(uid).collection("activities").document()
      batch.set(
        actRef,
        ActivityEntry(
          type = ActivityType.PEMBAYARAN_UTANG.code,
          title = "Utang Supplier Baru: ${supplierName.trim()}",
          subtitle = itemDescription.trim(),
          amount = totalDebt,
          occurredAtMillis = occurredAtMillis,
        ).toCreateMap(),
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun paySupplierDebt(
    debt: SupplierDebt,
    paymentAmount: Long,
    paymentMethod: PaymentMethod,
    note: String,
  ): Result<Unit> {
    var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/supplier_debts/${debt.id}"
    return try {
      val uid = requireUserId()
      require(debt.id.isNotBlank()) { "ID utang tidak valid." }
      require(debt.status == "BELUM_LUNAS") { "Utang supplier ini sudah lunas." }
      require(paymentAmount > 0L && paymentAmount <= debt.remainingAmount) {
        "Nominal cicilan harus antara Rp1 sampai ${formatRupiah(debt.remainingAmount)}."
      }

      val now = System.currentTimeMillis()
      val methodCode =
        if (paymentMethod == PaymentMethod.UTANG) PaymentMethod.TUNAI.code else paymentMethod.code
      val newPaid = debt.paidAmount + paymentAmount
      val newRemaining = debt.totalDebt - newPaid
      val newStatus = if (newRemaining == 0L) "LUNAS" else "BELUM_LUNAS"
      val newPayments =
        debt.payments +
          DebtPayment(
            amount = paymentAmount,
            paidAtMillis = now,
            paymentMethod = methodCode,
            note = note.trim().ifBlank { if (newRemaining == 0L) "Pelunasan" else "Cicilan" },
          )

      val debtRef = userDoc(uid).collection("supplier_debts").document(debt.id)
      docPath = debtRef.path
      val batch = db.batch()
      batch.update(
        debtRef,
        mapOf(
          "paidAmount" to newPaid,
          "remainingAmount" to newRemaining,
          "status" to newStatus,
          "payments" to newPayments.take(100).map { it.toMap() },
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )

      val cashRef = userDoc(uid).collection("cash_transactions").document()
      batch.set(
        cashRef,
        CashTransaction(
          direction = "KELUAR",
          category = "Belanja Stok",
          amount = paymentAmount,
          paymentMethod = methodCode,
          description = "Bayar utang supplier ${debt.supplierName}" + if (newRemaining == 0L) " (Lunas)" else "",
          occurredAtMillis = now,
        ).toCreateMap(),
      )

      val actRef = userDoc(uid).collection("activities").document()
      batch.set(
        actRef,
        ActivityEntry(
          type = ActivityType.PEMBAYARAN_UTANG.code,
          title = "Bayar Utang Supplier: ${debt.supplierName}",
          subtitle = "Dibayar ${formatRupiah(paymentAmount)} • Sisa ${formatRupiah(newRemaining)}",
          amount = paymentAmount,
          occurredAtMillis = now,
        ).toCreateMap(),
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  private suspend fun deleteDocument(collectionName: String, docId: String): Result<Unit> {
    var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/$collectionName/$docId"
    return try {
      val uid = requireUserId()
      require(docId.isNotBlank()) { "ID dokumen tidak valid." }
      val docRef = userDoc(uid).collection(collectionName).document(docId)
      docPath = docRef.path
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }

  suspend fun deleteSale(saleId: String): Result<Unit> = deleteDocument("sales", saleId)

  suspend fun deletePurchase(purchaseId: String): Result<Unit> = deleteDocument("purchases", purchaseId)

  suspend fun deleteStockLog(logId: String): Result<Unit> = deleteDocument("stock_logs", logId)

  suspend fun deleteCashTransaction(txId: String): Result<Unit> = deleteDocument("cash_transactions", txId)

  suspend fun deleteCustomerDebt(debtId: String): Result<Unit> = deleteDocument("customer_debts", debtId)

  suspend fun deleteSupplierDebt(debtId: String): Result<Unit> = deleteDocument("supplier_debts", debtId)

  suspend fun deleteActivity(activityId: String): Result<Unit> = deleteDocument("activities", activityId)

  suspend fun updateCustomerDebtNote(debtId: String, note: String): Result<Unit> {
    var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/customer_debts/$debtId"
    return try {
      val uid = requireUserId()
      require(debtId.isNotBlank()) { "ID utang tidak valid." }
      val docRef = userDoc(uid).collection("customer_debts").document(debtId)
      docPath = docRef.path
      docRef.update(
        mapOf(
          "note" to note.trim().take(300),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun updateSupplierDebtNote(debtId: String, note: String): Result<Unit> {
    var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/supplier_debts/$debtId"
    return try {
      val uid = requireUserId()
      require(debtId.isNotBlank()) { "ID utang tidak valid." }
      val docRef = userDoc(uid).collection("supplier_debts").document(debtId)
      docPath = docRef.path
      docRef.update(
        mapOf(
          "note" to note.trim().take(300),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun resetStoreData(includeProducts: Boolean): Result<Int> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Pengguna belum masuk."))
    var currentPath = "users/$uid"
    return try {
      val collections =
        buildList {
          add("sales")
          add("purchases")
          add("stock_logs")
          add("cash_transactions")
          add("customer_debts")
          add("supplier_debts")
          add("activities")
          if (includeProducts) add("products")
        }

      val refsToDelete = mutableListOf<com.google.firebase.firestore.DocumentReference>()
      for (col in collections) {
        currentPath = "users/$uid/$col"
        val snap = userDoc(uid).collection(col).get().await()
        snap.documents.forEach { refsToDelete.add(it.reference) }
      }

      var deletedCount = 0
      refsToDelete.chunked(450).forEach { chunk ->
        val batch = db.batch()
        chunk.forEach { batch.delete(it) }
        batch.commit().await()
        deletedCount += chunk.size
      }

      if (!includeProducts) {
        // Zero out stocks if user chose to keep catalog products while resetting history
        val prodSnap = userDoc(uid).collection("products").get().await()
        prodSnap.documents.chunked(450).forEach { chunk ->
          val batch = db.batch()
          chunk.forEach { doc ->
            batch.update(
              doc.reference,
              mapOf("stock" to 0L, "updatedAt" to FieldValue.serverTimestamp()),
            )
          }
          batch.commit().await()
        }
      }

      Result.success(deletedCount)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, currentPath)
      Result.failure(e)
    }
  }
}

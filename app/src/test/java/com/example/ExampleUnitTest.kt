package com.example

import com.example.model.CashTransaction
import com.example.model.CustomerDebt
import com.example.model.Product
import com.example.model.Sale
import com.example.model.SupplierDebt
import com.example.viewmodel.HealthLevel
import com.example.viewmodel.StoreDataBundle
import com.example.viewmodel.StoreViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun productProfitAndMargin_areCalculatedAccurately() {
    val product =
      Product(
        id = "1",
        name = "Minyak Goreng 1L",
        costPrice = 15_000L,
        sellPrice = 20_000L,
        stock = 10L,
        minStock = 3L,
      )
    assertEquals(5_000L, product.profitPerUnit)
    assertEquals(25.0, product.marginPercent, 0.01)
    assertEquals(33.33, product.markupPercent, 0.05)
    assertEquals(false, product.lowStockWarning)
  }

  @Test
  fun productLowStockWarning_triggersAtOrBelowMinStock() {
    val product = Product(name = "Gula", stock = 3L, minStock = 3L)
    assertEquals(true, product.lowStockWarning)
  }

  @Test
  fun computeDashboard_countsShoppersAndUniqueCustomers() {
    val now = System.currentTimeMillis()
    val yesterday = now - 2 * 24 * 60 * 60 * 1000L
    val bundle =
      StoreDataBundle(
        sales =
          listOf(
            Sale(id = "s1", customerName = "Bu Ani", totalAmount = 50_000L, totalProfit = 10_000L, occurredAtMillis = now),
            Sale(id = "s2", customerName = "", totalAmount = 25_000L, totalProfit = 5_000L, occurredAtMillis = now),
            Sale(id = "s3", customerName = "bu ani ", totalAmount = 30_000L, totalProfit = 6_000L, occurredAtMillis = yesterday),
            Sale(id = "s4", customerName = "Pak Budi", totalAmount = 40_000L, totalProfit = 8_000L, occurredAtMillis = yesterday),
          )
      )

    val snapshot = StoreViewModel.computeDashboard(bundle)
    assertEquals(4, snapshot.totalSalesCount)
    assertEquals(2, snapshot.todayCustomerCount)
    assertEquals(2, snapshot.uniqueNamedCustomers)
  }

  @Test
  fun computeFinancialHealth_detectsHealthyVsRiskStates() {
    val now = System.currentTimeMillis()
    val healthyBundle =
      StoreDataBundle(
        products = listOf(Product(id = "p1", name = "Beras", stock = 20L, minStock = 5L)),
        sales = listOf(Sale(id = "s1", totalAmount = 200_000L, totalCost = 150_000L, totalProfit = 50_000L, occurredAtMillis = now)),
        cashTransactions = listOf(CashTransaction(id = "c1", direction = "MASUK", category = "Penjualan", amount = 200_000L, occurredAtMillis = now)),
      )
    val healthyReport = StoreViewModel.computeFinancialHealth(healthyBundle)
    assertEquals(HealthLevel.SEHAT, healthyReport.level)
    assertEquals(100, healthyReport.score)

    val riskyBundle =
      StoreDataBundle(
        products = listOf(Product(id = "p1", name = "Gula", stock = 1L, minStock = 5L)),
        sales = listOf(Sale(id = "s1", totalAmount = 100_000L, totalCost = 95_000L, totalProfit = 5_000L, occurredAtMillis = now)),
        cashTransactions = listOf(
          CashTransaction(id = "c1", direction = "MASUK", category = "Penjualan", amount = 100_000L, occurredAtMillis = now),
          CashTransaction(id = "c2", direction = "KELUAR", category = "Biaya Operasional", amount = 250_000L, occurredAtMillis = now),
        ),
        customerDebts = listOf(CustomerDebt(id = "d1", customerName = "X", totalDebt = 90_000L, remainingAmount = 90_000L, status = "BELUM_LUNAS")),
        supplierDebts = listOf(SupplierDebt(id = "sd1", supplierName = "Agen", totalDebt = 300_000L, remainingAmount = 300_000L, status = "BELUM_LUNAS")),
      )
    val riskyReport = StoreViewModel.computeFinancialHealth(riskyBundle)
    assertTrue(riskyReport.score < 50)
    assertTrue(riskyReport.adviceList.any { it.priorityHigh })
  }
}

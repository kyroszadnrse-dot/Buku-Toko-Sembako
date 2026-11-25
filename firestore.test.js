const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_toko_123";
const BOB_UID = "bob_toko_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied all reads and writes across toko collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("products").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("products").doc("prod_1").set({
      name: "Beras Premium",
      category: "Beras",
      costPrice: 65000,
      sellPrice: 72000,
      stock: 10,
      unit: "sak",
      minStock: 2,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("sales").get());
});

test("Owner Product CRUD: valid Kotlin payload, partial update, shadow field & @DocumentId rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Valid create (no 'id' field, optional photoPath omitted)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("prod_1").set({
      name: "Minyak Goreng 1L",
      category: "Minyak",
      costPrice: 15500,
      sellPrice: 18000,
      stock: 24,
      unit: "pcs",
      minStock: 5,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid partial update (stock + sellPrice + updatedAt)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("prod_1").update({
      stock: 22,
      sellPrice: 18500,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("prod_1").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost 'id' field or shadow field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("prod_2").set({
      id: "prod_2",
      name: "Gula Pasir",
      category: "Gula",
      costPrice: 15000,
      sellPrice: 17000,
      stock: 10,
      unit: "kg",
      minStock: 3,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("prod_3").set({
      name: "Kopi Sachet",
      category: "Kopi",
      costPrice: 12000,
      sellPrice: 15000,
      stock: 10,
      unit: "renceng",
      minStock: 2,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Cross-user isolation: Bob cannot read or write Alice's store data", async () => {
  const past = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).collection("products").doc("prod_1").set({
      name: "Telur Ayam",
      category: "Telur",
      costPrice: 26000,
      sellPrice: 29000,
      stock: 15,
      unit: "kg",
      minStock: 3,
      createdAt: past,
      updatedAt: past,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("products").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("products").doc("prod_1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("products").doc("prod_1").update({
      stock: 0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Sales, Purchases, StockLogs, CashTransactions, and Activities write & list parity", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);
  const userDoc = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(
    userDoc.collection("sales").doc("sale_1").set({
      receiptNumber: "TRX-001",
      customerName: "Ibu Sari",
      paymentMethod: "TUNAI",
      paymentStatus: "LUNAS",
      items: [
        {
          productId: "prod_1",
          productName: "Minyak Goreng",
          unit: "pcs",
          quantity: 2,
          costPrice: 15500,
          sellPrice: 18000,
          subtotal: 36000,
          profit: 5000,
        },
      ],
      totalAmount: 36000,
      totalCost: 31000,
      totalProfit: 5000,
      paidAmount: 50000,
      changeAmount: 14000,
      remainingDebt: 0,
      occurredAtMillis: Date.now(),
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    userDoc.collection("purchases").doc("pur_1").set({
      productId: "prod_1",
      productName: "Minyak Goreng",
      unit: "pcs",
      quantity: 12,
      buyPrice: 15500,
      totalCost: 186000,
      paidAmount: 186000,
      supplierName: "Agen Berkah",
      paymentMethod: "TUNAI",
      paymentStatus: "LUNAS",
      occurredAtMillis: Date.now(),
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    userDoc.collection("stock_logs").doc("log_1").set({
      productId: "prod_1",
      productName: "Minyak Goreng",
      changeType: "MASUK",
      quantityDelta: 12,
      stockBefore: 0,
      stockAfter: 12,
      oldCostPrice: 15000,
      newCostPrice: 15500,
      note: "Belanja dari Agen Berkah",
      occurredAtMillis: Date.now(),
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    userDoc.collection("cash_transactions").doc("tx_1").set({
      direction: "MASUK",
      category: "Penjualan",
      amount: 36000,
      paymentMethod: "TUNAI",
      description: "Penjualan TRX-001",
      occurredAtMillis: Date.now(),
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    userDoc.collection("activities").doc("act_1").set({
      type: "PENJUALAN",
      title: "Penjualan TRX-001",
      subtitle: "Ibu Sari • Tunai",
      amount: 36000,
      occurredAtMillis: Date.now(),
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(userDoc.collection("sales").get());
  await assertSucceeds(userDoc.collection("purchases").get());
  await assertSucceeds(userDoc.collection("stock_logs").get());
  await assertSucceeds(userDoc.collection("cash_transactions").get());
  await assertSucceeds(userDoc.collection("activities").get());
});

test("Customer & Supplier Debts: installment updates and terminal LUNAS locking", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const debtRef = aliceDb.collection("users").doc(ALICE_UID).collection("customer_debts").doc("debt_1");

  await assertSucceeds(
    debtRef.set({
      customerName: "Pak Budi",
      phone: "08123456789",
      totalDebt: 50000,
      paidAmount: 0,
      remainingAmount: 50000,
      status: "BELUM_LUNAS",
      note: "Utang beras 5kg",
      payments: [],
      occurredAtMillis: Date.now() - 5000,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial payment
  await assertSucceeds(
    debtRef.update({
      paidAmount: 20000,
      remainingAmount: 30000,
      status: "BELUM_LUNAS",
      payments: [{ amount: 20000, paidAtMillis: Date.now(), paymentMethod: "TUNAI", note: "Cicilan 1" }],
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Final payoff -> LUNAS
  await assertSucceeds(
    debtRef.update({
      paidAmount: 50000,
      remainingAmount: 0,
      status: "LUNAS",
      payments: [
        { amount: 20000, paidAtMillis: Date.now() - 2000, paymentMethod: "TUNAI", note: "Cicilan 1" },
        { amount: 30000, paidAtMillis: Date.now(), paymentMethod: "TUNAI", note: "Pelunasan" },
      ],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Terminal state locking: cannot modify after LUNAS
  await assertFails(
    debtRef.update({
      paidAmount: 40000,
      remainingAmount: 10000,
      status: "BELUM_LUNAS",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

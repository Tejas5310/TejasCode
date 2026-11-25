const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_dev_123";
const BOB_UID = "bob_dev_456";
const ADMIN_UID = "tejas_admin_001";
const ADMIN_EMAIL = "tejashpatel02010@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("plan_configs").doc("active").get());
  await assertFails(unauthDb.collection("courses").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("subscriptions").where("userId", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("settlements").get());
});

test("User profile: student registration fields, partial update, cross-user denial, ghost-field rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Coder",
      learningGoal: "Full-Stack Web",
      experienceLevel: "Beginner",
      registered: true,
      xp: 120,
      streakDays: 3,
      lessonsCompleted: 2,
      battlesWon: 1,
      lastActiveDate: "2026-11-18",
      completedLessonIds: ["html_1", "css_1"],
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      xp: 220,
      bio: "Learning Kotlin & Python",
      preferredLanguage: "hi",
      lessonsCompleted: 3,
      completedLessonIds: ["html_1", "css_1", "js_1"],
      updatedAt: past,
    })
  );

  // Students cannot read another user's profile or inject sensitive fields
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      cardNumber: "4111111111111111",
      updatedAt: past,
    })
  );
});

test("Zero-Trust Billing & Entitlements: clients cannot forge Pro entitlements, subscriptions, transactions, or settlements", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();
  const past = new Date(Date.now() - 5000);

  // Seed server-authoritative billing docs with rules disabled
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const sysDb = ctx.firestore();
    await sysDb.collection("users").doc(ALICE_UID).collection("entitlements").doc("current").set({
      userId: ALICE_UID,
      tier: "pro",
      status: "Active",
      provider: "razorpay",
      billingInterval: "monthly",
      cancelAtPeriodEnd: false,
      updatedAt: past,
    });
    await sysDb.collection("subscriptions").doc("sub_alice_1").set({
      subscriptionId: "sub_alice_1",
      userId: ALICE_UID,
      provider: "razorpay",
      status: "Active",
      billingInterval: "monthly",
      amountMinorUnits: 49900,
      currency: "INR",
    });
    await sysDb.collection("payment_transactions").doc("tx_alice_1").set({
      transactionId: "tx_alice_1",
      userId: ALICE_UID,
      provider: "razorpay",
      grossAmountMinor: 49900,
      currency: "INR",
      status: "captured",
    });
    await sysDb.collection("settlements").doc("setl_1").set({
      settlementId: "setl_1",
      provider: "razorpay",
      netPayoutMinor: 48722,
      status: "settled",
    });
  });

  // Owner can read own entitlement, subscription, transaction
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("entitlements").doc("current").get()
  );
  await assertSucceeds(
    aliceDb.collection("subscriptions").where("userId", "==", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("payment_transactions").where("userId", "==", ALICE_UID).get()
  );

  // Owner CANNOT write/forge entitlements, subscriptions, transactions, or read settlements
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("entitlements").doc("current").set({
      userId: ALICE_UID,
      tier: "pro",
      status: "Active",
    })
  );
  await assertFails(
    aliceDb.collection("subscriptions").doc("sub_forged").set({
      userId: ALICE_UID,
      status: "Active",
    })
  );
  await assertFails(aliceDb.collection("settlements").get());

  // Cross-user billing access denied
  await assertFails(bobDb.collection("subscriptions").doc("sub_alice_1").get());
  await assertFails(bobDb.collection("payment_transactions").doc("tx_alice_1").get());

  // Admin can read settlements & configure plan pricing; students cannot mutate plan_configs
  await assertSucceeds(adminDb.collection("settlements").get());
  await assertSucceeds(
    adminDb.collection("plan_configs").doc("active").set({
      monthlyPriceMinor: 49900,
      yearlyPriceMinor: 399900,
      currency: "INR",
      currencySymbol: "₹",
      trialDays: 7,
      monthlyPlayProductId: "tejascode_pro_monthly",
      monthlyPlayBasePlanId: "monthly-base",
      yearlyPlayProductId: "tejascode_pro_yearly",
      yearlyPlayBasePlanId: "yearly-base",
      razorpayMonthlyPlanId: "plan_monthly_01",
      razorpayYearlyPlanId: "plan_yearly_01",
      razorpayEnabled: true,
      freeDailyAiLimit: 5,
      proDailyAiLimit: 100,
      billingTermsSummary: "Renews automatically until cancelled.",
      updatedBy: ADMIN_UID,
      updatedAt: past,
    })
  );
  await assertSucceeds(aliceDb.collection("plan_configs").doc("active").get());
  await assertFails(
    aliceDb.collection("plan_configs").doc("active").update({
      monthlyPriceMinor: 1,
      updatedAt: past,
    })
  );
});

test("Admin Courses & Community Moderation: verified admin CRUD; spoofed/student blocked", async () => {
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();
  const spoofedDb = testEnv
    .authenticatedContext("spoof_uid", { email: ADMIN_EMAIL, email_verified: false })
    .firestore();
  const studentDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 5000);

  const coursePayload = {
    title: "Python Data Structures",
    subtitle: "Lists, Dictionaries, Sets, and Tuples",
    language: "Python",
    category: "DATA_AI",
    difficulty: "Intermediate",
    durationMinutes: 10,
    xpReward: 150,
    isPremium: true,
    resourceUrl: "https://tejascode.dev/guides/py-ds",
    starterCode: "items = [1, 2, 3]\nprint(items)",
    lessonSummary: "Use lists and dicts for fast lookups.",
    quizQuestion: "Which structure stores key-value pairs in Python?",
    quizOptions: ["dict", "tuple", "set", "int"],
    correctOptionIndex: 0,
    visibility: "public",
    createdBy: ADMIN_UID,
    createdAt: past,
    updatedAt: past,
  };

  await assertSucceeds(adminDb.collection("courses").doc("course_py_ds").set(coursePayload));
  await assertFails(spoofedDb.collection("courses").doc("course_spoof").set({ ...coursePayload, createdBy: "spoof_uid" }));
  await assertFails(studentDb.collection("courses").doc("course_stu").set({ ...coursePayload, createdBy: ALICE_UID }));
  await assertSucceeds(studentDb.collection("courses").where("visibility", "==", "public").get());

  // Community Q&A + Moderation
  await assertSucceeds(
    studentDb.collection("community_qa").doc("qa_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      title: "How do Python decorators work?",
      body: "Can someone show a minimal timing decorator?",
      language: "Python",
      codeSnippet: "def timer(fn): return fn",
      likesCount: 0,
      likedBy: [],
      replyCount: 0,
      visibility: "public",
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    studentDb.collection("moderation_reports").doc("rep_1").set({
      reporterId: ALICE_UID,
      targetType: "community_qa",
      targetId: "qa_1",
      reason: "Spam link",
      status: "open",
      createdAt: past,
    })
  );
  await assertFails(studentDb.collection("moderation_reports").get());
  await assertSucceeds(adminDb.collection("moderation_reports").get());
});

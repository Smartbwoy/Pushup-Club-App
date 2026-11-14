const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const serverTimestamp = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const CHARLIE_UID = "charlie_uid_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validMemberStats(name, weekly = 50, total = 200, streak = 3, best = 25) {
  return {
    displayName: name,
    weeklyPushUps: weekly,
    totalPushUps: total,
    streakDays: streak,
    personalBest: best,
  };
}

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

test("Unauthenticated caller cannot read or write users or clubs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("clubs").doc("PUSH7X").get());
  await assertFails(unauthDb.collection("clubs").get());
});

test("User profile enforces strict owner isolation and rejects ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      weeklyPushUps: 40,
      totalPushUps: 120,
      streakDays: 4,
      personalBest: 20,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Ghost field injection fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      weeklyPushUps: 40,
      totalPushUps: 120,
      streakDays: 4,
      personalBest: 20,
      isAdmin: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Cross-user read/write fails
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hacked",
      updatedAt: serverTimestamp(),
    })
  );
});

test("Club creation, scoped list query, join by code, stats update, and leave work securely", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const clubRefAlice = aliceDb.collection("clubs").doc("PUSH7X");

  // Alice creates a club
  await assertSucceeds(
    clubRefAlice.set({
      name: "Morning Squad",
      joinCode: "PUSH7X",
      ownerId: ALICE_UID,
      memberUids: [ALICE_UID],
      memberStats: {
        [ALICE_UID]: validMemberStats("Alice", 70, 300, 5, 30),
      },
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Scoped query succeeds for Alice
  await assertSucceeds(
    aliceDb.collection("clubs").where("memberUids", "array-contains", ALICE_UID).get()
  );

  // Unscoped list fails (Query Trust Test)
  await assertFails(bobDb.collection("clubs").get());

  // Bob looks up club by exact join code and joins cleanly
  const bobClubSnap = await assertSucceeds(bobDb.collection("clubs").doc("PUSH7X").get());
  const bobExisting = bobClubSnap.data();
  await assertSucceeds(
    bobDb.collection("clubs").doc("PUSH7X").update({
      memberUids: [...bobExisting.memberUids, BOB_UID],
      [`memberStats.${BOB_UID}`]: validMemberStats("Bob", 55, 190, 2, 22),
      updatedAt: serverTimestamp(),
    })
  );

  // Bob tries to overwrite Alice's stats -> MUST FAIL
  await assertFails(
    bobDb.collection("clubs").doc("PUSH7X").update({
      [`memberStats.${ALICE_UID}`]: validMemberStats("Alice", 0, 0, 0, 0),
      updatedAt: serverTimestamp(),
    })
  );

  // Bob updates ONLY his own stats -> SUCCEEDS
  await assertSucceeds(
    bobDb.collection("clubs").doc("PUSH7X").update({
      [`memberStats.${BOB_UID}`]: validMemberStats("Bob", 85, 220, 3, 25),
      updatedAt: serverTimestamp(),
    })
  );

  // Non-member Charlie cannot update stats without joining
  await assertFails(
    charlieDb.collection("clubs").doc("PUSH7X").update({
      [`memberStats.${CHARLIE_UID}`]: validMemberStats("Charlie", 99, 99, 1, 99),
      updatedAt: serverTimestamp(),
    })
  );

  // Non-owner Bob cannot delete or rename the club
  await assertFails(bobDb.collection("clubs").doc("PUSH7X").delete());
  await assertFails(
    bobDb.collection("clubs").doc("PUSH7X").update({
      name: "Bob Hijack",
      updatedAt: serverTimestamp(),
    })
  );

  // Non-owner Bob can leave the club cleanly
  const beforeLeaveSnap = await bobDb.collection("clubs").doc("PUSH7X").get();
  const dataBeforeLeave = beforeLeaveSnap.data();
  const nextStats = { ...dataBeforeLeave.memberStats };
  delete nextStats[BOB_UID];
  await assertSucceeds(
    bobDb.collection("clubs").doc("PUSH7X").update({
      memberUids: dataBeforeLeave.memberUids.filter((u) => u !== BOB_UID),
      memberStats: nextStats,
      updatedAt: serverTimestamp(),
    })
  );

  // Owner Alice can delete the club
  await assertSucceeds(clubRefAlice.delete());
});

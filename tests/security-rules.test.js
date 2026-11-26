const { initializeTestEnvironment, assertSucceeds, assertFails } = require("@firebase/rules-unit-testing");
const { doc, setDoc, getDoc, updateDoc, serverTimestamp } = require("firebase/firestore");
const fs = require("fs");

const PROJECT_ID = "demo-stepfit-rules";
const CHALLENGE_ID = "weekly-walk";
const DAY = "2026-11-19";
const ENTRY_ID = "alice_2026-11-19";

let env;

async function seedClub() {
  await env.withSecurityRulesDisabled(async context => {
    await setDoc(doc(context.firestore(), "clubs/walkers"), {
      name: "Walkers",
      ownerUid: "alice",
      memberUids: ["alice"]
    });
  });
}

async function seedChallengeAndScore() {
  await env.withSecurityRulesDisabled(async context => {
    const db = context.firestore();
    await setDoc(doc(db, `challenges/${CHALLENGE_ID}`), {
      name: "Weekly walking challenge",
      participantUids: ["alice"]
    });
    await setDoc(doc(db, `challenges/${CHALLENGE_ID}/dailyProgress/${ENTRY_ID}`), {
      uid: "alice",
      day: DAY,
      steps: 4200,
      updatedAt: new Date()
    });
  });
}

before(async () => {
  env = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8")
    }
  });
});

after(async () => {
  if (env) await env.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
});

describe("StepFit PRO profile field rules", () => {
  it("allows a user to create only the supported fields in their own profile", async () => {
    const alice = env.authenticatedContext("alice").firestore();
    await assertSucceeds(setDoc(doc(alice, "users/alice"), {
      uid: "alice",
      email: "alice@example.com",
      name: "Alice",
      age: "30",
      gender: "",
      updatedAt: serverTimestamp()
    }));
  });

  it("rejects self-created profiles with blocked or arbitrary admin fields", async () => {
    const alice = env.authenticatedContext("alice").firestore();
    await assertFails(setDoc(doc(alice, "users/alice"), {
      uid: "alice",
      name: "Alice",
      blocked: true
    }));
    await assertFails(setDoc(doc(alice, "users/alice"), {
      uid: "alice",
      name: "Alice",
      admin: true
    }));
  });

  it("rejects profile creation under another user's document ID", async () => {
    const alice = env.authenticatedContext("alice").firestore();
    await assertFails(setDoc(doc(alice, "users/bob"), {
      uid: "bob",
      name: "Bob"
    }));
  });

  it("rejects a user changing their own protected blocked field", async () => {
    await env.withSecurityRulesDisabled(async context => {
      await setDoc(doc(context.firestore(), "users/alice"), {
        uid: "alice",
        name: "Alice",
        blocked: false
      });
    });
    const alice = env.authenticatedContext("alice").firestore();
    await assertFails(setDoc(doc(alice, "users/alice"), {
      blocked: true
    }, { merge: true }));
  });
});

describe("StepFit PRO club membership rules", () => {
  it("allows a user to add only their own UID when joining a club", async () => {
    await seedClub();
    const bob = env.authenticatedContext("bob").firestore();
    await assertSucceeds(updateDoc(doc(bob, "clubs/walkers"), {
      memberUids: ["alice", "bob"]
    }));
  });

  it("rejects an existing club member adding another account", async () => {
    await seedClub();
    const alice = env.authenticatedContext("alice").firestore();
    await assertFails(updateDoc(doc(alice, "clubs/walkers"), {
      memberUids: ["alice", "charlie"]
    }));
  });

  it("rejects a new member adding themselves and a second account together", async () => {
    await seedClub();
    const bob = env.authenticatedContext("bob").firestore();
    await assertFails(updateDoc(doc(bob, "clubs/walkers"), {
      memberUids: ["alice", "bob", "charlie"]
    }));
  });
});

describe("StepFit PRO challenge membership rules", () => {
  it("allows a user to join a challenge by adding only their own UID", async () => {
    await seedChallengeAndScore();
    const bob = env.authenticatedContext("bob").firestore();
    await assertSucceeds(updateDoc(doc(bob, `challenges/${CHALLENGE_ID}`), {
      participantUids: ["alice", "bob"]
    }));
  });

  it("rejects an existing participant adding a different account", async () => {
    await seedChallengeAndScore();
    const alice = env.authenticatedContext("alice").firestore();
    await assertFails(updateDoc(doc(alice, `challenges/${CHALLENGE_ID}`), {
      participantUids: ["alice", "charlie"]
    }));
  });

  it("rejects a new participant adding themselves and another account together", async () => {
    await seedChallengeAndScore();
    const bob = env.authenticatedContext("bob").firestore();
    await assertFails(updateDoc(doc(bob, `challenges/${CHALLENGE_ID}`), {
      participantUids: ["alice", "bob", "charlie"]
    }));
  });
});

describe("StepFit PRO challenge score rules", () => {
  it("rejects unauthenticated reads and writes", async () => {
    await seedChallengeAndScore();
    const anonymous = env.unauthenticatedContext().firestore();
    const scorePath = `challenges/${CHALLENGE_ID}/dailyProgress/${ENTRY_ID}`;

    await assertFails(getDoc(doc(anonymous, scorePath)));
    await assertFails(setDoc(doc(anonymous, `challenges/${CHALLENGE_ID}/dailyProgress/anonymous-entry`), {
      uid: "anonymous",
      day: DAY,
      steps: 1000,
      updatedAt: serverTimestamp()
    }));
  });

  it("rejects a blocked participant reading challenge scores", async () => {
    await seedChallengeAndScore();
    await env.withSecurityRulesDisabled(async context => {
      await setDoc(doc(context.firestore(), "users/alice"), {
        blocked: true
      });
    });

    const alice = env.authenticatedContext("alice").firestore();
    const scorePath = `challenges/${CHALLENGE_ID}/dailyProgress/${ENTRY_ID}`;
    await assertFails(getDoc(doc(alice, scorePath)));
  });

  it("rejects a participant creating a client-submitted score", async () => {
    await seedChallengeAndScore();
    const alice = env.authenticatedContext("alice").firestore();
    await assertFails(setDoc(
      doc(alice, `challenges/${CHALLENGE_ID}/dailyProgress/alice_2026-11-20`),
      {
        uid: "alice",
        day: "2026-11-20",
        steps: 9000,
        updatedAt: serverTimestamp()
      }
    ));
  });

  it("rejects a participant changing an existing score", async () => {
    await seedChallengeAndScore();
    const alice = env.authenticatedContext("alice").firestore();
    await assertFails(setDoc(
      doc(alice, `challenges/${CHALLENGE_ID}/dailyProgress/${ENTRY_ID}`),
      {
        uid: "alice",
        day: DAY,
        steps: 999999,
        updatedAt: serverTimestamp()
      }
    ));
  });

  it("allows an administrator to write challenge progress", async () => {
    await seedChallengeAndScore();
    const admin = env.authenticatedContext("owner", { admin: true }).firestore();
    await assertSucceeds(setDoc(
      doc(admin, `challenges/${CHALLENGE_ID}/dailyProgress/admin-entry`),
      {
        uid: "alice",
        day: DAY,
        steps: 4200,
        updatedAt: serverTimestamp()
      }
    ));
  });

  it("allows participants to read scores but denies non-participants", async () => {
    await seedChallengeAndScore();
    const alice = env.authenticatedContext("alice").firestore();
    const bob = env.authenticatedContext("bob").firestore();
    const scorePath = `challenges/${CHALLENGE_ID}/dailyProgress/${ENTRY_ID}`;
    await assertSucceeds(getDoc(doc(alice, scorePath)));
    await assertFails(getDoc(doc(bob, scorePath)));
  });
});

const { initializeTestEnvironment, assertSucceeds, assertFails } = require("@firebase/rules-unit-testing");
const { doc, setDoc, getDoc, serverTimestamp, collection, addDoc, updateDoc, deleteDoc } = require("firebase/firestore");
const { ref, uploadBytes, getDownloadURL } = require("firebase/storage");

const PROJECT_ID = "step-fit-pro-8e0ce";
let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: { rules: require("fs").readFileSync("firestore.rules", "utf8") },
    storage: { rules: require("fs").readFileSync("storage.rules", "utf8") }
  });
});

after(async () => {
  if (env) await env.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
  await env.clearStorage();
});

describe("StepFit PRO Firestore rules", () => {
  it("allows a user to create and read only their own profile", async () => {
    const db = env.authenticatedContext("alice").firestore();
    await assertSucceeds(setDoc(doc(db, "users/alice"), {
      uid: "alice", email: "alice@example.test", name: "Alice",
      age: "30", gender: "", updatedAt: serverTimestamp()
    }));
    await assertSucceeds(getDoc(doc(db, "users/alice")));
    await assertFails(getDoc(doc(db, "users/bob")));
  });

  it("prevents users from assigning themselves blocked/admin fields", async () => {
    const db = env.authenticatedContext("alice").firestore();
    await assertFails(setDoc(doc(db, "users/alice"), {
      uid: "alice", name: "Alice", blocked: true, admin: true
    }));
  });

  it("allows own daily stats with server timestamp and rejects negative steps", async () => {
    const db = env.authenticatedContext("alice").firestore();
    await assertSucceeds(setDoc(doc(db, "users/alice/dailyStats/2026-11-08"), {
      date: "2026-11-08", steps: 1200, distanceKm: 0.8,
      calories: 35, updatedAt: serverTimestamp()
    }));
    await assertFails(setDoc(doc(db, "users/alice/dailyStats/2026-11-09"), {
      date: "2026-11-09", steps: -1, distanceKm: 0,
      calories: 0, updatedAt: serverTimestamp()
    }));
  });

  it("allows private GPS routes only for their owner and validates route data", async () => {
    const alice = env.authenticatedContext("alice").firestore();
    const bob = env.authenticatedContext("bob").firestore();
    const route = {
      routeId: "r1731060000000", savedAt: 1731060000000,
      distanceKm: 2.5, durationSeconds: 1800,
      points: "18.5204,73.8567,1731060000000;18.5210,73.8570,1731060001000",
      updatedAt: serverTimestamp()
    };
    await assertSucceeds(setDoc(doc(alice, "users/alice/routes/r1731060000000"), route));
    await assertSucceeds(getDoc(doc(alice, "users/alice/routes/r1731060000000")));
    await assertFails(getDoc(doc(bob, "users/alice/routes/r1731060000000")));
    await assertFails(setDoc(doc(alice, "users/alice/routes/bad"), {
      ...route, routeId: "not-the-document-id"
    }));
    await assertFails(setDoc(doc(alice, "users/alice/routes/negative"), {
      ...route, routeId: "negative", distanceKm: -1
    }));
    await assertFails(setDoc(doc(alice, "users/alice/routes/extra"), {
      ...route, routeId: "extra", sharedWith: ["bob"]
    }));
  });

  it("allows only the route owner to delete a private GPS route", async () => {
    await env.withSecurityRulesDisabled(async context => {
      await setDoc(doc(context.firestore(), "users/alice/routes/route1"), {
        routeId: "route1", savedAt: 1731060000000, distanceKm: 1.2,
        durationSeconds: 600, points: "18.5204,73.8567;18.5210,73.8570",
        updatedAt: new Date()
      });
    });
    const alice = env.authenticatedContext("alice").firestore();
    const bob = env.authenticatedContext("bob").firestore();
    await assertFails(deleteDoc(doc(bob, "users/alice/routes/route1")));
    await assertSucceeds(deleteDoc(doc(alice, "users/alice/routes/route1")));
  });

  it("allows a friend request when the recipient profile has no blocked field", async () => {
    await env.withSecurityRulesDisabled(async context => {
      await setDoc(doc(context.firestore(), "users/alice"), { uid: "alice" });
      await setDoc(doc(context.firestore(), "users/bob"), { uid: "bob", name: "Bob" });
    });
    const db = env.authenticatedContext("alice").firestore();
    await assertSucceeds(addDoc(collection(db, "friendRequests"), {
      fromUid: "alice", toUid: "bob", status: "pending", createdAt: serverTimestamp()
    }));
  });

  it("allows a club owner to create a club and a new member to join", async () => {
    const alice = env.authenticatedContext("alice").firestore();
    await assertSucceeds(setDoc(doc(alice, "clubs/club1"), {
      name: "Walkers", ownerUid: "alice", memberUids: ["alice"]
    }));
    const bob = env.authenticatedContext("bob").firestore();
    await assertSucceeds(updateDoc(doc(bob, "clubs/club1"), {
      memberUids: ["alice", "bob"]
    }));
  });

  it("rejects client-submitted challenge scores, including a participant's own score", async () => {
    await env.withSecurityRulesDisabled(async context => {
      await setDoc(doc(context.firestore(), "challenges/ch1"), {
        name: "Steps", participantUids: ["alice"]
      });
      await setDoc(doc(context.firestore(), "challenges/ch1/dailyProgress/alice_2026-11-08"), {
        uid: "alice", day: "2026-11-08", steps: 4000, updatedAt: new Date()
      });
    });
    const alice = env.authenticatedContext("alice").firestore();
    await assertFails(setDoc(doc(alice, "challenges/ch1/dailyProgress/alice_2026-11-08"), {
      uid: "alice", day: "2026-11-08", steps: 5000, updatedAt: serverTimestamp()
    }));
    await assertFails(setDoc(doc(alice, "challenges/ch1/dailyProgress/bob_2026-11-08"), {
      uid: "bob", day: "2026-11-08", steps: 5000, updatedAt: serverTimestamp()
    }));
  });

  it("allows recipient to accept a friend request but not the sender", async () => {
    await env.withSecurityRulesDisabled(async context => {
      await setDoc(doc(context.firestore(), "users/alice"), { uid: "alice" });
      await setDoc(doc(context.firestore(), "users/bob"), { uid: "bob" });
      await setDoc(doc(context.firestore(), "friendRequests/request1"), {
        fromUid: "alice", toUid: "bob", status: "pending"
      });
    });
    const alice = env.authenticatedContext("alice").firestore();
    const bob = env.authenticatedContext("bob").firestore();
    await assertFails(updateDoc(doc(alice, "friendRequests/request1"), { status: "accepted" }));
    await assertSucceeds(updateDoc(doc(bob, "friendRequests/request1"), { status: "accepted" }));
  });

  it("restricts admin-only challenge creation", async () => {
    const ordinary = env.authenticatedContext("alice").firestore();
    const admin = env.authenticatedContext("owner", { admin: true }).firestore();
    await assertFails(addDoc(collection(ordinary, "challenges"), { name: "Test" }));
    await assertSucceeds(addDoc(collection(admin, "challenges"), {
      name: "Test", targetSteps: 1000, participantUids: [], createdBy: "owner"
    }));
  });

  it("blocks a suspended user's cloud access", async () => {
    await env.withSecurityRulesDisabled(async context => {
      await setDoc(doc(context.firestore(), "users/alice"), { uid: "alice", blocked: true });
    });
    const db = env.authenticatedContext("alice").firestore();
    await assertFails(getDoc(doc(db, "users/alice")));
  });
});

describe("StepFit PRO Storage rules", () => {
  it("allows an owner to upload an image to their profile folder", async () => {
    const storage = env.authenticatedContext("alice").storage();
    await assertSucceeds(uploadBytes(ref(storage, "profilePhotos/alice/avatar"), new Uint8Array([1,2,3]), {
      contentType: "image/jpeg"
    }));
  });

  it("allows profile photo upload when an existing profile has no blocked field", async () => {
    await env.withSecurityRulesDisabled(async context => {
      await setDoc(doc(context.firestore(), "users/alice"), { uid: "alice", name: "Alice" });
    });
    const storage = env.authenticatedContext("alice").storage();
    await assertSucceeds(uploadBytes(ref(storage, "profilePhotos/alice/avatar"), new Uint8Array([1,2,3]), {
      contentType: "image/jpeg"
    }));
  });

  it("rejects uploading another user's profile image", async () => {
    const storage = env.authenticatedContext("alice").storage();
    await assertFails(uploadBytes(ref(storage, "profilePhotos/bob/avatar"), new Uint8Array([1,2,3]), {
      contentType: "image/jpeg"
    }));
  });

  it("rejects non-image profile uploads", async () => {
    const storage = env.authenticatedContext("alice").storage();
    await assertFails(uploadBytes(ref(storage, "profilePhotos/alice/avatar.txt"), new Uint8Array([1,2,3]), {
      contentType: "text/plain"
    }));
  });
});

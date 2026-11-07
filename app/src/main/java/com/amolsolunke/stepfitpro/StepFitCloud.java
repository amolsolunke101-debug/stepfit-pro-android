package com.amolsolunke.stepfitpro;

import android.net.Uri;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Cloud repository for account-owned StepFit data.
 * Authorization is enforced by Firestore/Storage rules, never by this UI class.
 */
public final class StepFitCloud {
    private static final FirebaseFirestore DB = FirebaseFirestore.getInstance();
    private static final FirebaseStorage STORAGE = FirebaseStorage.getInstance();
    private StepFitCloud() {}

    public interface Result { void done(boolean ok, String message); }

    public static String uid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user == null ? null : user.getUid();
    }

    public static void syncProfile(String name, String age, String gender, Result result) {
        String id = uid();
        if (id == null) { result.done(false, "Sign in to sync your profile."); return; }
        Map<String,Object> data = new HashMap<>();
        data.put("uid", id);
        data.put("email", FirebaseAuth.getInstance().getCurrentUser().getEmail());
        data.put("name", name == null ? "" : name);
        data.put("age", age == null ? "" : age);
        data.put("gender", gender == null ? "" : gender);
        data.put("updatedAt", FieldValue.serverTimestamp());
        DB.collection("users").document(id).set(data, com.google.firebase.firestore.SetOptions.merge())
          .addOnSuccessListener(v -> result.done(true, "Profile synced."))
          .addOnFailureListener(e -> result.done(false, e.getLocalizedMessage()));
    }

    public static void uploadProfilePhoto(Uri photo, Result result) {
        String id = uid();
        if (id == null || photo == null) { result.done(false, "Sign in and choose a photo."); return; }
        StorageReference ref = STORAGE.getReference().child("profilePhotos/" + id + "/avatar");
        ref.putFile(photo).continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return ref.getDownloadUrl();
        }).addOnSuccessListener(url ->
            DB.collection("users").document(id).set(
                java.util.Collections.singletonMap("photoUrl", url.toString()),
                com.google.firebase.firestore.SetOptions.merge())
              .addOnSuccessListener(v -> result.done(true, "Profile photo synced."))
              .addOnFailureListener(e -> result.done(false, e.getLocalizedMessage()))
        ).addOnFailureListener(e -> result.done(false, e.getLocalizedMessage()));
    }

    public static void saveDailyStats(long steps, double distanceKm, double calories, Result result) {
        String id = uid();
        if (id == null) return;
        String day = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(new java.util.Date());
        Map<String,Object> data = new HashMap<>();
        data.put("date", day); data.put("steps", steps);
        data.put("distanceKm", distanceKm); data.put("calories", calories);
        data.put("updatedAt", FieldValue.serverTimestamp());
        DB.collection("users").document(id).collection("dailyStats").document(day).set(data)
          .addOnSuccessListener(v -> { if (result != null) result.done(true, "Activity synced."); })
          .addOnFailureListener(e -> { if (result != null) result.done(false, e.getLocalizedMessage()); });
    }

    public static void sendFriendRequest(String targetUid, Result result) {
        String id = uid();
        if (id == null || targetUid == null || targetUid.trim().isEmpty() || id.equals(targetUid)) {
            result.done(false, "Enter another user's valid ID."); return;
        }
        String target = targetUid.trim();
        // Check both directions to avoid duplicate pending requests.
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> outgoing =
            DB.collection("friendRequests").whereEqualTo("fromUid", id)
                .whereEqualTo("toUid", target).whereEqualTo("status", "pending").get();
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> incoming =
            DB.collection("friendRequests").whereEqualTo("fromUid", target)
                .whereEqualTo("toUid", id).whereEqualTo("status", "pending").get();
        com.google.android.gms.tasks.Tasks.whenAllSuccess(outgoing, incoming)
          .addOnSuccessListener(results -> {
              boolean exists = false;
              for (Object item : results) {
                  if (!((com.google.firebase.firestore.QuerySnapshot)item).isEmpty()) exists = true;
              }
              if (exists) {
                  result.done(false, "A pending friend request already exists.");
                  return;
              }
              Map<String,Object> data = new HashMap<>();
              data.put("fromUid", id); data.put("toUid", target);
              data.put("status", "pending"); data.put("createdAt", FieldValue.serverTimestamp());
              DB.collection("friendRequests").add(data)
                .addOnSuccessListener(v -> result.done(true, "Friend request sent."))
                .addOnFailureListener(e -> result.done(false, e.getLocalizedMessage()));
          })
          .addOnFailureListener(e -> result.done(false, e.getLocalizedMessage()));
    }

    public static void createClub(String name, Result result) {
        String id = uid();
        if (id == null || name == null || name.trim().isEmpty()) { result.done(false, "Enter a club name."); return; }
        Map<String,Object> data = new HashMap<>();
        data.put("name", name.trim()); data.put("ownerUid", id);
        data.put("memberUids", java.util.Collections.singletonList(id));
        data.put("createdAt", FieldValue.serverTimestamp());
        DB.collection("clubs").add(data)
          .addOnSuccessListener(v -> result.done(true, "Club created."))
          .addOnFailureListener(e -> result.done(false, e.getLocalizedMessage()));
    }


    public static void joinClub(String clubId, Result result) {
        String id = uid();
        if (id == null || clubId == null || clubId.trim().isEmpty()) {
            result.done(false, "Sign in and choose a valid club."); return;
        }
        DB.collection("clubs").document(clubId.trim())
          .update("memberUids", FieldValue.arrayUnion(id))
          .addOnSuccessListener(v -> result.done(true, "Joined club."))
          .addOnFailureListener(e -> result.done(false, e.getLocalizedMessage()));
    }


    public static void createChallenge(String name, String description, long targetSteps, Result result) {
        String id = uid();
        if (id == null || name == null || name.trim().isEmpty() || targetSteps < 1) {
            result.done(false, "Enter a challenge name and a positive step target."); return;
        }
        isAdmin((allowed, message) -> {
            if (!allowed) { result.done(false, "Only an authorized admin can create challenges."); return; }
            Map<String,Object> data = new HashMap<>();
            data.put("name", name.trim());
            data.put("description", description == null ? "" : description.trim());
            data.put("targetSteps", targetSteps);
            data.put("participantUids", new java.util.ArrayList<String>());
            data.put("createdBy", id);
            data.put("createdAt", FieldValue.serverTimestamp());
            DB.collection("challenges").add(data)
                .addOnSuccessListener(v -> result.done(true, "Challenge created."))
                .addOnFailureListener(e -> result.done(false, e.getLocalizedMessage()));
        });
    }

    public static void joinChallenge(String challengeId, Result result) {
        String id = uid();
        if (id == null || challengeId == null || challengeId.trim().isEmpty()) {
            result.done(false, "Enter a challenge ID."); return;
        }
        DB.collection("challenges").document(challengeId.trim())
          .update("participantUids", FieldValue.arrayUnion(id))
          .addOnSuccessListener(v -> result.done(true, "Joined challenge."))
          .addOnFailureListener(e -> result.done(false, e.getLocalizedMessage()));
    }

    public static void isAdmin(Result result) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { result.done(false, "Sign in first."); return; }
        user.getIdToken(true).addOnSuccessListener(token -> {
            Object admin = token.getClaims().get("admin");
            result.done(Boolean.TRUE.equals(admin), Boolean.TRUE.equals(admin) ? "Admin access granted." : "Admin access denied.");
        }).addOnFailureListener(e -> result.done(false, e.getLocalizedMessage()));
    }

    public static void setUserBlocked(String targetUid, boolean blocked, Result result) {
        isAdmin((allowed, message) -> {
            if (!allowed) { result.done(false, message); return; }
            DB.collection("users").document(targetUid).set(
                java.util.Collections.singletonMap("blocked", blocked),
                com.google.firebase.firestore.SetOptions.merge())
              .addOnSuccessListener(v -> result.done(true, blocked ? "User blocked." : "User unblocked."))
              .addOnFailureListener(e -> result.done(false, e.getLocalizedMessage()));
        });
    }
}

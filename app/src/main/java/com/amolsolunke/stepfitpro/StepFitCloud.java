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

    private static boolean isCurrentUser(String expectedUid) {
        return expectedUid != null && expectedUid.equals(uid());
    }

    public interface ProfileResult { void done(boolean ok, String name, String age, String gender); }

    /** Load the signed-in user's cloud profile, creating it from local values only when absent. */
    public static void loadOrCreateProfile(String localName, String localAge, String localGender, ProfileResult result) {
        String id = uid();
        if (id == null) { result.done(false, localName, localAge, localGender); return; }
        // Capture account-owned values before the asynchronous read. If the
        // device account changes while the request is pending, never create
        // the new account's profile using the previous account's local data.
        FirebaseUser requestedUser = FirebaseAuth.getInstance().getCurrentUser();
        final String requestedEmail = requestedUser == null ? null : requestedUser.getEmail();
        DB.collection("users").document(id).get()
          .addOnSuccessListener(doc -> {
              // Do not backfill or create a profile after the device account changes.
              if (!isCurrentUser(id)) {
                  result.done(false, localName, localAge, localGender);
                  return;
              }
              if (doc.exists()) {
                  String name = doc.getString("name");
                  String age = doc.getString("age");
                  String gender = doc.getString("gender");

                  // Backfill only missing fields; never replace cloud values
                  // with potentially stale values from this device.
                  Map<String,Object> missingFields = new HashMap<>();
                  if ((name == null || name.trim().isEmpty()) && localName != null && !localName.trim().isEmpty()) {
                      missingFields.put("name", localName.trim());
                      name = localName.trim();
                  }
                  if ((age == null || age.trim().isEmpty()) && localAge != null && !localAge.trim().isEmpty()) {
                      missingFields.put("age", localAge.trim());
                      age = localAge.trim();
                  }
                  if ((gender == null || gender.trim().isEmpty()) && localGender != null && !localGender.trim().isEmpty()) {
                      missingFields.put("gender", localGender.trim());
                      gender = localGender.trim();
                  }
                  String cloudEmail = doc.getString("email");
                  if ((cloudEmail == null || cloudEmail.trim().isEmpty())
                          && requestedEmail != null && !requestedEmail.trim().isEmpty()) {
                      missingFields.put("email", requestedEmail);
                  }
                  final String resolvedName = name == null ? (localName == null ? "" : localName) : name;
                  final String resolvedAge = age == null ? (localAge == null ? "" : localAge) : age;
                  final String resolvedGender = gender == null ? (localGender == null ? "" : localGender) : gender;
                  if (!missingFields.isEmpty()) {
                      missingFields.put("updatedAt", FieldValue.serverTimestamp());
                      if (!isCurrentUser(id)) {
                          result.done(false, localName, localAge, localGender);
                          return;
                      }
                      DB.collection("users").document(id)
                          .set(missingFields, com.google.firebase.firestore.SetOptions.merge())
                          .addOnSuccessListener(v -> result.done(isCurrentUser(id), resolvedName, resolvedAge, resolvedGender))
                          .addOnFailureListener(e -> result.done(false, localName, localAge, localGender));
                  } else {
                      result.done(isCurrentUser(id), resolvedName, resolvedAge, resolvedGender);
                  }
              } else {
                  if (!isCurrentUser(id)) {
                      result.done(false, localName, localAge, localGender);
                      return;
                  }
                  // Write the profile to the UID captured before the read,
                  // rather than resolving FirebaseAuth's current UID again.
                  Map<String,Object> data = new HashMap<>();
                  data.put("uid", id);
                  data.put("email", requestedEmail);
                  data.put("name", localName == null ? "" : localName);
                  data.put("age", localAge == null ? "" : localAge);
                  data.put("gender", localGender == null ? "" : localGender);
                  data.put("updatedAt", FieldValue.serverTimestamp());
                  DB.collection("users").document(id)
                      .set(data, com.google.firebase.firestore.SetOptions.merge())
                      .addOnSuccessListener(v -> result.done(isCurrentUser(id),
                          localName == null ? "" : localName,
                          localAge == null ? "" : localAge,
                          localGender == null ? "" : localGender))
                      .addOnFailureListener(e -> result.done(false,
                          localName, localAge, localGender));
              }
          })
          .addOnFailureListener(e -> result.done(false, localName, localAge, localGender));
    }

    public static void syncProfile(String name, String age, String gender, Result result) {
        FirebaseUser requestedUser = FirebaseAuth.getInstance().getCurrentUser();
        final String id = requestedUser == null ? null : requestedUser.getUid();
        final String email = requestedUser == null ? null : requestedUser.getEmail();
        if (id == null) {
            if (result != null) result.done(false, "Sign in to sync your profile.");
            return;
        }
        Map<String,Object> data = new HashMap<>();
        data.put("uid", id);
        if (email != null) data.put("email", email);
        data.put("name", name == null ? "" : name);
        data.put("age", age == null ? "" : age);
        data.put("gender", gender == null ? "" : gender);
        data.put("updatedAt", FieldValue.serverTimestamp());
        DB.collection("users").document(id).set(data, com.google.firebase.firestore.SetOptions.merge())
          .addOnSuccessListener(v -> {
              if (result != null) result.done(isCurrentUser(id),
                  isCurrentUser(id) ? "Profile synced." : "Account changed. Sync result was not applied to the current account.");
          })
          .addOnFailureListener(e -> { if (result != null) result.done(false, e.getLocalizedMessage()); });
    }

    public interface PhotoUrlResult { void done(String photoUrl); }

    /** Read the signed-in user's saved profile-photo URL from their own Firestore profile. */
    public static void loadProfilePhotoUrl(PhotoUrlResult result) {
        String id = uid();
        if (id == null) { result.done(null); return; }
        DB.collection("users").document(id).get()
          .addOnSuccessListener(doc -> result.done(isCurrentUser(id) ? doc.getString("photoUrl") : null))
          .addOnFailureListener(e -> result.done(null));
    }

    public static void uploadProfilePhoto(Uri photo, Result result) {
        final String id = uid();
        if (id == null || photo == null) { result.done(false, "Sign in and choose a photo."); return; }
        // Refuse to begin an upload unless the captured account is still active.
        if (!isCurrentUser(id)) {
            result.done(false, "Account changed. Please choose the photo again.");
            return;
        }
        StorageReference ref = STORAGE.getReference().child("profilePhotos/" + id + "/avatar");
        ref.putFile(photo).continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return ref.getDownloadUrl();
        }).addOnSuccessListener(url -> {
            // The upload path is UID-scoped; never write its URL into another account.
            if (!isCurrentUser(id)) {
                result.done(false, "Account changed. The photo upload was not linked to the current profile.");
                return;
            }
            DB.collection("users").document(id).set(
                java.util.Collections.singletonMap("photoUrl", url.toString()),
                com.google.firebase.firestore.SetOptions.merge())
              .addOnSuccessListener(v -> result.done(isCurrentUser(id),
                  isCurrentUser(id) ? "Profile photo synced." : "Account changed. Please refresh the profile."))
              .addOnFailureListener(e -> result.done(false, e.getLocalizedMessage()));
        }).addOnFailureListener(e -> result.done(false, e.getLocalizedMessage()));
    }


    /**
     * Optional, user-consented route backup. The caller must check the local
     * opt-in preference before invoking this method. Precise GPS data is private
     * under the matching Firestore rules and is never shared with friends.
     */
    public static void savePrivateRoute(String routeId, long savedAt, double distanceKm,
                                        long durationSeconds, String points, Result result) {
        final String id = uid();
        if (id == null || routeId == null || !routeId.matches("[A-Za-z0-9_-]{1,64}")
                || savedAt <= 0 || points == null || points.length() > 180000
                || !Double.isFinite(distanceKm) || distanceKm < 0
                || durationSeconds < 0) {
            if (result != null) result.done(false, "Invalid route data or sign-in required.");
            return;
        }
        int count = points.isEmpty() ? 0 : points.split(";", -1).length;
        if (count < 2 || count > 3000) {
            if (result != null) result.done(false, "Route must contain 2 to 3000 points.");
            return;
        }
        Map<String,Object> data = new HashMap<>();
        data.put("routeId", routeId);
        data.put("savedAt", savedAt);
        data.put("distanceKm", distanceKm);
        data.put("durationSeconds", durationSeconds);
        data.put("points", points);
        data.put("updatedAt", FieldValue.serverTimestamp());
        DB.collection("users").document(id).collection("routes").document(routeId).set(data)
          .addOnSuccessListener(v -> {
              if (result != null) result.done(id.equals(uid()), id.equals(uid())
                      ? "Private route backup saved." : "Account changed. Backup result was not applied to the current account.");
          })
          .addOnFailureListener(e -> {
              if (result != null) result.done(false, e.getLocalizedMessage());
          });
    }

    /** Delete one private cloud route belonging to the currently signed-in user. */
    public static void deletePrivateRoute(String routeId, Result result) {
        final String id = uid();
        if (id == null || routeId == null || !routeId.matches("[A-Za-z0-9_-]{1,64}")) {
            if (result != null) result.done(false, "Sign in and choose a valid route.");
            return;
        }
        DB.collection("users").document(id).collection("routes").document(routeId).delete()
          .addOnSuccessListener(v -> {
              if (result != null) result.done(id.equals(uid()), id.equals(uid())
                      ? "Cloud route deleted." : "Account changed. Please refresh cloud history.");
          })
          .addOnFailureListener(e -> { if (result != null) result.done(false, e.getLocalizedMessage()); });
    }

    public interface PrivateRoutesResult { void done(boolean ok, java.util.List<DocumentSnapshot> routes, String message); }

    /** List only the signed-in user's private GPS routes, newest first. */
    public static void loadPrivateRoutes(PrivateRoutesResult result) {
        final String id = uid();
        if (id == null) {
            if (result != null) result.done(false, new java.util.ArrayList<>(), "Sign in to view cloud routes.");
            return;
        }
        DB.collection("users").document(id).collection("routes")
          .orderBy("savedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
          .limit(50).get()
          .addOnSuccessListener(snapshot -> {
              if (!id.equals(uid())) {
                  if (result != null) result.done(false, new java.util.ArrayList<>(), "Account changed. Please try again.");
                  return;
              }
              if (result != null) result.done(true, snapshot.getDocuments(), "Cloud routes loaded.");
          })
          .addOnFailureListener(e -> {
              if (result != null) result.done(false, new java.util.ArrayList<>(),
                      id.equals(uid()) ? e.getLocalizedMessage() : "Account changed. Please try again.");
          });
    }

    public static void saveDailyStats(long steps, double distanceKm, double calories, Result result) {
        final String id = uid();
        if (id == null) {
            if (result != null) result.done(false, "Sign in to sync activity.");
            return;
        }
        final String day = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(new java.util.Date());
        Map<String,Object> data = new HashMap<>();
        data.put("date", day); data.put("steps", steps);
        data.put("distanceKm", distanceKm); data.put("calories", calories);
        data.put("updatedAt", FieldValue.serverTimestamp());
        DB.collection("users").document(id).collection("dailyStats").document(day).set(data)
          .addOnSuccessListener(v -> {
              if (result != null) result.done(true, "Activity synced to the account that started the sync.");
          })
          .addOnFailureListener(e -> {
              if (result != null) result.done(false, e.getLocalizedMessage());
          });
    }

    public static void sendFriendRequest(String targetUid, Result result) {
        String id = uid();
        if (id == null || targetUid == null || targetUid.trim().isEmpty() || id.equals(targetUid)) {
            result.done(false, "Enter another user's valid ID."); return;
        }
        String target = targetUid.trim();
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
        final String requestedUid = user.getUid();
        user.getIdToken(true).addOnSuccessListener(token -> {
            if (!requestedUid.equals(uid())) {
                result.done(false, "Account changed. Please try again.");
                return;
            }
            Object admin = token.getClaims().get("admin");
            result.done(Boolean.TRUE.equals(admin), Boolean.TRUE.equals(admin) ? "Admin access granted." : "Admin access denied.");
        }).addOnFailureListener(e -> result.done(false, e.getLocalizedMessage()));
    }

    /** Block/unblock accounts only after fresh admin verification; an admin cannot block itself. */
    public static void setUserBlocked(String targetUid, boolean blocked, Result result) {
        final String adminUid = uid();
        if (adminUid == null) {
            if (result != null) result.done(false, "Sign in as an authorized admin.");
            return;
        }
        if (targetUid == null || targetUid.trim().isEmpty()) {
            if (result != null) result.done(false, "Enter a valid user ID.");
            return;
        }
        final String target = targetUid.trim();
        if (blocked && adminUid.equals(target)) {
            if (result != null) result.done(false, "Your administrator account cannot be blocked.");
            return;
        }
        isAdmin((allowed, message) -> {
            if (!allowed) {
                if (result != null) result.done(false, message);
                return;
            }
            if (!adminUid.equals(uid())) {
                if (result != null) result.done(false, "Account changed. Please try again.");
                return;
            }
            DB.collection("users").document(target).set(
                java.util.Collections.singletonMap("blocked", blocked),
                com.google.firebase.firestore.SetOptions.merge())
              .addOnSuccessListener(v -> {
                  if (result != null) result.done(true, blocked ? "User blocked." : "User unblocked.");
              })
              .addOnFailureListener(e -> {
                  if (result != null) result.done(false, e.getLocalizedMessage());
              });
        });
    }
}

package com.amolsolunke.stepfitpro;

import android.app.*;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import android.graphics.Color;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class CommunityActivity extends Activity {
    private LinearLayout box;
    private void msg(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
    private void field(String title, java.util.function.Consumer<String> action) {
        EditText input = new EditText(this); input.setSingleLine(true); input.setHint(title);
        new AlertDialog.Builder(this).setTitle(title).setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Continue", (d,w) -> action.accept(input.getText().toString().trim())).show();
    }
    private Button add(String title, Runnable action) {
        Button b = new Button(this); b.setText(title); b.setOnClickListener(v -> action.run());
        box.addView(b, new LinearLayout.LayoutParams(-1, -2)); return b;
    }
    private void result(boolean ok, String text) { msg(text); }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            new AlertDialog.Builder(this)
                .setTitle("Sign in required")
                .setMessage("Community features need a free account and internet connection. Your offline step counter remains available without signing in.")
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setOnCancelListener(dialog -> finish())
                .show();
            return;
        }
        box = new LinearLayout(this); box.setOrientation(1); box.setPadding(24,24,24,24);
        box.setBackgroundColor(Color.rgb(10,18,38));
        TextView title = new TextView(this); title.setText("StepFit PRO • Community");
        title.setTextColor(Color.WHITE); title.setTextSize(24); title.setGravity(Gravity.CENTER);
        box.addView(title);
        add("Send Friend Request", () -> field("Friend's User ID", id -> StepFitCloud.sendFriendRequest(id, this::result)));
        add("Incoming Friend Requests", this::showFriendRequests);
        add("My Friends", this::showFriends);
        add("Browse Clubs", this::showClubs);
        add("Browse Challenges", this::showChallenges);
        add("Create a Club", () -> field("Club name", name -> StepFitCloud.createClub(name, this::result)));
        add("Join a Challenge", () -> field("Challenge ID", id -> StepFitCloud.joinChallenge(id, this::result)));
        add("My User ID", () -> {
            String id = StepFitCloud.uid();
            new AlertDialog.Builder(this).setTitle("Your User ID").setMessage(id == null ? "Not signed in" : id)
                .setPositiveButton("OK", null).show();
        });
        add("Admin Panel", this::openAdmin);
        add("Back", this::finish);
        setContentView(box);
    }

    private void showFriendRequests() {
        String uid = StepFitCloud.uid();
        if (uid == null) { msg("Sign in first."); return; }
        FirebaseFirestore.getInstance().collection("friendRequests")
            .whereEqualTo("toUid", uid).whereEqualTo("status", "pending")
            .get().addOnSuccessListener(snapshot -> {
                if (snapshot.isEmpty()) { msg("No pending friend requests."); return; }
                java.util.List<com.google.firebase.firestore.DocumentSnapshot> docs = snapshot.getDocuments();
                String[] labels = new String[docs.size()];
                for (int i = 0; i < docs.size(); i++) {
                    String from = docs.get(i).getString("fromUid");
                    labels[i] = "Request from " + (from == null ? "unknown" : from);
                }
                new AlertDialog.Builder(this).setTitle("Friend Requests")
                    .setItems(labels, (dialog, which) -> {
                        String id = docs.get(which).getId();
                        new AlertDialog.Builder(this).setTitle("Friend request")
                            .setItems(new String[]{"Accept", "Reject"}, (d, choice) -> {
                                String status = choice == 0 ? "accepted" : "rejected";
                                FirebaseFirestore.getInstance().collection("friendRequests").document(id)
                                    .update("status", status)
                                    .addOnSuccessListener(v -> msg("Request " + status + "."))
                                    .addOnFailureListener(e -> msg("Could not update request: " + e.getLocalizedMessage()));
                            }).setNegativeButton("Cancel", null).show();
                    }).setPositiveButton("Close", null).show();
            }).addOnFailureListener(e -> msg("Could not load requests: " + e.getLocalizedMessage()));
    }

    private void showFriends() {
        String uid = StepFitCloud.uid();
        if (uid == null) { msg("Sign in first."); return; }
        com.google.firebase.firestore.CollectionReference requests =
            FirebaseFirestore.getInstance().collection("friendRequests");
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> sent =
            requests.whereEqualTo("fromUid", uid).whereEqualTo("status", "accepted").get();
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> received =
            requests.whereEqualTo("toUid", uid).whereEqualTo("status", "accepted").get();

        com.google.android.gms.tasks.Tasks.whenAllSuccess(sent, received)
            .addOnSuccessListener(results -> {
                java.util.Set<String> friends = new java.util.LinkedHashSet<>();
                for (Object item : results) {
                    com.google.firebase.firestore.QuerySnapshot snapshot =
                        (com.google.firebase.firestore.QuerySnapshot)item;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        String from = doc.getString("fromUid");
                        String to = doc.getString("toUid");
                        if (uid.equals(from) && to != null) friends.add(to);
                        else if (uid.equals(to) && from != null) friends.add(from);
                    }
                }
                if (friends.isEmpty()) { msg("No accepted friends yet."); return; }
                String[] labels = friends.toArray(new String[0]);
                new AlertDialog.Builder(this).setTitle("My Friends (" + labels.length + ")")
                    .setItems(labels, (dialog, which) ->
                        new AlertDialog.Builder(this).setTitle("Friend User ID")
                            .setMessage(labels[which]).setPositiveButton("OK", null).show())
                    .setPositiveButton("Close", null).show();
            }).addOnFailureListener(e -> msg("Could not load friends: " + e.getLocalizedMessage()));
    }

    private void showClubs() {
        FirebaseFirestore.getInstance().collection("clubs").limit(50).get()
            .addOnSuccessListener(snapshot -> {
                if (snapshot.isEmpty()) { msg("No clubs are available yet."); return; }
                java.util.List<String> lines = new java.util.ArrayList<>();
                java.util.List<String> ids = new java.util.ArrayList<>();
                for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                    String name = doc.getString("name");
                    lines.add((name == null ? "Unnamed club" : name) + "  •  " + doc.getId());
                    ids.add(doc.getId());
                }
                new AlertDialog.Builder(this).setTitle("Available Clubs")
                    .setItems(lines.toArray(new String[0]), (dialog, which) ->
                        StepFitCloud.joinClub(ids.get(which), this::result))
                    .setNegativeButton("Close", null).show();
            }).addOnFailureListener(e -> msg("Could not load clubs: " + e.getLocalizedMessage()));
    }

    private void showChallenges() {
        FirebaseFirestore.getInstance().collection("challenges").limit(50).get()
            .addOnSuccessListener(snapshot -> {
                if (snapshot.isEmpty()) { msg("No challenges are available yet."); return; }
                java.util.List<String> lines = new java.util.ArrayList<>();
                java.util.List<String> ids = new java.util.ArrayList<>();
                for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                    String name = doc.getString("name");
                    lines.add((name == null ? "Challenge" : name) + "  •  " + doc.getId());
                    ids.add(doc.getId());
                }
                new AlertDialog.Builder(this).setTitle("Challenges")
                    .setItems(lines.toArray(new String[0]), (dialog, which) ->
                        showChallengeDetails(ids.get(which)))
                    .setNegativeButton("Close", null).show();
            }).addOnFailureListener(e -> msg("Could not load challenges: " + e.getLocalizedMessage()));
    }



    private void showChallengeDetails(String challengeId) {
        FirebaseFirestore.getInstance().collection("challenges").document(challengeId)
            .get().addOnSuccessListener(doc -> {
                if (!doc.exists()) { msg("This challenge is no longer available."); return; }
                String name = doc.getString("name");
                String description = doc.getString("description");
                Long target = doc.getLong("targetSteps");
                java.util.List<String> participants = (java.util.List<String>) doc.get("participantUids");
                if (participants == null) participants = new java.util.ArrayList<>();
                String uid = StepFitCloud.uid();
                boolean joined = uid != null && participants.contains(uid);
                long goal = target == null ? 0L : Math.max(0L, target);
                long todaySteps = getLocalTodaySteps();
                int percent = goal <= 0 ? 0 : (int)Math.min(100L, todaySteps * 100L / goal);
                String message = (description == null || description.trim().isEmpty() ? "" : description + "\n\n")
                    + "Target: " + goal + " steps\n"
                    + "Participants: " + participants.size() + "\n"
                    + "Your status: " + (joined ? "Joined" : "Not joined") + "\n"
                    + "Your steps today: " + todaySteps + "\n"
                    + "Today's progress against target: " + percent + "%\n\n"
                    + "Score submission is temporarily disabled because device-reported steps cannot be securely verified. A trusted, no-cost server validation method is required before competitive scores can be accepted.";
                AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                    .setTitle(name == null ? "Challenge" : name)
                    .setMessage(message)
                    .setPositiveButton(joined ? "Close" : "Join challenge", (d, w) -> {
                        if (!joined) StepFitCloud.joinChallenge(challengeId, (ok, messageText) -> runOnUiThread(() -> {
                            msg(messageText);
                            if (ok) showChallengeDetails(challengeId);
                        }));
                    })
                    .setNeutralButton("Leaderboard", (d, w) -> showChallengeLeaderboard(challengeId))
                    .setNegativeButton("Cancel", null);
                dialog.show();
            }).addOnFailureListener(e -> msg("Could not load challenge: " + e.getLocalizedMessage()));
    }

    private long getLocalTodaySteps() {
        Object value = getSharedPreferences("StepFitData", MODE_PRIVATE).getAll().get("today_steps");
        return value instanceof Number ? Math.max(0L, ((Number)value).longValue()) : 0L;
    }

    private String todayKey() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(new java.util.Date());
    }

    // Scores are submitted by each participant's device; they are not server-verified.
    private void submitChallengeProgress(String challengeId) {
        String uid = StepFitCloud.uid();
        if (uid == null) { msg("Sign in first."); return; }
        long steps = getLocalTodaySteps();
        String day = todayKey();
        java.util.Map<String,Object> data = new java.util.HashMap<>();
        data.put("uid", uid);
        data.put("day", day);
        data.put("steps", steps);
        data.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        FirebaseFirestore.getInstance().collection("challenges").document(challengeId)
            .collection("dailyProgress").document(uid + "_" + day).set(data)
            .addOnSuccessListener(v -> msg("Today's device step count submitted: " + steps))
            .addOnFailureListener(e -> msg("Could not submit progress: " + e.getLocalizedMessage()));
    }

    private void showChallengeLeaderboard(String challengeId) {
        String day = todayKey();
        FirebaseFirestore.getInstance().collection("challenges").document(challengeId)
            .collection("dailyProgress").whereEqualTo("day", day).limit(100).get()
            .addOnSuccessListener(snapshot -> {
                if (snapshot.isEmpty()) { msg("No submitted scores for today yet."); return; }
                java.util.List<com.google.firebase.firestore.DocumentSnapshot> docs =
                    new java.util.ArrayList<>(snapshot.getDocuments());
                java.util.Collections.sort(docs, (a, b) ->
                    Long.compare(b.getLong("steps") == null ? 0L : b.getLong("steps"),
                                 a.getLong("steps") == null ? 0L : a.getLong("steps")));
                StringBuilder board = new StringBuilder();
                int rank = 1;
                for (com.google.firebase.firestore.DocumentSnapshot doc : docs.subList(0, Math.min(20, docs.size()))) {
                    String uid = doc.getString("uid");
                    Long steps = doc.getLong("steps");
                    String shortId = uid == null ? "unknown" :
                        (uid.length() > 10 ? uid.substring(0, 10) + "…" : uid);
                    board.append(rank++).append(". ").append(shortId)
                        .append(" — ").append(steps == null ? 0 : steps).append(" steps\n");
                }
                new AlertDialog.Builder(this).setTitle("Today's leaderboard")
                    .setMessage(board.toString() + "\nScores are self-reported from devices and are not verified.")
                    .setPositiveButton("Close", null).show();
            })
            .addOnFailureListener(e -> msg("Could not load leaderboard: " + e.getLocalizedMessage()));
    }

    private void createChallengeDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(32, 12, 32, 0);
        EditText name = new EditText(this); name.setHint("Challenge name");
        EditText description = new EditText(this); description.setHint("Description");
        EditText target = new EditText(this); target.setHint("Target steps");
        target.setInputType(2);
        form.addView(name); form.addView(description); form.addView(target);
        new AlertDialog.Builder(this).setTitle("Create challenge").setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Create", (dialog, which) -> {
                long steps = 0;
                try { steps = Long.parseLong(target.getText().toString().trim()); }
                catch (NumberFormatException ignored) {}
                StepFitCloud.createChallenge(name.getText().toString(),
                    description.getText().toString(), steps, this::result);
            }).show();
    }

    private void showAdminUsers() {
        showAdminUsersPage(new java.util.ArrayList<>(), 0);
    }

    private void showAdminUsersPage(
            java.util.List<com.google.firebase.firestore.DocumentSnapshot> cursors, int pageIndex) {
        com.google.firebase.firestore.CollectionReference users =
            FirebaseFirestore.getInstance().collection("users");
        com.google.firebase.firestore.Query query = users
            .orderBy(com.google.firebase.firestore.FieldPath.documentId()).limit(25);
        if (pageIndex > 0 && pageIndex <= cursors.size())
            query = query.startAfter(cursors.get(pageIndex - 1));

        query.get().addOnSuccessListener(snapshot -> {
            if (snapshot.isEmpty()) {
                if (pageIndex == 0) msg("No user profiles found.");
                else msg("No more users on this page.");
                return;
            }
            java.util.List<com.google.firebase.firestore.DocumentSnapshot> docs = snapshot.getDocuments();
            String[] labels = new String[docs.size()];
            for (int i = 0; i < docs.size(); i++) {
                com.google.firebase.firestore.DocumentSnapshot doc = docs.get(i);
                String name = doc.getString("name");
                String email = doc.getString("email");
                boolean blocked = Boolean.TRUE.equals(doc.getBoolean("blocked"));
                labels[i] = (name == null || name.isEmpty() ? "Unnamed user" : name)
                    + " • " + (email == null ? doc.getId() : email)
                    + (blocked ? " • SUSPENDED" : "");
            }
            java.util.List<com.google.firebase.firestore.DocumentSnapshot> nextCursors =
                new java.util.ArrayList<>(cursors);
            AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Users • Page " + (pageIndex + 1))
                .setItems(labels, (d, which) -> {
                    String uid = docs.get(which).getId();
                    boolean blocked = Boolean.TRUE.equals(docs.get(which).getBoolean("blocked"));
                    new AlertDialog.Builder(this).setTitle("Manage account")
                        .setMessage(labels[which])
                        .setPositiveButton(blocked ? "Unblock" : "Block", (x, w) ->
                            StepFitCloud.setUserBlocked(uid, !blocked, this::result))
                        .setNegativeButton("Cancel", null).show();
                })
                .setNegativeButton(pageIndex > 0 ? "Previous" : "Close", (d, w) -> {
                    if (pageIndex > 0) showAdminUsersPage(nextCursors, pageIndex - 1);
                })
                .setPositiveButton("Next", (d, w) -> {
                    if (docs.size() == 25) {
                        if (nextCursors.size() == pageIndex) nextCursors.add(docs.get(docs.size() - 1));
                        showAdminUsersPage(nextCursors, pageIndex + 1);
                    } else msg("You have reached the end of the user list.");
                })
                .setNeutralButton("Done", null).create();
            dialog.setOnShowListener(ignored -> {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(pageIndex > 0);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(docs.size() == 25);
            });
            dialog.show();
        }).addOnFailureListener(e -> msg("Could not load users: " + e.getLocalizedMessage()));
    }

    private void openAdmin() {
        StepFitCloud.isAdmin((allowed, message) -> runOnUiThread(() -> {
            if (!allowed) { msg(message); return; }
            new AlertDialog.Builder(this).setTitle("Protected Admin Panel")
              .setItems(new String[]{"Manage user list","Block user by UID","Unblock user by UID","Create challenge"}, (d, which) -> {
                  if (which == 0) showAdminUsers();
                  else if (which == 3) createChallengeDialog();
                  else field("Target user's UID", id ->
                      StepFitCloud.setUserBlocked(id, which == 1, this::result));
              })
              .setNegativeButton("Close", null).show();
        }));
    }
}
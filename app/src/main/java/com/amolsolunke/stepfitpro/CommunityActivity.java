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
        FirebaseFirestore.getInstance().collection("friendRequests")
            .whereEqualTo("status", "accepted").get()
            .addOnSuccessListener(snapshot -> {
                java.util.Set<String> friends = new java.util.LinkedHashSet<>();
                for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                    String from = doc.getString("fromUid");
                    String to = doc.getString("toUid");
                    if (uid.equals(from) && to != null) friends.add(to);
                    else if (uid.equals(to) && from != null) friends.add(from);
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
                        StepFitCloud.joinChallenge(ids.get(which), this::result))
                    .setNegativeButton("Close", null).show();
            }).addOnFailureListener(e -> msg("Could not load challenges: " + e.getLocalizedMessage()));
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
        FirebaseFirestore.getInstance().collection("users").limit(100).get()
            .addOnSuccessListener(snapshot -> {
                if (snapshot.isEmpty()) { msg("No user profiles found."); return; }
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
                new AlertDialog.Builder(this).setTitle("Users (up to 100)")
                    .setItems(labels, (dialog, which) -> {
                        String uid = docs.get(which).getId();
                        boolean blocked = Boolean.TRUE.equals(docs.get(which).getBoolean("blocked"));
                        new AlertDialog.Builder(this).setTitle("Manage account")
                            .setMessage(labels[which])
                            .setPositiveButton(blocked ? "Unblock" : "Block", (d, w) ->
                                StepFitCloud.setUserBlocked(uid, !blocked, this::result))
                            .setNegativeButton("Cancel", null).show();
                    }).setPositiveButton("Close", null).show();
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
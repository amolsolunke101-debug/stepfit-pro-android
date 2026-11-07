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
        box = new LinearLayout(this); box.setOrientation(1); box.setPadding(24,24,24,24);
        box.setBackgroundColor(Color.rgb(10,18,38));
        TextView title = new TextView(this); title.setText("StepFit PRO • Community");
        title.setTextColor(Color.WHITE); title.setTextSize(24); title.setGravity(Gravity.CENTER);
        box.addView(title);
        add("Send Friend Request", () -> field("Friend's User ID", id -> StepFitCloud.sendFriendRequest(id, this::result)));
        add("Incoming Friend Requests", this::showFriendRequests);
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
              .setItems(new String[]{"Manage user list","Block user by UID","Unblock user by UID"}, (d, which) -> {
                  if (which == 0) showAdminUsers();
                  else field("Target user's UID", id ->
                      StepFitCloud.setUserBlocked(id, which == 1, this::result));
              })
              .setNegativeButton("Close", null).show();
        }));
    }
}
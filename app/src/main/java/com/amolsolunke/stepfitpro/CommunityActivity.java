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
    private void openAdmin() {
        StepFitCloud.isAdmin((allowed, message) -> runOnUiThread(() -> {
            if (!allowed) { msg(message); return; }
            new AlertDialog.Builder(this).setTitle("Protected Admin Panel")
              .setItems(new String[]{"Block user","Unblock user"}, (d, which) ->
                field("Target user's UID", id -> StepFitCloud.setUserBlocked(id, which == 0, this::result)))
              .setNegativeButton("Close", null).show();
        }));
    }
}
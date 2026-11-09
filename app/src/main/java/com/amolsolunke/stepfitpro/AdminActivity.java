package com.amolsolunke.stepfitpro;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class AdminActivity extends Activity {
    private LinearLayout list;
    private String ownerUid;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ownerUid = FirebaseAuth.getInstance().getCurrentUser() == null ? null
            : FirebaseAuth.getInstance().getCurrentUser().getUid();
        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(24, 24, 24, 24);
        scroll.addView(list);
        setContentView(scroll);
        TextView title = new TextView(this);
        title.setText("StepFit PRO — Admin Panel");
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        list.addView(title);
        verifyAndLoad();
    }

    private boolean sameAccount() {
        return ownerUid != null && FirebaseAuth.getInstance().getCurrentUser() != null
            && ownerUid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid());
    }

    private void verifyAndLoad() {
        if (!sameAccount()) { deny(); return; }
        StepFitCloud.isAdmin((allowed, message) -> runOnUiThread(() -> {
            if (isFinishing()) return;
            if (!allowed || !sameAccount()) { deny(); return; }
            loadUsers();
        }));
    }

    private void deny() {
        Toast.makeText(this, "Admin access denied.", Toast.LENGTH_LONG).show();
        finish();
    }

    private void loadUsers() {
        if (!sameAccount()) { deny(); return; }
        FirebaseFirestore.getInstance().collection("users").get()
            .addOnSuccessListener(snapshot -> {
                if (isFinishing() || !sameAccount()) { deny(); return; }
                list.removeViews(1, Math.max(0, list.getChildCount() - 1));
                TextView count = new TextView(this);
                count.setText("Registered accounts: " + snapshot.size());
                count.setTextSize(16);
                list.addView(count);
                for (QueryDocumentSnapshot doc : snapshot) {
                    String uid = doc.getId();
                    String name = doc.getString("name");
                    String email = doc.getString("email");
                    boolean blocked = Boolean.TRUE.equals(doc.getBoolean("blocked"));
                    TextView details = new TextView(this);
                    details.setText((name == null || name.trim().isEmpty() ? "Unnamed user" : name)
                        + "\n" + (email == null ? "No email" : email)
                        + "\nUID: " + uid + "\nStatus: " + (blocked ? "Blocked" : "Active"));
                    details.setTextSize(14);
                    list.addView(details);
                    if (!uid.equals(ownerUid)) {
                        Button action = new Button(this);
                        action.setText(blocked ? "Unblock user" : "Block user");
                        action.setOnClickListener(v -> confirmBlock(uid, blocked));
                        list.addView(action);
                    } else {
                        TextView self = new TextView(this);
                        self.setText("Current admin account — protected from self-blocking");
                        list.addView(self);
                    }
                }
            })
            .addOnFailureListener(error -> {
                if (!isFinishing()) Toast.makeText(this,
                    "Could not load users: " + error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            });
    }

    private void confirmBlock(String targetUid, boolean currentlyBlocked) {
        if (!sameAccount()) { deny(); return; }
        new AlertDialog.Builder(this)
            .setTitle(currentlyBlocked ? "Unblock account?" : "Block account?")
            .setMessage(currentlyBlocked
                ? "This user will regain access to protected cloud features."
                : "This user will lose access to protected cloud features.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Confirm", (dialog, which) -> {
                if (!sameAccount() || ownerUid.equals(targetUid)) { deny(); return; }
                StepFitCloud.setUserBlocked(targetUid, !currentlyBlocked, (ok, message) ->
                    runOnUiThread(() -> {
                        if (isFinishing()) return;
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        if (ok && sameAccount()) loadUsers();
                    }));
            }).show();
    }
}
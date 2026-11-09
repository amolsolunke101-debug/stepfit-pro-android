package com.amolsolunke.stepfitpro;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.Locale;

public class AdminActivity extends Activity {
    private static final int PAGE_SIZE = 25;
    private LinearLayout rows;
    private EditText searchInput;
    private Button moreButton;
    private String ownerUid;
    private DocumentSnapshot lastVisible;
    private boolean loading;
    private boolean searching;
    private String activeSearch = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ownerUid = FirebaseAuth.getInstance().getCurrentUser() == null ? null
            : FirebaseAuth.getInstance().getCurrentUser().getUid();
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        scroll.addView(root);
        setContentView(scroll);

        TextView title = new TextView(this);
        title.setText("StepFit PRO — Admin Panel");
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setHint("Search users by name");
        root.addView(searchInput);
        Button searchButton = new Button(this);
        searchButton.setText("Search");
        searchButton.setOnClickListener(v -> startSearch());
        root.addView(searchButton);

        rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        root.addView(rows);
        moreButton = new Button(this);
        moreButton.setText("Load more users");
        moreButton.setVisibility(View.GONE);
        moreButton.setOnClickListener(v -> loadUsers(false));
        root.addView(moreButton);
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
            loadUsers(true);
        }));
    }

    private void deny() {
        Toast.makeText(this, "Admin access denied.", Toast.LENGTH_LONG).show();
        finish();
    }

    private void startSearch() {
        activeSearch = searchInput.getText().toString().trim();
        searching = !activeSearch.isEmpty();
        lastVisible = null;
        rows.removeAllViews();
        moreButton.setVisibility(View.GONE);
        loadUsers(true);
    }

    private void loadUsers(boolean reset) {
        if (!sameAccount()) { deny(); return; }
        if (loading) return;
        if (reset) {
            lastVisible = null;
            rows.removeAllViews();
        }
        loading = true;
        moreButton.setEnabled(false);

        Query query;
        if (searching) {
            String prefix = activeSearch.toLowerCase(Locale.ROOT);
            query = FirebaseFirestore.getInstance().collection("users")
                .orderBy("name")
                .whereGreaterThanOrEqualTo("name", prefix)
                .whereLessThanOrEqualTo("name", prefix + "\uf8ff")
                .limit(PAGE_SIZE);
        } else {
            query = FirebaseFirestore.getInstance().collection("users")
                .orderBy(com.google.firebase.firestore.FieldPath.documentId())
                .limit(PAGE_SIZE);
            if (lastVisible != null) query = query.startAfter(lastVisible);
        }

        query.get().addOnSuccessListener(snapshot -> {
            loading = false;
            if (isFinishing() || !sameAccount()) { deny(); return; }
            if (!snapshot.isEmpty()) lastVisible = snapshot.getDocuments().get(snapshot.size() - 1);
            boolean hasMore = !searching && snapshot.size() == PAGE_SIZE;
            for (DocumentSnapshot doc : snapshot.getDocuments()) addUserRow(doc);
            moreButton.setVisibility(hasMore ? View.VISIBLE : View.GONE);
            moreButton.setEnabled(true);
            if (snapshot.isEmpty() && reset) {
                TextView empty = new TextView(this);
                empty.setText(searching ? "No matching users found." : "No registered users found.");
                rows.addView(empty);
            }
        }).addOnFailureListener(error -> {
            loading = false;
            moreButton.setEnabled(true);
            if (!isFinishing()) Toast.makeText(this,
                "Could not load users: " + error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void addUserRow(DocumentSnapshot doc) {
        String uid = doc.getId();
        String name = doc.getString("name");
        String email = doc.getString("email");
        boolean blocked = Boolean.TRUE.equals(doc.getBoolean("blocked"));
        TextView details = new TextView(this);
        details.setPadding(0, 18, 0, 6);
        details.setText((name == null || name.trim().isEmpty() ? "Unnamed user" : name)
            + "\n" + (email == null ? "No email" : email)
            + "\nUID: " + uid + "\nStatus: " + (blocked ? "Blocked" : "Active"));
        details.setTextSize(14);
        rows.addView(details);
        if (!uid.equals(ownerUid)) {
            Button action = new Button(this);
            action.setText(blocked ? "Unblock user" : "Block user");
            action.setOnClickListener(v -> confirmBlock(uid, blocked));
            rows.addView(action);
        } else {
            TextView self = new TextView(this);
            self.setText("Current admin account — protected from self-blocking");
            rows.addView(self);
        }
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
                        if (ok && sameAccount()) loadUsers(true);
                    }));
            }).show();
    }
}
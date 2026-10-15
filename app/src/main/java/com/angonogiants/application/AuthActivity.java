
package com.angonogiants.application;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.util.Locale;

public class AuthActivity extends AppCompatActivity {

    private static final String DATABASE_URL =
            "https://angonogiants-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private FirebaseAuth auth;
    private DatabaseReference database;

    private EditText usernameInput;
    private EditText emailInput;
    private EditText passwordInput;

    private Button loginButton;
    private Button registerButton;
    private Button resetButton;
    private ProgressBar progressBar;

    private boolean busy = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_auth);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase
                .getInstance(DATABASE_URL)
                .getReference();

        initializeViews();
        setupButtons();

        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            loadExistingProfile(user);
        }
    }

    private void initializeViews() {
        usernameInput = findViewById(R.id.authUsername);
        emailInput = findViewById(R.id.authEmail);
        passwordInput = findViewById(R.id.authPassword);

        loginButton = findViewById(R.id.authLoginButton);
        registerButton =
                findViewById(R.id.authRegisterButton);

        resetButton = findViewById(R.id.authResetButton);
        progressBar = findViewById(R.id.authProgress);
    }

    private void setupButtons() {
        loginButton.setOnClickListener(v -> login());
        registerButton.setOnClickListener(v -> register());
        resetButton.setOnClickListener(
                v -> resetPassword()
        );
    }

    private void setBusy(boolean value) {
        busy = value;

        loginButton.setEnabled(!value);
        registerButton.setEnabled(!value);
        resetButton.setEnabled(!value);

        progressBar.setVisibility(
                value ? View.VISIBLE : View.GONE
        );
    }

    private void showMessage(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    // -----------------------------------------
    // Login
    // -----------------------------------------

    private void login() {
        if (busy) return;

        String email = emailInput.getText()
                .toString().trim();

        String password = passwordInput.getText()
                .toString();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email");
            return;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Enter your password");
            return;
        }

        setBusy(true);

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();

                    if (user == null) {
                        setBusy(false);
                        showMessage("Login failed.");
                        return;
                    }

                    loadExistingProfile(user);
                })
                .addOnFailureListener(error -> {
                    setBusy(false);
                    showMessage("Unable to sign in. Check your credentials.");
                });
    }

    // -----------------------------------------
    // Registration
    // -----------------------------------------

    private void register() {
        if (busy) return;

        String username = usernameInput.getText()
                .toString().trim();

        String email = emailInput.getText()
                .toString().trim();

        String password = passwordInput.getText()
                .toString();

        if (!username.matches("[A-Za-z0-9_]{3,20}")) {
            usernameInput.setError(
                    "Use 3–20 letters, numbers or underscores"
            );
            return;
        }

        FirebaseUser existingUser = auth.getCurrentUser();

        // Allow a signed-in user whose account setup was
        // interrupted to complete their profile.
        if (existingUser != null) {
            setBusy(true);
            reserveUsername(existingUser, username);
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email");
            return;
        }

        if (password.length() < 8) {
            passwordInput.setError(
                    "Use at least 8 characters"
            );
            return;
        }

        setBusy(true);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();

                    if (user == null) {
                        setBusy(false);
                        showMessage("Registration failed.");
                        return;
                    }

                    user.sendEmailVerification();
                    reserveUsername(user, username);
                })
                .addOnFailureListener(error -> {
                    setBusy(false);
                    showMessage(
                            "Could not create account: "
                                    + error.getLocalizedMessage()
                    );
                });
    }

    // -----------------------------------------
    // Username reservation
    // -----------------------------------------

    private void reserveUsername(
            FirebaseUser user,
            String username
    ) {
        String normalized = username
                .toLowerCase(Locale.ROOT);

        String uid = user.getUid();

        DatabaseReference nameReference =
                database.child("usernames")
                        .child(normalized);

        nameReference.runTransaction(
                new Transaction.Handler() {

                    @Override
                    public Transaction.Result doTransaction(
                            MutableData current
                    ) {
                        String owner = current.getValue(
                                String.class
                        );

                        if (owner != null
                                && !owner.equals(uid)) {
                            return Transaction.abort();
                        }

                        current.setValue(uid);
                        return Transaction.success(current);
                    }

                    @Override
                    public void onComplete(
                            DatabaseError error,
                            boolean committed,
                            DataSnapshot snapshot
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        if (error != null) {
                            setBusy(false);
                            showMessage(
                                    "Username reservation failed."
                            );
                            return;
                        }

                        if (!committed) {
                            setBusy(false);
                            usernameInput.setError(
                                    "Username already taken"
                            );
                            return;
                        }

                        createProfile(user, username, normalized);
                    }
                }
        );
    }

    private void createProfile(
            FirebaseUser user,
            String username,
            String normalized
    ) {
        PlayerProfile profile = new PlayerProfile(
                user.getUid(),
                username,
                normalized
        );

        database.child("playerProfiles")
                .child(user.getUid())
                .setValue(profile)
                .addOnSuccessListener(unused -> {
                    initializeProgress(user.getUid());
                })
                .addOnFailureListener(error -> {
                    setBusy(false);
                    showMessage(
                            "Account exists, but profile setup failed. Retry Create Account."
                    );
                });
    }

    private void initializeProgress(String uid) {
        DatabaseReference progressRef =
                database.child("playerProgress").child(uid);

        progressRef.runTransaction(
                new Transaction.Handler() {

                    @Override
                    public Transaction.Result doTransaction(
                            MutableData current
                    ) {
                        if (current.getValue() == null) {
                            current.setValue(
                                    new PlayerProgress(1, 0)
                            );
                        }

                        return Transaction.success(current);
                    }

                    @Override
                    public void onComplete(
                            DatabaseError error,
                            boolean committed,
                            DataSnapshot snapshot
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        setBusy(false);

                        if (error != null || !committed) {
                            showMessage(
                                    "Could not initialize progress."
                            );
                            return;
                        }

                        openMainMenu();
                    }
                }
        );
    }

    // -----------------------------------------
    // Returning player
    // -----------------------------------------

    private void loadExistingProfile(FirebaseUser user) {
        setBusy(true);

        database.child("playerProfiles")
                .child(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    setBusy(false);

                    if (snapshot.exists()) {
                        openMainMenu();
                    } else {
                        showMessage(
                                "Complete account setup by entering a username and tapping Create Account."
                        );
                    }
                })
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    setBusy(false);
                    showMessage(
                            "Could not load your account."
                    );
                });
    }

    // -----------------------------------------
    // Password recovery
    // -----------------------------------------

    private void resetPassword() {
        if (busy) return;

        String email = emailInput.getText()
                .toString().trim();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter your account email");
            return;
        }

        setBusy(true);

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    setBusy(false);

                    showMessage(
                            "If the address is registered, a password-reset email will be sent."
                    );
                });
    }

    private void openMainMenu() {
        if (isFinishing() || isDestroyed()) return;

        Intent intent = new Intent(
                this,
                MainActivity.class
        );

        startActivity(intent);
        finish();
    }
}

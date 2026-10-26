//app/src/main/java/com/angonogiants/application/MainActivity.java
package com.angonogiants.application;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final String DATABASE_URL =
            "https://angonogiants-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private static final long TIMEOUT_MS = 20000L;

    private FirebaseAuth auth;
    private DatabaseReference database;

    private ImageView backgroundGif;
    private ImageView leaderboardButton;
    private ImageView aboutButton;

    private EditText playerNameEditText;
    private TextView levelStatusText;

    private Button startButton;
    private Button accountButton;

    private MediaPlayer menuMusic;
    private MediaPlayer buttonSound;

    private boolean cloudReady = false;
    private boolean needsProfile = false;
    private boolean navigating = false;

    private String cloudPlayerName = "";
    private int cloudLevel = 1;

    private Dialog authDialog;
    private EditText usernameInput;
    private EditText emailInput;
    private EditText passwordInput;

    private TextView authTitle;
    private TextView authSubtitle;
    private TextView authErrorText;

    private Button submitButton;
    private Button switchButton;
    private Button resetButton;
    private Button backButton;

    private ProgressBar authProgress;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private Runnable timeoutRunnable;

    private int operationToken = 0;
    private String currentStage = "";

    private enum AuthMode {
        LOGIN,
        SIGNUP,
        COMPLETE_PROFILE
    }

    private AuthMode authMode = AuthMode.LOGIN;

    // -----------------------------------------
    // ACTIVITY SETUP
    // -----------------------------------------

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();

        database = FirebaseDatabase
                .getInstance(DATABASE_URL)
                .getReference();

        backgroundGif =
                findViewById(R.id.backgroundGif);

        leaderboardButton =
                findViewById(R.id.btnLeaderboard);

        aboutButton =
                findViewById(R.id.btnAboutUs);

        playerNameEditText =
                findViewById(R.id.playerNameEditText);

        levelStatusText =
                findViewById(R.id.levelStatusText);

        startButton =
                findViewById(R.id.btnStart);

        accountButton =
                findViewById(R.id.btnAccount);

        initializeGraphics();
        initializeAudio();
        initializeButtons();

        showGuestMode();
    }

    private void initializeGraphics() {
        Glide.with(this)
                .asGif()
                .load(R.drawable.mainmenubg01)
                .placeholder(R.drawable.mainmenubg01)
                .into(backgroundGif);

        Glide.with(this)
                .load(R.drawable.leaderboard_button)
                .into(leaderboardButton);
    }

    private void initializeAudio() {
        menuMusic = MediaPlayer.create(
                this,
                R.raw.mainmenubg
        );

        if (menuMusic != null) {
            menuMusic.setLooping(true);
        }

        buttonSound = MediaPlayer.create(
                this,
                R.raw.buttonsound
        );
    }

    private void playSound() {
        if (buttonSound == null) return;

        try {
            buttonSound.seekTo(0);
            buttonSound.start();
        } catch (IllegalStateException ignored) {
        }
    }

    // -----------------------------------------
    // MAIN MENU BUTTONS
    // -----------------------------------------

    private void initializeButtons() {
        startButton.setOnClickListener(v -> {
            playSound();
            startGame();
        });

        accountButton.setOnClickListener(v -> {
            playSound();

            FirebaseUser user =
                    auth.getCurrentUser();

            if (user == null) {
                showAuthDialog();

            } else if (cloudReady) {
                signOut();

            } else {
                showAuthDialog();
            }
        });

        leaderboardButton.setOnClickListener(v -> {
            playSound();

            startActivity(
                    new Intent(
                            this,
                            LeaderboardActivity.class
                    )
            );
        });

        aboutButton.setOnClickListener(v -> {
            playSound();

            startActivity(
                    new Intent(
                            this,
                            AboutUsActivity.class
                    )
            );
        });
    }

    // -----------------------------------------
    // GUEST MODE
    // -----------------------------------------

    private void showGuestMode() {
        cloudReady = false;
        cloudPlayerName = "";
        cloudLevel = 1;

        startButton.setEnabled(true);

        playerNameEditText.setEnabled(true);
        playerNameEditText.setFocusable(true);
        playerNameEditText.setFocusableInTouchMode(true);
        playerNameEditText.setCursorVisible(true);

        playerNameEditText.setText(
                GuestProgressStore.getGuestName(this)
        );

        int level =
                GuestProgressStore
                        .getHighestUnlockedLevel(this);

        levelStatusText.setText(
                "GUEST - LEVEL " + level
        );

        FirebaseUser user =
                auth == null ? null :
                        auth.getCurrentUser();

        if (user == null) {
            accountButton.setText(
                    "SIGN IN"
            );

        } else if (needsProfile) {
            accountButton.setText(
                    "FINISH PROFILE"
            );

        } else {
            accountButton.setText(
                    "CLOUD ACCOUNT"
            );
        }
    }

    private void startGame() {
        if (navigating) return;

        boolean guestRun = !cloudReady;

        String name;
        int level;

        if (guestRun) {
            name = playerNameEditText
                    .getText().toString().trim();

            if (name.isEmpty()) {
                name = "Guest";
            }

            GuestProgressStore.setGuestName(
                    this,
                    name
            );

            level = GuestProgressStore
                    .getHighestUnlockedLevel(this);

        } else {
            name = cloudPlayerName;
            level = cloudLevel;
        }

        navigating = true;

        Intent intent = new Intent(
                this,
                LoadingActivity.class
        );

        intent.putExtra("PLAYER_NAME", name);
        intent.putExtra("LEVEL_NUMBER", level);
        intent.putExtra("IS_GUEST", guestRun);

        startActivity(intent);
        finish();
    }

    // -----------------------------------------
    // AUTHENTICATION OPERATION TIMEOUT
    // -----------------------------------------

    private int beginOperation(String stage) {
        operationToken++;

        int token = operationToken;

        setBusy(true);
        updateStage(token, stage);

        return token;
    }

    private void updateStage(
            int token,
            String stage
    ) {
        if (token > 0 &&
                token != operationToken) {
            return;
        }

        currentStage = stage;

        Log.d(TAG, "Firebase stage: " + stage);

        if (token > 0
                && authSubtitle != null
                && authDialog != null
                && authDialog.isShowing()) {

            authSubtitle.setText(stage);
        }

        if (token <= 0) return;

        cancelTimeout();

        timeoutRunnable = () -> {
            if (token != operationToken) return;

            operationToken++;

            setBusy(false);

            showAuthError(
                    "Firebase connection timed out during: "
                            + currentStage
                            + ". Check internet, Firebase "
                            + "Authentication and Database rules. "
                            + "You can press Back and play as Guest."
            );

            FirebaseUser user =
                    auth.getCurrentUser();

            if (user != null && !cloudReady) {
                needsProfile = true;
                showGuestMode();
            }
        };

        handler.postDelayed(
                timeoutRunnable,
                TIMEOUT_MS
        );
    }

    private void cancelTimeout() {
        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
    }

    private boolean validOperation(int token) {
        return token == 0
                || token == operationToken;
    }

    private void finishOperation(int token) {
        if (!validOperation(token)) return;

        if (token > 0) {
            cancelTimeout();
            setBusy(false);
        }
    }

    private boolean validUser(
            FirebaseUser user,
            int token
    ) {
        FirebaseUser current =
                auth.getCurrentUser();

        return !isFinishing()
                && !isDestroyed()
                && validOperation(token)
                && user != null
                && current != null
                && user.getUid().equals(
                current.getUid()
        );
    }

    private String errorMessage(Exception error) {
        if (error == null) {
            return "Unknown Firebase error";
        }

        String message = error.getLocalizedMessage();

        return message == null
                ? error.toString()
                : message;
    }

    private void fail(
            int token,
            String stage,
            Exception error
    ) {
        if (!validOperation(token)) return;

        Log.e(TAG, stage, error);

        finishOperation(token);

        needsProfile = auth.getCurrentUser() != null
                && stage.contains("profile");

        showGuestMode();

        showAuthError(
                stage + ": " + errorMessage(error)
        );
    }

    // -----------------------------------------
    // FIREBASE ACCOUNT LOADING
    // -----------------------------------------

    private void loadCloudProfile(
            FirebaseUser user,
            int token
    ) {
        if (!validUser(user, token)) {
            return;
        }

        updateStage(
                token,
                "Loading player profile..."
        );

        database.child("playerProfiles")
                .child(user.getUid())
                .get()
                .addOnCompleteListener(this, task -> {
                    if (!validUser(user, token)) {
                        return;
                    }

                    if (!task.isSuccessful()) {
                        fail(
                                token,
                                "Loading player profile failed",
                                task.getException()
                        );
                        return;
                    }

                    DataSnapshot snapshot =
                            task.getResult();

                    if (snapshot == null
                            || !snapshot.exists()) {

                        needsProfile = true;

                        finishOperation(token);
                        showGuestMode();

                        if (authDialog != null
                                && authDialog.isShowing()) {

                            showAuthMode(
                                    AuthMode.COMPLETE_PROFILE
                            );

                            showAuthError(
                                    "Your email account exists. "
                                            + "Choose a username to finish "
                                            + "creating your player profile."
                            );
                        }

                        return;
                    }

                    String name = snapshot
                            .child("playerName")
                            .getValue(String.class);

                    if (name == null ||
                            name.trim().isEmpty()) {

                        finishOperation(token);
                        showGuestMode();

                        showAuthError(
                                "Player profile has no username."
                        );

                        return;
                    }

                    loadCloudProgress(
                            user,
                            name,
                            token
                    );
                });
    }

    private void loadCloudProgress(
            FirebaseUser user,
            String name,
            int token
    ) {
        updateStage(
                token,
                "Loading saved level..."
        );

        DatabaseReference progressRef =
                database.child("playerProgress")
                        .child(user.getUid());

        progressRef.get()
                .addOnCompleteListener(this, task -> {
                    if (!validUser(user, token)) {
                        return;
                    }

                    if (!task.isSuccessful()) {
                        fail(
                                token,
                                "Loading saved progress failed",
                                task.getException()
                        );
                        return;
                    }

                    DataSnapshot snapshot =
                            task.getResult();

                    if (snapshot == null
                            || !snapshot.exists()) {

                        createInitialProgress(
                                user,
                                name,
                                token
                        );
                        return;
                    }

                    int level = 1;

                    Object value = snapshot.child(
                            "highestUnlockedLevel"
                    ).getValue();

                    if (value instanceof Number) {
                        level = Math.max(
                                1,
                                ((Number) value).intValue()
                        );
                    }

                    activateCloudAccount(
                            user,
                            name,
                            level,
                            token
                    );
                });
    }

    private void createInitialProgress(
            FirebaseUser user,
            String name,
            int token
    ) {
        updateStage(
                token,
                "Creating player progress..."
        );

        Map<String, Object> progress =
                new HashMap<>();

        progress.put(
                "highestUnlockedLevel", 1
        );

        progress.put(
                "bestScore", 0
        );

        database.child("playerProgress")
                .child(user.getUid())
                .setValue(progress)
                .addOnCompleteListener(this, task -> {
                    if (!validUser(user, token)) {
                        return;
                    }

                    if (!task.isSuccessful()) {
                        fail(
                                token,
                                "Creating player progress failed",
                                task.getException()
                        );
                        return;
                    }

                    activateCloudAccount(
                            user,
                            name,
                            1,
                            token
                    );
                });
    }

    private void activateCloudAccount(
            FirebaseUser user,
            String name,
            int level,
            int token
    ) {
        if (!validUser(user, token)) return;

        cloudReady = true;
        needsProfile = false;

        cloudPlayerName = name;
        cloudLevel = Math.max(1, level);

        playerNameEditText.setText(name);
        playerNameEditText.setFocusable(false);
        playerNameEditText.setCursorVisible(false);

        levelStatusText.setText(
                "LEVEL " + cloudLevel
        );

        accountButton.setText("SIGN OUT");
        startButton.setEnabled(true);

        finishOperation(token);
        dismissAuthDialog();

        Log.d(
                TAG,
                "Cloud account ready for " + name
        );

        Toast.makeText(
                this,
                "Welcome, " + name + "!",
                Toast.LENGTH_SHORT
        ).show();
    }

    // -----------------------------------------
    // AUTH DIALOG
    // -----------------------------------------

    private void showAuthDialog() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        if (authDialog != null
                && authDialog.isShowing()) {
            return;
        }

        authDialog = new Dialog(this);

        authDialog.requestWindowFeature(
                Window.FEATURE_NO_TITLE
        );

        authDialog.setContentView(
                R.layout.dialog_auth
        );

        authDialog.setCancelable(true);
        authDialog.setCanceledOnTouchOutside(false);

        authTitle =
                authDialog.findViewById(
                        R.id.authTitle
                );

        authSubtitle =
                authDialog.findViewById(
                        R.id.authSubtitle
                );

        usernameInput =
                authDialog.findViewById(
                        R.id.authUsername
                );

        emailInput =
                authDialog.findViewById(
                        R.id.authEmail
                );

        passwordInput =
                authDialog.findViewById(
                        R.id.authPassword
                );

        authErrorText =
                authDialog.findViewById(
                        R.id.authErrorText
                );

        submitButton =
                authDialog.findViewById(
                        R.id.authSubmitButton
                );

        switchButton =
                authDialog.findViewById(
                        R.id.authSwitchButton
                );

        resetButton =
                authDialog.findViewById(
                        R.id.authResetButton
                );

        backButton =
                authDialog.findViewById(
                        R.id.authBackButton
                );

        authProgress =
                authDialog.findViewById(
                        R.id.authProgress
                );

        submitButton.setOnClickListener(
                v -> submitAuthentication()
        );

        switchButton.setOnClickListener(v -> {
            if (authMode == AuthMode.LOGIN) {
                showAuthMode(AuthMode.SIGNUP);

            } else if (authMode == AuthMode.SIGNUP) {
                showAuthMode(AuthMode.LOGIN);

            } else {
                operationToken++;
                auth.signOut();

                needsProfile = false;
                showGuestMode();
                showAuthMode(AuthMode.LOGIN);
            }
        });

        resetButton.setOnClickListener(
                v -> resetPassword()
        );

        backButton.setOnClickListener(
                v -> dismissAuthDialog()
        );

        authDialog.setOnDismissListener(dialog -> {
            operationToken++;
            cancelTimeout();
            authDialog = null;
        });

        authDialog.show();

        Window window = authDialog.getWindow();

        if (window != null) {
            window.setBackgroundDrawable(
                    new ColorDrawable(
                            Color.TRANSPARENT
                    )
            );

            window.addFlags(
                    WindowManager.LayoutParams
                            .FLAG_DIM_BEHIND
            );

            WindowManager.LayoutParams params =
                    window.getAttributes();

            params.width = (int) (
                    getResources()
                            .getDisplayMetrics()
                            .widthPixels * 0.90f
            );

            params.height =
                    WindowManager.LayoutParams.WRAP_CONTENT;

            params.gravity = Gravity.CENTER;
            params.dimAmount = 0.7f;

            window.setAttributes(params);

            window.setSoftInputMode(
                    WindowManager.LayoutParams
                            .SOFT_INPUT_ADJUST_RESIZE
            );
        }

        showAuthMode(
                auth.getCurrentUser() == null
                        ? AuthMode.LOGIN
                        : AuthMode.COMPLETE_PROFILE
        );
    }

    private void dismissAuthDialog() {
        if (authDialog != null) {
            authDialog.dismiss();
            authDialog = null;
        }
    }

    private void showAuthMode(AuthMode mode) {
        authMode = mode;

        if (authDialog == null) return;

        clearAuthError();
        setBusy(false);

        if (mode == AuthMode.LOGIN) {
            authTitle.setText("WELCOME BACK");
            authSubtitle.setText(
                    "Sign in to continue"
            );

            usernameInput.setVisibility(View.GONE);
            emailInput.setVisibility(View.VISIBLE);
            passwordInput.setVisibility(View.VISIBLE);

            submitButton.setText("LOGIN");
            switchButton.setText(
                    "CREATE NEW ACCOUNT"
            );

            resetButton.setVisibility(View.VISIBLE);

        } else if (mode == AuthMode.SIGNUP) {
            authTitle.setText("CREATE ACCOUNT");
            authSubtitle.setText(
                    "Create your cloud account"
            );

            usernameInput.setVisibility(View.VISIBLE);
            emailInput.setVisibility(View.VISIBLE);
            passwordInput.setVisibility(View.VISIBLE);

            submitButton.setText("SIGN UP");
            switchButton.setText(
                    "ALREADY HAVE AN ACCOUNT?"
            );

            resetButton.setVisibility(View.GONE);

        } else {
            authTitle.setText("COMPLETE PROFILE");
            authSubtitle.setText(
                    "Choose your username"
            );

            usernameInput.setVisibility(View.VISIBLE);
            emailInput.setVisibility(View.GONE);
            passwordInput.setVisibility(View.GONE);

            submitButton.setText(
                    "SAVE PLAYER PROFILE"
            );

            switchButton.setText(
                    "USE DIFFERENT ACCOUNT"
            );

            resetButton.setVisibility(View.GONE);
        }
    }

    private void setBusy(boolean busy) {
        if (authDialog == null) return;

        submitButton.setEnabled(!busy);
        switchButton.setEnabled(!busy);
        resetButton.setEnabled(!busy);

        usernameInput.setEnabled(!busy);
        emailInput.setEnabled(!busy);
        passwordInput.setEnabled(!busy);

        authProgress.setVisibility(
                busy ? View.VISIBLE : View.GONE
        );

        // Back remains enabled.
        backButton.setEnabled(true);
    }

    private void clearAuthError() {
        if (authErrorText == null) return;

        authErrorText.setText("");
        authErrorText.setVisibility(View.GONE);
    }

    private void showAuthError(String message) {
        Log.w(TAG, message);

        if (authDialog != null
                && authDialog.isShowing()) {

            authErrorText.setText(message);
            authErrorText.setVisibility(
                    View.VISIBLE
            );

        } else if (!isFinishing()) {
            Toast.makeText(
                    this,
                    message,
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // -----------------------------------------
    // LOGIN
    // -----------------------------------------

    private void submitAuthentication() {
        clearAuthError();

        switch (authMode) {
            case LOGIN:
                login();
                break;

            case SIGNUP:
                register();
                break;

            case COMPLETE_PROFILE:
                completeProfile();
                break;
        }
    }

    private void login() {
        String email = emailInput
                .getText().toString().trim();

        String password = passwordInput
                .getText().toString();

        if (!Patterns.EMAIL_ADDRESS
                .matcher(email).matches()) {

            emailInput.setError("Invalid email");
            return;
        }

        if (password.isEmpty()) {
            passwordInput.setError(
                    "Enter your password"
            );
            return;
        }

        int token = beginOperation(
                "Signing in with Firebase..."
        );

        auth.signInWithEmailAndPassword(
                email,
                password
        ).addOnCompleteListener(this, task -> {
            if (!validOperation(token)) return;

            if (!task.isSuccessful()) {
                fail(
                        token,
                        "Firebase Login failed",
                        task.getException()
                );
                return;
            }

            FirebaseUser user =
                    auth.getCurrentUser();

            if (user == null) {
                finishOperation(token);
                showAuthError(
                        "No user returned after Login."
                );
                return;
            }

            loadCloudProfile(user, token);
        });
    }

    // -----------------------------------------
    // SIGN UP
    // -----------------------------------------

    private void register() {
        String username = usernameInput
                .getText().toString().trim();

        String email = emailInput
                .getText().toString().trim();

        String password = passwordInput
                .getText().toString();

        if (!validUsername(username)) {
            usernameInput.setError(
                    "Use 3-20 letters, numbers or underscores"
            );
            return;
        }

        if (!Patterns.EMAIL_ADDRESS
                .matcher(email).matches()) {

            emailInput.setError("Invalid email");
            return;
        }

        if (password.length() < 8) {
            passwordInput.setError(
                    "Use at least 8 characters"
            );
            return;
        }

        int token = beginOperation(
                "Creating Firebase Auth account..."
        );

        auth.createUserWithEmailAndPassword(
                email,
                password
        ).addOnCompleteListener(this, task -> {
            if (!validOperation(token)) return;

            if (!task.isSuccessful()) {
                fail(
                        token,
                        "Firebase Sign Up failed",
                        task.getException()
                );
                return;
            }

            FirebaseUser user =
                    auth.getCurrentUser();

            if (user == null) {
                finishOperation(token);
                showAuthError(
                        "Account creation returned no user."
                );
                return;
            }

            // The email account now exists.
            // Continue with the player profile.
            createOrCompleteProfile(
                    user,
                    username,
                    token
            );
        });
    }

    private boolean validUsername(
            String username
    ) {
        return username != null
                && username.matches(
                "[A-Za-z0-9_]{3,20}"
        );
    }

    // -----------------------------------------
    // COMPLETE PROFILE
    // -----------------------------------------

    private void completeProfile() {
        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            showAuthMode(AuthMode.LOGIN);
            return;
        }

        String username = usernameInput
                .getText().toString().trim();

        if (!validUsername(username)) {
            usernameInput.setError(
                    "Use 3-20 letters, numbers or underscores"
            );
            return;
        }

        int token = beginOperation(
                "Checking player profile..."
        );

        createOrCompleteProfile(
                user,
                username,
                token
        );
    }

    private void createOrCompleteProfile(
            FirebaseUser user,
            String username,
            int token
    ) {
        updateStage(
                token,
                "Checking existing player profile..."
        );

        database.child("playerProfiles")
                .child(user.getUid())
                .get()
                .addOnCompleteListener(this, task -> {
                    if (!validUser(user, token)) return;

                    if (!task.isSuccessful()) {
                        fail(
                                token,
                                "Checking profile failed",
                                task.getException()
                        );
                        return;
                    }

                    DataSnapshot snapshot =
                            task.getResult();

                    if (snapshot != null
                            && snapshot.exists()) {

                        loadCloudProfile(
                                user,
                                token
                        );
                        return;
                    }

                    reserveUsername(
                            user,
                            username,
                            token
                    );
                });
    }

    // -----------------------------------------
    // RESERVE USERNAME
    // -----------------------------------------

    private void reserveUsername(
            FirebaseUser user,
            String username,
            int token
    ) {
        String normalized =
                username.toLowerCase(Locale.ROOT);

        updateStage(
                token,
                "Reserving username..."
        );

        // Server rules enforce unique usernames.
        // No client transaction is required here.
        database.child("usernames")
                .child(normalized)
                .setValue(user.getUid())
                .addOnCompleteListener(this, task -> {
                    if (!validUser(user, token)) return;

                    if (!task.isSuccessful()) {
                        finishOperation(token);

                        needsProfile = true;

                        showAuthMode(
                                AuthMode.COMPLETE_PROFILE
                        );

                        showAuthError(
                                "Username reservation failed: "
                                        + errorMessage(
                                        task.getException()
                                )
                                        + ". It may already be taken, "
                                        + "or database rules denied it."
                        );

                        showGuestMode();
                        return;
                    }

                    savePlayerProfile(
                            user,
                            username,
                            normalized,
                            token
                    );
                });
    }

    // -----------------------------------------
    // SAVE PLAYER PROFILE
    // -----------------------------------------

    private void savePlayerProfile(
            FirebaseUser user,
            String username,
            String normalized,
            int token
    ) {
        updateStage(
                token,
                "Saving player profile..."
        );

        Map<String, Object> profile =
                new HashMap<>();

        profile.put(
                "uid",
                user.getUid()
        );

        profile.put(
                "playerName",
                username
        );

        profile.put(
                "normalizedName",
                normalized
        );

        database.child("playerProfiles")
                .child(user.getUid())
                .setValue(profile)
                .addOnCompleteListener(this, task -> {
                    if (!validUser(user, token)) return;

                    if (!task.isSuccessful()) {
                        finishOperation(token);

                        needsProfile = true;
                        showGuestMode();

                        showAuthMode(
                                AuthMode.COMPLETE_PROFILE
                        );

                        showAuthError(
                                "Email account created, but "
                                        + "player profile failed: "
                                        + errorMessage(
                                        task.getException()
                                )
                        );
                        return;
                    }

                    loadCloudProfile(
                            user,
                            token
                    );
                });
    }

    // -----------------------------------------
    // PASSWORD RESET
    // -----------------------------------------

    private void resetPassword() {
        String email = emailInput
                .getText().toString().trim();

        if (!Patterns.EMAIL_ADDRESS
                .matcher(email).matches()) {

            emailInput.setError(
                    "Enter a valid email"
            );
            return;
        }

        int token = beginOperation(
                "Sending password reset email..."
        );

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(this, task -> {
                    if (!validOperation(token)) return;

                    finishOperation(token);

                    if (task.isSuccessful()) {
                        showAuthError(
                                "Password reset requested. "
                                        + "Check your email inbox."
                        );

                    } else {
                        showAuthError(
                                "Password reset failed: "
                                        + errorMessage(
                                        task.getException()
                                )
                        );
                    }
                });
    }

    // -----------------------------------------
    // SIGN OUT
    // -----------------------------------------

    private void signOut() {
        operationToken++;
        cancelTimeout();

        auth.signOut();

        cloudReady = false;
        needsProfile = false;

        showGuestMode();
        dismissAuthDialog();

        Toast.makeText(
                this,
                "Signed out. Guest mode active.",
                Toast.LENGTH_SHORT
        ).show();
    }

    // -----------------------------------------
    // LIFECYCLE
    // -----------------------------------------

    @Override
    protected void onStart() {
        super.onStart();

        if (menuMusic != null) {
            try {
                if (!menuMusic.isPlaying()) {
                    menuMusic.start();
                }
            } catch (IllegalStateException ignored) {
            }
        }

        // Never force the Login modal open.
        if (!cloudReady) {
            FirebaseUser user =
                    auth.getCurrentUser();

            if (user != null) {
                loadCloudProfile(user, 0);
            } else {
                needsProfile = false;
                showGuestMode();
            }
        }
    }

    @Override
    protected void onStop() {
        if (menuMusic != null) {
            try {
                if (menuMusic.isPlaying()) {
                    menuMusic.pause();
                }
            } catch (IllegalStateException ignored) {
            }
        }

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        operationToken++;
        cancelTimeout();

        dismissAuthDialog();

        if (menuMusic != null) {
            menuMusic.release();
            menuMusic = null;
        }

        if (buttonSound != null) {
            buttonSound.release();
            buttonSound = null;
        }

        super.onDestroy();
    }
}

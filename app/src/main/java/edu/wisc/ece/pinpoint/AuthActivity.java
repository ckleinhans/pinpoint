package edu.wisc.ece.pinpoint;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.Arrays;

import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class AuthActivity extends AppCompatActivity {
    private static final int RELOAD_AUTH_DELAY = 2000;
    private static final String TAG = "AUTH";
    private FirebaseDriver firebase;
    private ViewSwitcher switcher;
    private Handler reloadAuthHandler;
    private ActivityResultLauncher<Intent> authLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        firebase = FirebaseDriver.getInstance();
        switcher = findViewById(R.id.view_switcher);

        if (firebase.isLoggedIn() && firebase.isVerified()) {
            FirebaseCrashlytics.getInstance().setUserId(firebase.getUid());
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        authLauncher = this.registerForActivityResult(new FirebaseAuthUIActivityResultContract(),
                (result) -> {
                    Log.d("TEST", String.format("Result code: %d", result.getResultCode()));
                    if (result.getResultCode() == RESULT_OK) {
                        if (firebase.isNewUser()) {
                            // TODO: currently don't handle if this call fails, which would make
                            //  user's app crash on launch & be unusable
                            firebase.handleNewUser();
                            if (!firebase.isVerified()) {
                                firebase.sendEmailVerification(null);
                            }
                        }
                        if (firebase.isVerified()) {
                            FirebaseCrashlytics.getInstance().setUserId(firebase.getUid());
                            Intent intent = new Intent(AuthActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    }
                });
    }


    @Override
    public void onStart() {
        super.onStart();
        if (!firebase.isLoggedIn()) {
            showView(R.id.loading_view);
            // If user not signed in prompt them to sign into tester account before pinpoint account
            if (!firebase.isTesterSignedIn()) firebase.signInTester(this, (d, b) -> launchAuth());
            else launchAuth();
        } else if (!firebase.isVerified()) {
            showView(R.id.verify_email_view);
            startAuthReloadHandler();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // Stop auth reload handler when app goes to background
        if (reloadAuthHandler != null) {
            reloadAuthHandler.removeCallbacksAndMessages(null);
            reloadAuthHandler = null;
            Log.d(TAG, "Auth reload stopped.");
        }
    }

    public void resendEmail(View view) {
        // Remove button from view to prevent spam
        view.setVisibility(View.INVISIBLE);
        firebase.sendEmailVerification(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Email verification sent! Check your inbox.",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Couldn't send verification email. Try again later.",
                        Toast.LENGTH_LONG).show();
                view.setVisibility(View.VISIBLE);
            }
        });
    }

    public void logout(View view) {
        showView(R.id.loading_view);
        if (reloadAuthHandler != null) {
            reloadAuthHandler.removeCallbacksAndMessages(null);
            reloadAuthHandler = null;
            Log.d(TAG, "Auth reload stopped.");
        }
        firebase.logout(this).addOnCompleteListener(task -> launchAuth());
    }

    private void launchAuth() {
        Intent signInIntent =
                AuthUI.getInstance().createSignInIntentBuilder().setLogo(R.drawable.pinpoint)
                        .setTheme(R.style.Theme_Pinpoint)
                        // Enable smart lock only on production builds
                        .setIsSmartLockEnabled(!BuildConfig.DEBUG, true).setAvailableProviders(
                                Arrays.asList(new AuthUI.IdpConfig.EmailBuilder().build(),
                                        new AuthUI.IdpConfig.GoogleBuilder().build(),
                                        new AuthUI.IdpConfig.GitHubBuilder().build()
                                        // new AuthUI.IdpConfig.FacebookBuilder().build(),
                                        // new AuthUI.IdpConfig.AnonymousBuilder().build(),
                                )).build();

        authLauncher.launch(signInIntent);
    }

    private void showView(int viewId) {
        if (switcher.getNextView().getId() == viewId) {
            switcher.showNext();
        }
    }

    private void startAuthReloadHandler() {
        if (reloadAuthHandler != null) {
            return;
        }
        reloadAuthHandler = new Handler(getMainLooper());
        Runnable reloadAuth = new Runnable() {
            @Override
            public void run() {
                if (!firebase.isLoggedIn()) {
                    Log.d(TAG, "User logged out.");
                }
                Log.d(TAG, "Reloading user auth state...");
                firebase.reloadAuth().addOnCompleteListener((reloadTask -> {
                    if (!firebase.isLoggedIn()) {
                        Log.d(TAG, "User logged out.");
                    } else if (firebase.isVerified()) {
                        Log.d(TAG, "User verified!");
                        FirebaseCrashlytics.getInstance().setUserId(firebase.getUid());
                        Intent intent = new Intent(AuthActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Log.d(TAG, "Not verified, scheduling next reload...");
                        if (reloadAuthHandler != null) {
                            reloadAuthHandler.postDelayed(this, RELOAD_AUTH_DELAY);
                        }
                    }
                }));
            }
        };
        Log.d(TAG, "Starting auth reload...");
        reloadAuthHandler.postDelayed(reloadAuth, RELOAD_AUTH_DELAY);
    }
}
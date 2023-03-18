package edu.wisc.ece.pinpoint;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;

import java.util.Arrays;

import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class AuthActivity extends AppCompatActivity {
    private static final int RELOAD_AUTH_DELAY = 5000;
    private FirebaseDriver firebase;
    private ViewSwitcher switcher;
    private Handler reloadAuthHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        firebase = FirebaseDriver.getInstance();
        switcher = findViewById(R.id.view_switcher);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!firebase.isLoggedIn()) {
            launchAuth();
        } else if (!firebase.isVerified()) {
            // Switch to email verification view
            if (switcher.getNextView().getId() == R.id.verify_email_view) {
                switcher.showNext();
            }
            // Start periodically reloading auth to know if user becomes verified
            startAuthReloadHandler();
        } else {
            // User is logged in and verified
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void launchAuth() {
        ActivityResultLauncher<Intent> authLauncher =
                this.registerForActivityResult(new FirebaseAuthUIActivityResultContract(),
                        (result) -> {
                            if (result.getResultCode() == RESULT_OK) {
                                // TODO: post user's username to DB node so it is visible to others
                                if (!firebase.isVerified()) {
                                    firebase.sendEmailVerification(null);
                                }
                            }
                        });

        Intent signInIntent =
                AuthUI.getInstance().createSignInIntentBuilder().setLogo(R.mipmap.ic_launcher)
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

    private void startAuthReloadHandler() {
        if (reloadAuthHandler != null) {
            return;
        }
        reloadAuthHandler = new Handler(Looper.myLooper());
        Runnable reloadAuth = new Runnable() {
            @Override
            public void run() {
                if (!firebase.isLoggedIn()) {
                    return;
                }
                if (firebase.isVerified()) {
                    // Verified, start main activity and finish auth activity
                    Intent intent = new Intent(AuthActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    // Reload auth and schedule verification check again
                    firebase.reloadAuth();
                    reloadAuthHandler.postDelayed(this, RELOAD_AUTH_DELAY);
                }
            }
        };
        reloadAuthHandler.postDelayed(reloadAuth, RELOAD_AUTH_DELAY);
    }
}
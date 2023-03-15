package edu.wisc.ece.pinpoint.utils;

import android.content.Context;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;

import edu.wisc.ece.pinpoint.BuildConfig;
import edu.wisc.ece.pinpoint.R;

public final class FirebaseDriver {
    private static FirebaseDriver instance;
    private final FirebaseAuth auth;

    private FirebaseDriver() {
        if (instance != null) {
            throw new IllegalStateException("FirebaseDriver has already been instantiated.");
        }
        instance = this;
        auth = FirebaseAuth.getInstance();
    }

    public static FirebaseDriver getInstance() {
        if (instance == null) {
            new FirebaseDriver();
        }
        return instance;
    }

    public FirebaseUser getUser() {
        return auth.getCurrentUser();
    }

    public void launchAuth(AppCompatActivity activity) {
        ActivityResultLauncher<Intent> authLauncher =
                activity.registerForActivityResult(new FirebaseAuthUIActivityResultContract(),
                        (result) -> {
                            // Handle the FirebaseAuthUIAuthenticationResult
                        });

        Intent signInIntent =
                AuthUI.getInstance().createSignInIntentBuilder().setLogo(R.mipmap.ic_launcher)
                        // Enable smart lock only on production builds
                        .setIsSmartLockEnabled(!BuildConfig.DEBUG, true)
                        .setAvailableProviders(
                                Arrays.asList(new AuthUI.IdpConfig.EmailBuilder().build(),
                                        new AuthUI.IdpConfig.GoogleBuilder().build(),
                                        new AuthUI.IdpConfig.GitHubBuilder().build()
                                        // new AuthUI.IdpConfig.FacebookBuilder().build(),
                                        // new AuthUI.IdpConfig.AnonymousBuilder().build(),
                                )).build();
        authLauncher.launch(signInIntent);
    }

    public void logout(Context context) {
        AuthUI.getInstance().signOut(context);
    }
}

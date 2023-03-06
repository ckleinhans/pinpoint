package edu.wisc.ece.pinpoint.utils;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public final class FirebaseDriver {
    private static FirebaseDriver instance;
    private final FirebaseAuth auth;

    private FirebaseDriver() {
        auth = FirebaseAuth.getInstance();
    }

    public static FirebaseDriver getInstance() {
        if (instance == null) {
            instance = new FirebaseDriver();
        }
        return instance;
    }

    public FirebaseUser getUser() {
        return auth.getCurrentUser();
    }

    public Task<AuthResult> registerUserPassword(String email, String password) {
        return auth.createUserWithEmailAndPassword(email, password);
    }

    public Task<AuthResult> loginUserPassword(String email, String password) {
        return auth.signInWithEmailAndPassword(email, password);
    }
}

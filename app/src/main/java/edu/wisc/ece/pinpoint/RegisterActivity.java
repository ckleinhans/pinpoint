package edu.wisc.ece.pinpoint;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.UserProfileChangeRequest;

import edu.wisc.ece.pinpoint.utils.FirebaseDriver;
import edu.wisc.ece.pinpoint.utils.ValidationUtils;

public class RegisterActivity extends AppCompatActivity {
    private EditText usernameInput;
    private TextInputLayout usernameInputLayout;
    private EditText emailInput;
    private TextInputLayout emailInputLayout;
    private EditText passwordInput;
    private TextInputLayout passwordInputLayout;
    private EditText confirmPasswordInput;
    private TextInputLayout confirmPasswordInputLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        usernameInput = findViewById(R.id.usernameInput);
        usernameInputLayout = findViewById(R.id.usernameInputLayout);
        emailInput = findViewById(R.id.emailInput);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInput = findViewById(R.id.passwordInput);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(getString(R.string.register));
        }
    }

    public void handleRegister(View view) {
        boolean isValid = true;
        if (ValidationUtils.isEmpty(usernameInput)) {
            usernameInputLayout.setError(getString(R.string.usernameBlank));
            isValid = false;
        } else {
            usernameInputLayout.setErrorEnabled(false);
        }
        if (ValidationUtils.isNotEmail(emailInput)) {
            emailInputLayout.setError(getString(R.string.invalidEmail));
            isValid = false;
        } else {
            emailInputLayout.setErrorEnabled(false);
        }
        if (ValidationUtils.isEmpty(passwordInput)) {
            passwordInputLayout.setError(getString(R.string.blankPassword));
            isValid = false;
        } else {
            passwordInputLayout.setErrorEnabled(false);
        }
        if (!ValidationUtils.isEqual(passwordInput, confirmPasswordInput)) {
            confirmPasswordInputLayout.setError(getString(R.string.passwordsNotMatched));
            isValid = false;
        } else {
            confirmPasswordInputLayout.setErrorEnabled(false);
        }
        if (isValid) {
            String email = emailInput.getText().toString();
            String password = passwordInput.getText().toString();
            FirebaseDriver.getInstance().registerUserPassword(email, password).addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setDisplayName(usernameInput.getText().toString()).build();
                    FirebaseDriver.getInstance().getUser().updateProfile(profileUpdates);
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(RegisterActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
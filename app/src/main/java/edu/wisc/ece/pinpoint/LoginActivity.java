package edu.wisc.ece.pinpoint;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

import edu.wisc.ece.pinpoint.utils.FirebaseDriver;
import edu.wisc.ece.pinpoint.utils.ValidationUtils;

public class LoginActivity extends AppCompatActivity {
    private EditText emailInput;
    private TextInputLayout emailInputLayout;
    private EditText passwordInput;
    private TextInputLayout passwordInputLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        emailInput = findViewById(R.id.emailInput);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInput = findViewById(R.id.passwordInput);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        if (FirebaseDriver.getInstance().getUser() != null) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
        }
    }

    public void handleLogin(View view) {
        boolean isValid = true;
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
        if (isValid) {
            String email = emailInput.getText().toString();
            String password = passwordInput.getText().toString();
            FirebaseDriver.getInstance().loginUserPassword(email, password).addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void handleRegister(View view) {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }
}
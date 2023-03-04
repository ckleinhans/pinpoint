package edu.wisc.ece.pinpoint.utils;

import android.util.Patterns;
import android.widget.EditText;

public final class ValidationUtils {

    // Checks if a the trimmed content in an EditText is empty
    public static boolean isEmpty(EditText text) {
        return text.getText().toString().trim().isEmpty();
    }

    // Checks if the EditText content is an email
    public static boolean isNotEmail(EditText text) {
        return !Patterns.EMAIL_ADDRESS.matcher(text.getText().toString()).matches();
    }

    public static boolean isEqual(EditText text1, EditText text2) {
        return text1.getText().toString().equals(text2.getText().toString());
    }

    // Checks if the EditText content is at least a certain length
    public static boolean isAtLeastLength(EditText text, int length) {
        return text.getText().toString().length() >= length;
    }
}

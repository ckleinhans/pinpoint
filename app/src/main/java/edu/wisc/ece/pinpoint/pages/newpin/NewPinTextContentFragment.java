package edu.wisc.ece.pinpoint.pages.newpin;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import edu.wisc.ece.pinpoint.R;


public class NewPinTextContentFragment extends Fragment {

    private NestedScrollView scrollView;
    private EditText textInput;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_new_pin_text_content, container, false);
    }

}
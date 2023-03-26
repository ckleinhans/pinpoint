package edu.wisc.ece.pinpoint.pages.pins;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.DateFormat;
import java.util.Calendar;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class PinViewFragment extends Fragment {
    private TextView textView;
    private TextView nameView;
    private TextView captionView;
    private TextView likeCount;
    private TextView commentCount;
    private TextView textContentView;
    private FirebaseDriver firebase;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pin_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textView = requireView().findViewById(R.id.pin_post_time);
        nameView = requireView().findViewById(R.id.pin_username);
        captionView = requireView().findViewById(R.id.pin_caption);
        likeCount = requireView().findViewById(R.id.pin_like_count);
        commentCount = requireView().findViewById(R.id.pin_comment_count);
        textContentView = requireView().findViewById(R.id.pin_text_content);

        Calendar calendar = Calendar.getInstance();
        String currentTime = DateFormat.getDateInstance().format(calendar.getTime());
        textView.setText(currentTime);
        nameView.setText("Bhuvi");
        captionView.setText("Amazing place");
        likeCount.setText("12 likes");
        commentCount.setText("20 Comments");
        textContentView.setText("Hyd");
    }
}

package edu.wisc.ece.pinpoint.pages.pinList;

        import androidx.annotation.NonNull;
        import androidx.annotation.Nullable;
        import androidx.appcompat.app.AppCompatActivity;
        import androidx.fragment.app.Fragment;
        import androidx.navigation.Navigation;


        import android.os.Build;
        import android.os.Bundle;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.TextView;


        import java.text.DateFormat;
        import java.util.Calendar;

        import edu.wisc.ece.pinpoint.R;
        import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class pinViewFragment extends Fragment {
    private TextView textView;
    private TextView nameView;
    private TextView descriptionView;
    private TextView likeCount;
    private TextView commentCount;
    private TextView messageView;

    private FirebaseDriver firebase;
    

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pin_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        textView = requireView().findViewById(R.id.post_time_text);
        nameView = requireView().findViewById(R.id.display_name_text_view);
        descriptionView = requireView().findViewById(R.id.description_textview);
        likeCount = requireView().findViewById(R.id.like_count_textview);
        commentCount = requireView().findViewById(R.id.comment_count_textview);
        messageView = requireView().findViewById(R.id.messageView);

        Calendar calendar = Calendar.getInstance();
        String currentTime = DateFormat.getDateInstance().format(calendar.getTime());
        textView.setText(currentTime);
        nameView.setText("Bhuvi");
        descriptionView.setText("Amazing place");
        likeCount.setText("12 likes");
        commentCount.setText("20 Comments");
        messageView.setText("Hyd");

    }

}

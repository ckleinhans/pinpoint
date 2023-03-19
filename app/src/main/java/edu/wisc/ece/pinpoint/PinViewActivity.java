package edu.wisc.ece.pinpoint;

        import androidx.appcompat.app.AppCompatActivity;


        import android.os.Bundle;
        import android.widget.TextView;


        import java.text.DateFormat;
        import java.util.Calendar;

        import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class PinViewActivity extends AppCompatActivity {
    private TextView textView;
    private TextView nameView;
    private TextView descriptionView;
    private TextView likeCount;
    private TextView commentCount;
    private TextView messageView;

    private FirebaseDriver firebase;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pin_view);

        textView = findViewById(R.id.post_time_text);
        nameView = findViewById(R.id.display_name_text_view);
        descriptionView = findViewById(R.id.description_textview);
        likeCount = findViewById(R.id.like_count_textview);
        commentCount = findViewById(R.id.comment_count_textview);
        messageView = findViewById(R.id.messageView);

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

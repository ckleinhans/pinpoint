package edu.wisc.ece.pinpoint.pages.pins;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.text.DateFormat;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.Pin;
import edu.wisc.ece.pinpoint.data.User;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;

public class PinViewFragment extends Fragment {
    private FirebaseDriver firebase;
    private NavController navController;
    private TextView timestamp;
    private TextView authorUsername;
    private ImageView authorProfilePic;
    private TextView caption;
    private TextView foundCount;
    private TextView commentCount;
    private TextView textContent;
    private ImageView imageContent;
    private ConstraintLayout metadataBar;
    private String pid;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pin_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebase = FirebaseDriver.getInstance();
        navController = Navigation.findNavController(view);
        timestamp = requireView().findViewById(R.id.pin_post_time);
        authorUsername = requireView().findViewById(R.id.pin_author_username);
        authorProfilePic = requireView().findViewById(R.id.pin_author_profile_pic);
        caption = requireView().findViewById(R.id.pin_caption);
        foundCount = requireView().findViewById(R.id.pin_found_count);
        commentCount = requireView().findViewById(R.id.pin_comment_count);
        textContent = requireView().findViewById(R.id.pin_text_content);
        imageContent = requireView().findViewById(R.id.pin_image_content);
        metadataBar = requireView().findViewById(R.id.pin_view_metadata);
        ImageButton backButton = requireView().findViewById(R.id.pin_view_back_button);

        backButton.setOnClickListener((v) -> navController.popBackStack());

        // Fetch pin data & load using argument
        Bundle args = getArguments();
        pid = args != null ? PinViewFragmentArgs.fromBundle(args).getPid() : null;
        if (pid == null) {
            navController.popBackStack();
            return;
        }
        Pin cachedPin = firebase.getCachedPin(pid);
        if (cachedPin != null) {
            setPinData(cachedPin);
        } else {
            // Since pin data shouldn't change, only fetch if not cached
            firebase.fetchPin(pid).addOnCompleteListener(task -> setPinData(task.getResult()));
        }
    }

    public void setPinData(Pin pin) {
        User cachedAuthor = firebase.getCachedUser(pin.getAuthorUID());
        if (cachedAuthor != null) {
            setPinAuthorData(cachedAuthor);
        } else {
            // Since only using author for profile pic & username, only fetch if not cached
            firebase.fetchUser(pin.getAuthorUID())
                    .addOnCompleteListener(task -> setPinAuthorData(task.getResult()));
        }

        timestamp.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(pin.getTimestamp()));
        if (pin.getCaption() == null) {
            caption.setVisibility(View.GONE);
        } else {
            caption.setText(pin.getCaption());
        }
        foundCount.setText("12 finds");
        commentCount.setText("20 Comments");
        if (pin.getType() == Pin.PinType.IMAGE) {
            firebase.loadPinImage(imageContent, requireContext(), pid);
        } else {
            textContent.setText(pin.getTextContent());
        }

        // Set metadata bar above pin content to bring users to author profile page on click
        metadataBar.setOnClickListener(v -> navController.navigate(
                PinViewFragmentDirections.profile().setUid(pin.getAuthorUID())));
    }

    public void setPinAuthorData(User author) {
        authorUsername.setText(author.getUsername());
        author.loadProfilePic(authorProfilePic, this);
    }
}

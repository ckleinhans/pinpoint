package edu.wisc.ece.pinpoint.pages.pins;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.lang.reflect.Field;
import java.util.List;

import edu.wisc.ece.pinpoint.R;
import edu.wisc.ece.pinpoint.data.Comment;
import edu.wisc.ece.pinpoint.data.Pin;
import edu.wisc.ece.pinpoint.data.User;
import edu.wisc.ece.pinpoint.utils.FirebaseDriver;
import edu.wisc.ece.pinpoint.utils.FormatUtils;
import edu.wisc.ece.pinpoint.utils.ValidationUtils;

public class PinViewFragment extends Fragment {
    private static final String TAG = PinViewFragment.class.getName();
    private static final int SHARE = 0;
    private static final int REPORT = 1;
    private static final int DELETE = 2;
    private final int COMMENT_FOCUS_SCROLL = 1000;
    private FirebaseDriver firebase;
    private NavController navController;
    private TextView timestamp;
    private TextView authorUsername;
    private ImageView authorProfilePic;
    private TextView caption;
    private TextView foundCount;
    private TextView commentCount;
    private TextView textContent;
    private TextView location;
    private ConstraintLayout locationLayout;
    private ImageView imageContent;
    private ConstraintLayout metadataBar;
    private String pid;
    private String authorUID;
    private ImageView addCommentButton;
    private ConstraintLayout addCommentLayout;
    private TextInputEditText addCommentEditText;
    private NestedScrollView scrollView;
    private List<Comment> comments;

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
        location = requireView().findViewById(R.id.pin_location);
        locationLayout = requireView().findViewById(R.id.pin_location_layout);
        foundCount = requireView().findViewById(R.id.pin_found_count);
        commentCount = requireView().findViewById(R.id.pin_comment_count);
        textContent = requireView().findViewById(R.id.pin_text_content);
        imageContent = requireView().findViewById(R.id.pin_image_content);
        metadataBar = requireView().findViewById(R.id.pin_view_metadata);
        addCommentButton = requireView().findViewById(R.id.add_comment_button);
        addCommentEditText = requireView().findViewById(R.id.comment_edittext_layout);
        addCommentLayout = requireView().findViewById(R.id.pin_comment_layout);
        scrollView = requireView().findViewById(R.id.viewpin_scrollview);

        ImageView sendCommentButton = requireView().findViewById(R.id.send_comment_button);
        sendCommentButton.setOnClickListener(this::sendCommentHandler);

        ImageButton backButton = requireView().findViewById(R.id.pin_view_back_button);
        backButton.setOnClickListener((v) -> navController.popBackStack());

        ImageButton optionsButton = requireView().findViewById(R.id.pin_view_options_button);
        optionsButton.setOnClickListener(this::showOptionsMenu);

        addCommentButton.setOnClickListener((v) -> {
            if (addCommentLayout.getVisibility() == View.GONE) {
                addCommentButton.setForeground(
                        ContextCompat.getDrawable(requireContext(), R.drawable.ic_cancel_comment));
                addCommentLayout.setVisibility(View.VISIBLE);
            } else {
                addCommentButton.setForeground(
                        ContextCompat.getDrawable(requireContext(), R.drawable.ic_add_comment));
                addCommentLayout.setVisibility(View.GONE);
            }
        });

        addCommentEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && Resources.getSystem()
                    .getDisplayMetrics().heightPixels == requireView().getHeight()) {
                scrollView.postDelayed(() -> scrollView.scrollTo(0, COMMENT_FOCUS_SCROLL), 150);
            }
        });

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

        fetchAndDisplayComments();
    }

    private void sendCommentHandler(View v) {
        if (!ValidationUtils.isEmpty(addCommentEditText)) {
            //noinspection ConstantConditions
            Comment comment = new Comment(addCommentEditText.getText().toString());
            firebase.postComment(comment, pid).addOnSuccessListener(t -> {
                // clear comment text and hide posting components
                addCommentEditText.setText("");
                addCommentLayout.setVisibility(View.GONE);
                if (getContext() != null) {
                    addCommentButton.setForeground(
                            ContextCompat.getDrawable(getContext(), R.drawable.ic_add_comment));
                }

                // add new comment locally, refresh display
                comments.add(0, comment);
                updateCommentList(comments);
            }).addOnFailureListener(t -> {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to post comment", Toast.LENGTH_LONG)
                            .show();
                }
            });

        }
    }

    private void fetchAndDisplayComments() {
        firebase.fetchComments(pid).addOnCompleteListener(t -> {
            comments = t.getResult();
            updateCommentList(comments);
        });
    }

    private void updateCommentList(List<Comment> comments) {
        commentCount.setText(comments.size() == 1 ? getString(R.string.pin_comments_singular) :
                String.format(getString(R.string.pin_comments_plural), comments.size()));

        if (getView() != null) {
            setupCommentRecyclerView(getView(), comments);
        }
    }

    private void setPinData(Pin pin) {
        authorUID = pin.getAuthorUID();
        User cachedAuthor = firebase.getCachedUser(authorUID);
        if (cachedAuthor != null) {
            setPinAuthorData(cachedAuthor);
        } else {
            // Since only using author for profile pic & username, only fetch if not cached
            firebase.fetchUser(authorUID)
                    .addOnCompleteListener(task -> setPinAuthorData(task.getResult()));
        }

        timestamp.setText(FormatUtils.formattedDateTime(pin.getTimestamp()));
        if (pin.getCaption() == null) {
            caption.setVisibility(View.GONE);
        } else {
            caption.setText(pin.getCaption());
        }

        String locationText = FormatUtils.formattedPinLocation(pin.getBroadLocationName(),
                pin.getNearbyLocationName());
        if (locationText != null) {
            location.setText(locationText);
        } else {
            locationLayout.setVisibility(View.GONE);
        }

        foundCount.setText(pin.getFinds() == 1 ? getString(R.string.pin_finds_singular) :
                String.format(getString(R.string.pin_finds_plural), pin.getFinds()));

        if (pin.getType() == Pin.PinType.IMAGE) {
            firebase.loadPinImage(imageContent, requireContext(), pid);
        } else {
            textContent.setText(pin.getTextContent());
        }

        // Set metadata bar above pin content to bring users to author profile page on click
        metadataBar.setOnClickListener(v -> navController.navigate(
                PinViewFragmentDirections.profile().setUid(pin.getAuthorUID())));
    }

    private void setPinAuthorData(User author) {
        authorUsername.setText(author.getUsername());
        author.loadProfilePic(authorProfilePic, this);
    }

    private void showOptionsMenu(View v) {
        PopupMenu popup = new PopupMenu(requireContext(), v);
        Menu menu = popup.getMenu();
        menu.add(Menu.NONE, SHARE, Menu.NONE, "Share").setIcon(R.drawable.ic_nfc_share);
        if (authorUID.equals(firebase.getUid())) {
            menu.add(Menu.NONE, DELETE, Menu.NONE, "Delete").setIcon(R.drawable.ic_delete);
        } else {
            menu.add(Menu.NONE, REPORT, Menu.NONE, "Report").setIcon(R.drawable.ic_flag);
        }
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case SHARE:
                    // TODO: add NFC sharing logic here
                    Toast.makeText(requireContext(), "NFC pin sharing not yet implemented!",
                            Toast.LENGTH_SHORT).show();
                    return true;
                case REPORT:
                    // TODO: add pin reporting logic here
                    Toast.makeText(requireContext(), "Pin reporting not yet implemented!",
                            Toast.LENGTH_SHORT).show();
                    return true;
                case DELETE:
                    AlertDialog.Builder dialog = new AlertDialog.Builder(requireContext());
                    dialog.setTitle(R.string.pin_delete_confirm_title);
                    dialog.setMessage(R.string.pin_delete_confirm_message);
                    dialog.setPositiveButton(R.string.delete_text, this::deletePin);
                    dialog.setNegativeButton(R.string.cancel_text, (d, buttonId) -> {
                        // Cancelled dialog
                    });
                    dialog.show();
                    return true;
                default:
                    return false;
            }
        });
        // Hacky reflection to make menu show icons, couldn't find any better way online
        try {
            //noinspection DiscouragedPrivateApi,JavaReflectionMemberAccess
            Field fMenuHelper = PopupMenu.class.getDeclaredField("mPopup");
            fMenuHelper.setAccessible(true);
            Object menuHelper = fMenuHelper.get(popup);
            //noinspection rawtypes
            Class[] argTypes = new Class[]{boolean.class};
            //noinspection ConstantConditions
            menuHelper.getClass().getDeclaredMethod("setForceShowIcon", argTypes)
                    .invoke(menuHelper, true);
        } catch (Exception e) {
            // Error doesn't matter, menu will just show without icons
        }
        popup.show();
    }

    private void deletePin(DialogInterface dialog, int buttonId) {
        firebase.deletePin(pid).addOnFailureListener(e -> {
            Log.w(TAG, e);
            Toast.makeText(requireContext(),
                    "Something went wrong deleting your pin. Please try again later.",
                    Toast.LENGTH_LONG).show();
        }).addOnSuccessListener(t -> {
            Toast.makeText(requireContext(), "Successfully deleted your pin!", Toast.LENGTH_LONG)
                    .show();
            navController.popBackStack();
        });
    }

    private void setupCommentRecyclerView(View view, List<Comment> comments) {
        RecyclerView commentRecyclerView = view.findViewById(R.id.comment_recycler_view);
        NavController navController =
                Navigation.findNavController(requireParentFragment().requireView());
        commentRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1));
        commentRecyclerView.setAdapter(new PinCommentAdapter(comments, navController, this));
        commentRecyclerView.setItemAnimator(new DefaultItemAnimator());
        commentRecyclerView.setNestedScrollingEnabled(false);
    }
}

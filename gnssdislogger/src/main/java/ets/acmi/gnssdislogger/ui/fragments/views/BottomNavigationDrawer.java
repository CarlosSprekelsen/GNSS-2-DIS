package ets.acmi.gnssdislogger.ui.fragments.views;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.navigation.NavigationView;

import de.greenrobot.event.EventBus;
import ets.acmi.gnssdislogger.R;
import ets.acmi.gnssdislogger.common.events.CommandEvents;
import ets.acmi.gnssdislogger.ui.activity.MainPreferenceActivity;


public class BottomNavigationDrawer extends BottomSheetDialogFragment {

    public static final String TAG = "bottom_navigation_drawer";
    private ImageView closeButton;
    //Bottom Sheet Callback
    private final BottomSheetBehavior.BottomSheetCallback mBottomSheetBehaviorCallback = new BottomSheetBehavior.BottomSheetCallback() {

        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss();
            }

        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            //check the slide offset and change the visibility of close button
            if (slideOffset > 0.5) {
                closeButton.setVisibility(View.VISIBLE);
            } else {
                closeButton.setVisibility(View.GONE);
            }
        }
    };

    public static BottomNavigationDrawer newInstance() {
        Bundle args = new Bundle();
        BottomNavigationDrawer drawer = new BottomNavigationDrawer();
        drawer.setArguments(args);
        return drawer;
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        //Get the content View
        View contentView = View.inflate(getContext(), R.layout.fragment_bottomsheet, null);
        dialog.setContentView(contentView);

        NavigationView navigationView = contentView.findViewById(R.id.navigation_view);


        //implement navigation menu item click event
        navigationView.setNavigationItemSelectedListener(item -> {
            EventBus.getDefault().post(new CommandEvents.MenuOptionClicked(item.getItemId()));
            dismiss();
            return false;
        });
        closeButton = contentView.findViewById(R.id.close_image_view);
        closeButton.setOnClickListener(view -> {
            //dismiss bottom sheet
            dismiss();
        });


        //Set the coordinator layout behavior
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) ((View) contentView.getParent()).getLayoutParams();
        CoordinatorLayout.Behavior behavior = params.getBehavior();

        //Set callback
        if (behavior instanceof BottomSheetBehavior) {
            ((BottomSheetBehavior) behavior).addBottomSheetCallback(mBottomSheetBehaviorCallback);
        }
    }

    //
    //Helper method, launches activity in a delayed handler, less stutter
    //
    // TODO: put this helper method in a common view management class or implement class interface
    private void launchPreferenceScreen(final String whichFragment) {
        new Handler().postDelayed(() -> {
            Context context = getContext();
            Intent targetActivity = new Intent(context.getApplicationContext(), MainPreferenceActivity.class);
            targetActivity.putExtra("preference_fragment", whichFragment);
            startActivity(targetActivity);
        }, 250);
    }

}






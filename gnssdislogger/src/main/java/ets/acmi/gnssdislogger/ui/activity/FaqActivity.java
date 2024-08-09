package ets.acmi.gnssdislogger.ui.activity;

import android.os.Bundle;
import android.widget.ExpandableListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import ets.acmi.gnssdislogger.R;
import ets.acmi.gnssdislogger.ui.components.faqExpandableListViewAdapter;

public class FaqActivity extends AppCompatActivity {

    private ExpandableListView expandableListView;
    private faqExpandableListViewAdapter expandableListAdapter;
    private List<String> listDataTitle;
    private HashMap<String, List<String>> listDataChild;

    /**
     * Event raised when the form is created for the first time
     */
    //TODO: Fix Faq
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faq);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        // initializing the views
        initViews();

        // initializing the listeners
        initListeners();

        // initializing the objects
        initObjects();

        // preparing list data
        initListData();

    }


    /**
     * method to initialize the views
     */
    private void initViews() {
        // on activity FAX.XML
        expandableListView = findViewById(R.id.expandableListView);

    }

    /**
     * method to initialize the listeners
     */
    private void initListeners() {

        // ExpandableListView on child click listener
        expandableListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            Toast.makeText(
                    getApplicationContext(),
                    listDataTitle.get(groupPosition)
                            + " : "
                            + listDataChild.get(
                            listDataTitle.get(groupPosition)),
                    Toast.LENGTH_SHORT)
                    .show();
            return false;
        });

        // ExpandableListView Group expanded listener
        expandableListView.setOnGroupExpandListener(groupPosition -> Toast.makeText(getApplicationContext(),
                listDataTitle.get(groupPosition) + " " + getString(R.string.faq_text_collapsed),
                Toast.LENGTH_SHORT).show());

        // ExpandableListView Group collapsed listener
        expandableListView.setOnGroupCollapseListener(groupPosition -> Toast.makeText(getApplicationContext(),
                listDataTitle.get(groupPosition) + " " + getString(R.string.faq_text_collapsed),
                Toast.LENGTH_SHORT).show());

    }

    /**
     * method to initialize the objects
     */
    private void initObjects() {

        // initializing the list of groups
        listDataTitle = new ArrayList<>();

        // initializing the list of child
        listDataChild = new HashMap<>();

        // initializing the adapter object
        expandableListAdapter = new faqExpandableListViewAdapter(this, listDataTitle, listDataChild);

        // setting list adapter
        expandableListView.setAdapter(expandableListAdapter);

    }

    /*
     * Preparing the list data
     */
    private void initListData() {

        // Adding Text headers group data
        Collections.addAll(listDataTitle, getResources().getStringArray(R.array.faq_headers));

        // Adding child data
        // FAQ 01
        List<String> faq01 = new ArrayList<>();
        Collections.addAll(faq01, getResources().getStringArray(R.array.faq_01));

        // FAQ 02
        List<String> faq02 = new ArrayList<>();
        Collections.addAll(faq02, getResources().getStringArray(R.array.faq_02));
        // FAQ 03
        List<String> faq03 = new ArrayList<>();
        Collections.addAll(faq03, getResources().getStringArray(R.array.faq_03));
        // FAQ 04
        List<String> faq04 = new ArrayList<>();
        Collections.addAll(faq04, getResources().getStringArray(R.array.faq_04));
        // FAQ 05
        List<String> faq05 = new ArrayList<>();
        Collections.addAll(faq05, getResources().getStringArray(R.array.faq_05));

        listDataChild.put(listDataTitle.get(0), faq01);
        listDataChild.put(listDataTitle.get(1), faq02);
        listDataChild.put(listDataTitle.get(2), faq03);
        listDataChild.put(listDataTitle.get(3), faq04);
        listDataChild.put(listDataTitle.get(4), faq05);

        expandableListAdapter.notifyDataSetChanged();
    }


}


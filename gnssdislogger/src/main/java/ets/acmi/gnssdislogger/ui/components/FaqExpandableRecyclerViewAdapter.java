package ets.acmi.gnssdislogger.ui.components;


import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;

import java.util.List;

import ets.acmi.gnssdislogger.R;

public class FaqExpandableRecyclerViewAdapter extends RecyclerView.Adapter<FaqExpandableRecyclerViewAdapter.ViewHolder> {

    private final Context context;
    private final List<String> values;

    public FaqExpandableRecyclerViewAdapter(Context context, List<String> values) {
        this.context = context;
        this.values = values;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tv;
        final MaterialTextView expTv1;

        ViewHolder(@NonNull View view) {
            super(view);
            this.expTv1 = view
                    .findViewById(R.id.expandable_text);

            this.tv = expTv1.findViewById(R.id.expandable_text)
                    .findViewById(R.id.expand_text_view);
        }
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(
                LayoutInflater
                        .from(context)
                        .inflate(R.layout.activity_faq_list_item, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.expTv1.setText(HtmlCompat.fromHtml(values.get(position), HtmlCompat.FROM_HTML_MODE_LEGACY));
        holder.tv.setMovementMethod(LinkMovementMethod.getInstance());
        holder.tv.setTag(String.valueOf(position));
    }

    @Override
    public int getItemCount() {
        return this.values.size();
    }

}
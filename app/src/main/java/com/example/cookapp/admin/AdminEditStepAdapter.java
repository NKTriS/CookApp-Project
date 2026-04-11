package com.example.cookapp.admin;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookapp.R;
import com.example.cookapp.api.dto.RecipeStepDto;

import java.util.List;

public class AdminEditStepAdapter extends RecyclerView.Adapter<AdminEditStepAdapter.VH> {

    private List<RecipeStepDto> items;

    public void setItems(List<RecipeStepDto> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public List<RecipeStepDto> getItems() {
        return items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_edit_step, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        RecipeStepDto step = items.get(pos);
        h.tvStepNumber.setText("Bước " + step.step_number);
        h.tvInstruction.setText(step.instruction);

        h.etTimeSeconds.setText(String.valueOf(step.video_start_time));

        // Remove old watcher if exists to prevent messy updates during recycling
        if (h.textWatcher != null) {
            h.etTimeSeconds.removeTextChangedListener(h.textWatcher);
        }

        h.textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    if (s.toString().trim().isEmpty()) {
                        step.video_start_time = 0;
                    } else {
                        step.video_start_time = Integer.parseInt(s.toString().trim());
                    }
                } catch (NumberFormatException e) {
                    step.video_start_time = 0;
                }
            }
        };

        h.etTimeSeconds.addTextChangedListener(h.textWatcher);
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvStepNumber, tvInstruction;
        EditText etTimeSeconds;
        TextWatcher textWatcher;

        VH(View v) {
            super(v);
            tvStepNumber = v.findViewById(R.id.tv_step_number);
            tvInstruction = v.findViewById(R.id.tv_instruction);
            etTimeSeconds = v.findViewById(R.id.et_time_seconds);
        }
    }
}

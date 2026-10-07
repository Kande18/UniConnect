package com.uniconnect.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

/**
 * StudentRosterAdapter  (§14.3)
 * ==============================
 * RecyclerView adapter that binds student data to item_student_card.xml rows.
 *
 * Integration note for the Backend team:
 *   Replace StudentItem with your Room Student entity.
 *   Call setStudents(list) after observing ViewModel LiveData.
 */
public class StudentRosterAdapter
        extends RecyclerView.Adapter<StudentRosterAdapter.StudentViewHolder> {

    /** Callback for student card tap events. */
    public interface OnStudentClickListener {
        void onStudentClick(LecturerRosterActivity.StudentItem student);
    }

    private final OnStudentClickListener clickListener;
    private List<LecturerRosterActivity.StudentItem> students = new ArrayList<>();

    public StudentRosterAdapter(OnStudentClickListener listener) {
        this.clickListener = listener;
    }

    /** Replace the full list and refresh the RecyclerView. */
    public void setStudents(List<LecturerRosterActivity.StudentItem> list) {
        this.students = new ArrayList<>(list);
        notifyDataSetChanged();
        // TODO: Use DiffUtil for efficient updates when real data is available
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_card, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        LecturerRosterActivity.StudentItem student = students.get(position);
        holder.bind(student, clickListener);
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    // ─── ViewHolder ──────────────────────────────────────────────────────
    static class StudentViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvAvatar;
        private final TextView tvStudentName;
        private final TextView tvStudentNumber;
        private final Chip chipProgramme;
        private final Chip chipGroup;

        StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar        = itemView.findViewById(R.id.tvAvatar);
            tvStudentName   = itemView.findViewById(R.id.tvStudentName);
            tvStudentNumber = itemView.findViewById(R.id.tvStudentNumber);
            chipProgramme   = itemView.findViewById(R.id.chipProgramme);
            chipGroup       = itemView.findViewById(R.id.chipGroup);
        }

        void bind(LecturerRosterActivity.StudentItem student,
                  OnStudentClickListener listener) {

            // Avatar — first letter of name
            String initial = student.name.isEmpty()
                    ? "?" : String.valueOf(student.name.charAt(0)).toUpperCase();
            tvAvatar.setText(initial);

            tvStudentName.setText(student.name);
            tvStudentNumber.setText(student.studentNumber);
            chipProgramme.setText(student.programme);

            // Group chip — colour it differently if unassigned
            chipGroup.setText(student.group);
            if ("Unassigned".equalsIgnoreCase(student.group)) {
                chipGroup.setChipBackgroundColorResource(R.color.status_warning);
            } else {
                chipGroup.setChipBackgroundColorResource(R.color.action_teal);
            }

            // Accessibility description
            itemView.setContentDescription(
                    itemView.getContext().getString(
                            R.string.desc_student_card, student.name)
            );

            itemView.setOnClickListener(v -> listener.onStudentClick(student));
        }
    }
}

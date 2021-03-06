package com.example.hppc.edified;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class EnrolledCourseFragment extends Fragment implements FireBaseConn {

    private static final String TAG = "EnrolledCourseFragment";
    FirebaseUser user = mAuth.getCurrentUser();
    private RecyclerView enrolledCourseRecView;
    private LinearLayoutManager enrolledCourseLayoutManager;
    private FirebaseRecyclerAdapter<Course, CourseHolder> enrolledCourseAdapter;
    private ArrayList<Course> enrolledCourseList = new ArrayList<>();
    private Course crs;
    private User usr;
    private ProgressDialog progress;

    public EnrolledCourseFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_enrolled_course, container, false);
        enrolledCourseRecView = (RecyclerView) view.findViewById(R.id.enrolledCourseRecView);
        enrolledCourseLayoutManager = new LinearLayoutManager(getActivity());
        enrolledCourseRecView.setLayoutManager(enrolledCourseLayoutManager);

        progress = new ProgressDialog(view.getContext());
        progress.setMessage("Loading...");
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.show();

        mDatabase.child("users").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                usr = dataSnapshot.getValue(User.class);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        enrolledCourseAdapter = new FirebaseRecyclerAdapter<Course, CourseHolder>(
                Course.class,
                R.layout.course_card,
                CourseHolder.class,
                mDatabase.child(getString(R.string.USERS)).child(user.getUid()).child("enrolledCourses")) {

            @Override
            protected Course parseSnapshot(DataSnapshot snapshot) {
                Course course = super.parseSnapshot(snapshot);
                progress.dismiss();
                if (course != null) {
                    course.setCourseID(snapshot.getKey());
                    enrolledCourseList.add(course);
                }
                return course;
            }

            @Override
            protected void populateViewHolder(CourseHolder viewHolder, Course model, int position) {
                progress.dismiss();
                viewHolder.getCourse_name().setText(model.getCourseName());
                viewHolder.getCourse_category().setText(model.getCourseCategory());
                viewHolder.getEnroll().setVisibility(View.GONE);
            }

            @Override
            public CourseHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                final CourseHolder courseHolder = super.onCreateViewHolder(parent, viewType);
                courseHolder.setOnClickListener(new CourseHolder.ClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        crs = enrolledCourseList.get(position);
                        Intent intent = new Intent(getContext(), CourseDescription.class);
                        intent.putExtra("Course", (Parcelable) crs);
                        intent.putExtra("User", (Parcelable) usr);
                        startActivity(intent);
                    }

                    @Override
                    public void onEnrollClick(View view, int position) {

                    }
                });
                return courseHolder;
            }
        };
        enrolledCourseRecView.setAdapter(enrolledCourseAdapter);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}

package com.codingwithmitch.audiostreamer.ui;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.codingwithmitch.audiostreamer.R;
import com.codingwithmitch.audiostreamer.adapters.CategoryRecyclerAdapter;
import com.codingwithmitch.audiostreamer.adapters.HomeRecyclerAdapter;
import com.codingwithmitch.audiostreamer.models.Artist;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;


public class CategoryFragment extends Fragment implements CategoryRecyclerAdapter.ICategorySelector
{

    private static final String TAG = "CategoryFragment";


    // UI Components
    private RecyclerView mRecyclerView;

    // Vars
    private CategoryRecyclerAdapter mAdapter;
    private ArrayList<Artist> mArtists = new ArrayList<>();
    private IMainActivity mIMainActivity;
    private String mSelectedCategory;

    public static CategoryFragment newInstance(String category){
        CategoryFragment categoryFragment = new CategoryFragment();
        Bundle args = new Bundle();
        args.putString("category", category);
        categoryFragment.setArguments(args);
        return categoryFragment;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if(!hidden){
            mIMainActivity.setActionBarTitle(mSelectedCategory);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            mSelectedCategory = getArguments().getString("category");
        }
		setRetainInstance(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initRecyclerView(view);
        mIMainActivity.setActionBarTitle(mSelectedCategory);
    }

    private void retrieveArtists(){
        mIMainActivity.showPrgressBar();

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        Query query = firestore
                .collection(getString(R.string.collection_audio))
                .document(getString(R.string.document_categories))
                .collection(mSelectedCategory);

        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    for(QueryDocumentSnapshot document: task.getResult()){
                        mArtists.add(document.toObject(Artist.class));
                    }
                }
                else{
                    Log.d(TAG, "onComplete: error getting documents: " + task.getException());
                }
                updateDataSet();
            }
        });
    }

    private void updateDataSet(){
        mIMainActivity.hideProgressBar();
        mAdapter.notifyDataSetChanged();
    }

    private void initRecyclerView(View view){
        mRecyclerView = view.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new CategoryRecyclerAdapter(getActivity(), mArtists, this);
        mRecyclerView.setAdapter(mAdapter);

        if(mArtists.size() == 0){
            retrieveArtists();
        }
    }

    @Override
    public void onArtistSelected(int position) {
        mIMainActivity.onArtistSelected(mSelectedCategory, mArtists.get(position));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mIMainActivity = (IMainActivity) getActivity();
    }
}
















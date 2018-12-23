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
import com.codingwithmitch.audiostreamer.adapters.HomeRecyclerAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;


public class HomeFragment extends Fragment implements HomeRecyclerAdapter.IHomeSelector
{

    private static final String TAG = "HomeFragment";


    // UI Components
    private RecyclerView mRecyclerView;

    // Vars
    private HomeRecyclerAdapter mAdapter;
    private ArrayList<String> mCategories = new ArrayList<>();
    private IMainActivity mIMainActivity;

    public static HomeFragment newInstance(){
        return new HomeFragment();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if(!hidden){
            mIMainActivity.setActionBarTitle(getString(R.string.categories));
        }
    }
	
	@Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        mIMainActivity.setActionBarTitle(getString(R.string.categories));
    }

    private void retrieveCategories(){
        mIMainActivity.showPrgressBar();

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        DocumentReference ref = firestore
                .collection(getString(R.string.collection_audio))
                .document(getString(R.string.document_categories));

        ref.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    DocumentSnapshot doc = task.getResult();
                    Log.d(TAG, "onComplete: " + doc);
                    HashMap<String, String> categoriesMap = (HashMap)doc.getData().get("categories");
                    mCategories.addAll(categoriesMap.keySet());
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
        mAdapter = new HomeRecyclerAdapter(mCategories, getActivity(),  this);
        mRecyclerView.setAdapter(mAdapter);

        if(mCategories.size() == 0){
            retrieveCategories();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mIMainActivity = (IMainActivity) getActivity();
    }

    @Override
    public void onCategorySelected(int postion) {
        Log.d(TAG, "onCategorySelected: list item is clicked!");
        mIMainActivity.onCategorySelected(mCategories.get(postion));
    }


}
















package com.codingwithmitch.audiostreamer.ui;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.codingwithmitch.audiostreamer.R;
import com.codingwithmitch.audiostreamer.adapters.PlaylistRecyclerAdapter;
import com.codingwithmitch.audiostreamer.models.Artist;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;


public class PlaylistFragment extends Fragment implements
        PlaylistRecyclerAdapter.IMediaSelector
{

    private static final String TAG = "PlaylistFragment";

    // UI Components
    private RecyclerView mRecyclerView;


    // Vars
    private PlaylistRecyclerAdapter mAdapter;
    private ArrayList<MediaMetadataCompat> mMediaList = new ArrayList<>();
    private IMainActivity mIMainActivity;
    public String mSelectedCategory;
    public Artist mSelectedArtist;
    public MediaMetadataCompat mSelectedMedia;


    public static PlaylistFragment newInstance(String category, Artist artist) {
        PlaylistFragment playlistFragment = new PlaylistFragment();
        Bundle args = new Bundle();
        args.putString("category", category);
        args.putParcelable("artist", artist);
        playlistFragment.setArguments(args);
        return playlistFragment;
    }


    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden){
            mIMainActivity.setActionBarTitle(mSelectedArtist.getTitle());
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            mSelectedCategory = getArguments().getString("category");
            mSelectedArtist = getArguments().getParcelable("artist");
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
        mIMainActivity.setActionBarTitle(mSelectedArtist.getTitle());
    }

    public void retrieveMedia(){
        Log.d(TAG, "retrieveMedia: called.");
        mIMainActivity.showProgressBar();

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        Query query  = firestore
                .collection(getString(R.string.collection_audio))
                .document(getString(R.string.document_categories))
                .collection(mSelectedCategory)
                .document(mSelectedArtist.getArtist_id())
                .collection(getString(R.string.collection_content))
                .orderBy(getString(R.string.field_date_added), Query.Direction.ASCENDING);

        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        addToMediaList(document);
                    }
                } else {
                    Log.d(TAG, "Error getting documents: ", task.getException());
                }
                updateDataSet();
            }
        });

    }

    /**
     * Translate the Firestore data into something the MediaBrowserService can deal with (MediaMetaDataCompat objects)
     * @param document
     */
    private void addToMediaList(QueryDocumentSnapshot document){

        MediaMetadataCompat media = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, document.getString(getString(R.string.field_media_id)))
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, document.getString(getString(R.string.field_artist)))
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, document.getString(getString(R.string.field_title)))
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, document.getString(getString(R.string.field_media_url)))
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, document.getString(getString(R.string.field_description)))
                .putString(MediaMetadataCompat.METADATA_KEY_DATE, document.getDate(getString(R.string.field_date_added)).toString())
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, mSelectedArtist.getImage())
                .build();

//        Log.d(TAG, "addToMediaList: MediaMetaData: " + media.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID));
//        Log.d(TAG, "addToMediaList: MediaMetaData: " + media.getString(MediaMetadataCompat.METADATA_KEY_DATE));
//        Log.d(TAG, "addToMediaList: MediaMetaData: " + media.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
//        Log.d(TAG, "addToMediaList: MediaMetaData: " + media.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
//        Log.d(TAG, "addToMediaList: MediaMetaData: " + media.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI));
//        Log.d(TAG, "addToMediaList: MediaMetaData: " + media.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION));
//        Log.d(TAG, "addToMediaList: MediaMetaData: " + media.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI));
        mMediaList.add(media);
    }

    private void initRecyclerView(View view){
        mRecyclerView = view.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new PlaylistRecyclerAdapter(getActivity(), mMediaList, this);
        mRecyclerView.setAdapter(mAdapter);

        if(mMediaList.size() == 0){
            retrieveMedia();
        }
    }

    private void updateDataSet(){
        mAdapter.notifyDataSetChanged();
        mIMainActivity.hideProgressBar();
    }

    @Override
    public void onMediaSelected(int position) {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mIMainActivity = (IMainActivity) getActivity();
    }
}
















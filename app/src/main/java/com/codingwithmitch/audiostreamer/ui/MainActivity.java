package com.codingwithmitch.audiostreamer.ui;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.codingwithmitch.audiostreamer.MyApplication;
import com.codingwithmitch.audiostreamer.R;
import com.codingwithmitch.audiostreamer.client.MediaBrowserHelper;
import com.codingwithmitch.audiostreamer.client.MediaBrowserHelperCallback;
import com.codingwithmitch.audiostreamer.models.Artist;
import com.codingwithmitch.audiostreamer.services.MediaService;
import com.codingwithmitch.audiostreamer.util.MainActivityFragmentManager;
import com.codingwithmitch.audiostreamer.util.MyPreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import static com.codingwithmitch.audiostreamer.util.Constants.MEDIA_QUEUE_POSITION;
import static com.codingwithmitch.audiostreamer.util.Constants.QUEUE_NEW_PLAYLIST;
import static com.codingwithmitch.audiostreamer.util.Constants.SEEK_BAR_MAX;
import static com.codingwithmitch.audiostreamer.util.Constants.SEEK_BAR_PROGRESS;


public class MainActivity extends AppCompatActivity implements
        IMainActivity,
        MediaBrowserHelperCallback
{

    private static final String TAG = "MainActivity";

    //UI Components
    private ProgressBar mProgressBar;

    // Vars
    private MediaBrowserHelper mMediaBrowserHelper;
    private MyApplication mMyApplication;
    private MyPreferenceManager mMyPrefManager;
    private boolean mIsPlaying;
    private SeekBarBroadcastReceiver mSeekbarBroadcastReceiver;
    private UpdateUIBroadcastReceiver mUpdateUIBroadcastReceiver;
    private boolean mOnAppOpen;
    private boolean mWasConfigurationChange = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mProgressBar = findViewById(R.id.progress_bar);

        mMyApplication = MyApplication.getInstance();
        mMyPrefManager = new MyPreferenceManager(this);

        mMediaBrowserHelper = new MediaBrowserHelper(this, MediaService.class);
        mMediaBrowserHelper.setMediaBrowserHelperCallback(this);

        if(savedInstanceState == null){
            loadFragment(HomeFragment.newInstance(), true);
        }
    }

    private class UpdateUIBroadcastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String newMediaId = intent.getStringExtra(getString(R.string.broadcast_new_media_id));
            Log.d(TAG, "onReceive: CALLED: " + newMediaId);
            if(getPlaylistFragment() != null){
                Log.d(TAG, "onReceive: " + mMyApplication.getMediaItem(newMediaId).getDescription().getMediaId());
                getPlaylistFragment().updateUI(mMyApplication.getMediaItem(newMediaId));
            }
        }
    }

    private void initUpdateUIBroadcastReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(getString(R.string.broadcast_update_ui));
        mUpdateUIBroadcastReceiver = new UpdateUIBroadcastReceiver();
        registerReceiver(mUpdateUIBroadcastReceiver, intentFilter);
    }

    @Override
    public void onMediaControllerConnected(MediaControllerCompat mediaController) {
        getMediaControllerFragment().getMediaSeekBar().setMediaController(mediaController);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initSeekBarBroadcastReceiver();
        initUpdateUIBroadcastReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mSeekbarBroadcastReceiver != null){
            unregisterReceiver(mSeekbarBroadcastReceiver);
        }
        if(mUpdateUIBroadcastReceiver != null){
            unregisterReceiver(mUpdateUIBroadcastReceiver);
        }
    }

    private class SeekBarBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            long seekProgress = intent.getLongExtra(SEEK_BAR_PROGRESS, 0);
            long seekMax = intent.getLongExtra(SEEK_BAR_MAX, 0);
            if(!getMediaControllerFragment().getMediaSeekBar().isTracking()){
                getMediaControllerFragment().getMediaSeekBar().setProgress((int)seekProgress);
                getMediaControllerFragment().getMediaSeekBar().setMax((int)seekMax);
            }
        }
    }

    private void initSeekBarBroadcastReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(getString(R.string.broadcast_seekbar_update));
        mSeekbarBroadcastReceiver = new SeekBarBroadcastReceiver();
        registerReceiver(mSeekbarBroadcastReceiver, intentFilter);
    }


    @Override
    public void onMetadataChanged(MediaMetadataCompat metadata) {
        Log.d(TAG, "onMetadataChanged: called");
        if(metadata == null){
            return;
        }

        // Do stuff with new Metadata
        getMediaControllerFragment().setMediaTitle(metadata);
    }

    @Override
    public void onPlaybackStateChanged(PlaybackStateCompat state) {
        Log.d(TAG, "onPlaybackStateChanged: called.");
        mIsPlaying = state != null &&
                state.getState() == PlaybackStateCompat.STATE_PLAYING;

        // update UI
        if(getMediaControllerFragment() != null){
            getMediaControllerFragment().setIsPlaying(mIsPlaying);
        }
    }

    private PlaylistFragment getPlaylistFragment(){
        PlaylistFragment playlistFragment = (PlaylistFragment)getSupportFragmentManager()
                .findFragmentByTag(getString(R.string.fragment_playlist));
        if(playlistFragment != null){
            return playlistFragment;
        }
        return null;
    }

    private MediaControllerFragment getMediaControllerFragment(){
        MediaControllerFragment mediaControllerFragment = (MediaControllerFragment)getSupportFragmentManager()
                .findFragmentById(R.id.bottom_media_controller);
        if(mediaControllerFragment != null){
            return mediaControllerFragment;
        }
        return null;
    }

    @Override
    public void onMediaSelected(String playlistId, MediaMetadataCompat mediaItem, int queuePosition) {
        if(mediaItem != null){
            Log.d(TAG, "onMediaSelected: CALLED: " + mediaItem.getDescription().getMediaId());

            String currentPlaylistId = getMyPreferenceManager().getPlaylistId();

            Bundle bundle = new Bundle();
            bundle.putInt(MEDIA_QUEUE_POSITION, queuePosition);
            if(playlistId.equals(currentPlaylistId)){
                mMediaBrowserHelper.getTransportControls().playFromMediaId(mediaItem.getDescription().getMediaId(), bundle);
            }
            else{
                bundle.putBoolean(QUEUE_NEW_PLAYLIST, true); // let the player know this is a new playlist
                mMediaBrowserHelper.subscribeToNewPlaylist(currentPlaylistId, playlistId);
                mMediaBrowserHelper.getTransportControls().playFromMediaId(mediaItem.getDescription().getMediaId(), bundle);
            }

            mOnAppOpen = true;
        }
        else{
            Toast.makeText(this, "select something to play", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public MyPreferenceManager getMyPreferenceManager() {
        return mMyPrefManager;
    }

    @Override
    public MyApplication getMyApplicationInstance() {
        return mMyApplication;
    }


    @Override
    public void playPause() {
        if(mOnAppOpen){
            if (mIsPlaying) {
                mMediaBrowserHelper.getTransportControls().pause();
            }
            else {
                mMediaBrowserHelper.getTransportControls().play();
            }
        }
        else{
            if(!getMyPreferenceManager().getPlaylistId().equals("")){
                onMediaSelected(
                        getMyPreferenceManager().getPlaylistId(),
                        mMyApplication.getMediaItem(getMyPreferenceManager().getLastPlayedMedia()),
                        getMyPreferenceManager().getQueuePosition()
                );
            }
            else{
                Toast.makeText(this, "select something to play", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mWasConfigurationChange = true;
    }

    @Override
    public void onStart() {
        super.onStart();

        if(!getMyPreferenceManager().getPlaylistId().equals("")){
            prepareLastPlayedMedia();
        }
        else{
            mMediaBrowserHelper.onStart(mWasConfigurationChange);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: called.");
        mMediaBrowserHelper.onStop();
        getMediaControllerFragment().getMediaSeekBar().disconnectController();
    }

    /**
     * In a production app you'd want to get this data from a cache.
     */
    private void prepareLastPlayedMedia(){
        showPrgressBar();

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        Query query  = firestore
                .collection(getString(R.string.collection_audio))
                .document(getString(R.string.document_categories))
                .collection(getMyPreferenceManager().getLastCategory())
                .document(getMyPreferenceManager().getLastPlayedArtist())
                .collection(getString(R.string.collection_content))
                .orderBy(getString(R.string.field_date_added), Query.Direction.ASCENDING);

        final List<MediaMetadataCompat> mediaItems = new ArrayList<>();
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        MediaMetadataCompat mediaItem = addToMediaList(document);
                        mediaItems.add(mediaItem);
                        if(mediaItem.getDescription().getMediaId().equals(getMyPreferenceManager().getLastPlayedMedia())){
                            getMediaControllerFragment().setMediaTitle(mediaItem);
                        }
                    }
                } else {
                    Log.d(TAG, "Error getting documents: ", task.getException());
                }
                onFinishedGettingPreviousSessionData(mediaItems);
            }
        });
    }

    private void onFinishedGettingPreviousSessionData(List<MediaMetadataCompat> mediaItems){
        mMyApplication.setMediaItems(mediaItems);
        mMediaBrowserHelper.onStart(mWasConfigurationChange);
        hideProgressBar();
    }

    /**
     * Translate the Firestore data into something the MediaBrowserService can deal with (MediaMetaDataCompat objects)
     * @param document
     */
    private MediaMetadataCompat addToMediaList(QueryDocumentSnapshot document){

        MediaMetadataCompat media = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, document.getString(getString(R.string.field_media_id)))
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, document.getString(getString(R.string.field_artist)))
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, document.getString(getString(R.string.field_title)))
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, document.getString(getString(R.string.field_media_url)))
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, document.getString(getString(R.string.field_description)))
                .putString(MediaMetadataCompat.METADATA_KEY_DATE, document.getDate(getString(R.string.field_date_added)).toString())
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, getMyPreferenceManager().getLastPlayedArtistImage())
                .build();

        return media;
    }

    private void loadFragment(Fragment fragment, boolean lateralMovement){

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if(lateralMovement){
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left);
        }

        String tag = "";
        if(fragment instanceof HomeFragment){
            tag = getString(R.string.fragment_home);
        }
        else if(fragment instanceof CategoryFragment){
            tag = getString(R.string.fragment_category);
            transaction.addToBackStack(tag);
        }
        else if(fragment instanceof PlaylistFragment){
            tag = getString(R.string.fragment_playlist);
            transaction.addToBackStack(tag);
        }

        transaction.add(R.id.main_container, fragment, tag);
        transaction.commit();

        MainActivityFragmentManager.getInstance().addFragment(fragment);

        showFragment(fragment, false);
    }


    private void showFragment(Fragment fragment, boolean backswardsMovement){

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if(backswardsMovement){
            transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
        }

        transaction.show(fragment);
        transaction.commit();

        for(Fragment f: MainActivityFragmentManager.getInstance().getFragments()){
            if(f != null ){
                if(!f.getTag().equals(fragment.getTag())){
                    FragmentTransaction t = getSupportFragmentManager().beginTransaction();
                    t.hide(f);
                    t.commit();
                }
            }
        }


        Log.d(TAG, "showFragment: num fragments: " + MainActivityFragmentManager.getInstance().getFragments().size());

    }

    @Override
    public void onBackPressed() {
        ArrayList<Fragment> fragments = new ArrayList<>(MainActivityFragmentManager.getInstance().getFragments());

        if(fragments.size() > 1){
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.remove(fragments.get(fragments.size() - 1));
            transaction.commit();

            MainActivityFragmentManager.getInstance().removeFragment(fragments.size() - 1);
            showFragment(fragments.get(fragments.size() - 2), true);
        }

        super.onBackPressed();
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("active_fragments", MainActivityFragmentManager.getInstance().getFragments().size());
    }


    @Override
    public void hideProgressBar() {
        mProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void showPrgressBar() {
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCategorySelected(String category) {
        loadFragment(CategoryFragment.newInstance(category), true);
    }

    @Override
    public void onArtistSelected(String category, Artist artist) {
        loadFragment(PlaylistFragment.newInstance(category, artist), true);
    }

    @Override
    public void setActionBarTitle(String title) {
        getSupportActionBar().setTitle(title);
    }

}



















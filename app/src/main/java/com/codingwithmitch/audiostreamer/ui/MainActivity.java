package com.codingwithmitch.audiostreamer.ui;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
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

import static com.codingwithmitch.audiostreamer.util.MyPreferenceManager.MEDIA_QUEUE_POSITION;
import static com.codingwithmitch.audiostreamer.util.MyPreferenceManager.QUEUE_NEW_PLAYLIST;
import static com.codingwithmitch.audiostreamer.util.MyPreferenceManager.SEEK_BAR_MAX;
import static com.codingwithmitch.audiostreamer.util.MyPreferenceManager.SEEK_BAR_PROGRESS;


public class MainActivity extends AppCompatActivity implements
        IMainActivity
{

    private static final String TAG = "MainActivity";

    //UI Components
    private ProgressBar mProgressBar;

    // Vars
    private MediaBrowserHelper mMediaBrowserHelper;
    private boolean mIsPlaying;
    private MyPreferenceManager mPreferenceManager;
    private SeekBarBroadcastReceiver mSeekbarBroadcastReceiver;
    private UpdateUIBroadcastReceiver mUpdateUIBroadcastReceiver;
    private MyApplication mMyApplication;
    private Boolean mOnAppOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mProgressBar = findViewById(R.id.progress_bar);

        mMyApplication = MyApplication.getInstance();
        mPreferenceManager = new MyPreferenceManager(this);

        mMediaBrowserHelper = new MediaBrowserConnection(this);
        mMediaBrowserHelper.registerCallback(new MediaBrowserListener());

        if(savedInstanceState == null){
            loadFragment(HomeFragment.newInstance(), false);
        }
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
            onMediaSelected(
                    mPreferenceManager.getPlaylistId(),
                    mMyApplication.getMediaItem(mPreferenceManager.getLastPlayedMedia()),
                    mPreferenceManager.getQueuePosition()
            );
        }
    }


    @Override
    public void onStart() {
        super.onStart();

        if(!mPreferenceManager.getPlaylistId().equals("")){
            prepareLastPlayedMedia();
        }
        else{
            mMediaBrowserHelper.onStart("");
        }
    }

    /**
     * In a production app you'd want to get this data from a cache.
     */
    private void prepareLastPlayedMedia(){
        showProgressBar();

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        Query query  = firestore
                .collection(getString(R.string.collection_audio))
                .document(getString(R.string.document_categories))
                .collection(mPreferenceManager.getLastCategory())
                .document(mPreferenceManager.getLastPlayedArtist())
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
                        if(mediaItem.getDescription().getMediaId().equals(mPreferenceManager.getLastPlayedMedia())){
                            getMediaControllerFragment().setMediaItem(mediaItem);
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
        mMediaBrowserHelper.onStart(mPreferenceManager.getPlaylistId());
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
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, mPreferenceManager.getLastPlayedArtistImage())
                .build();

        return media;
    }


    @Override
    public void onStop() {
        super.onStop();
        getMediaControllerFragment().getMediaSeekBar().disconnectController();
        mMediaBrowserHelper.onStop();
    }


    /**
     * Customize the connection to our {@link android.support.v4.media.MediaBrowserServiceCompat}
     * and implement our app specific desires.
     */
    private class MediaBrowserConnection extends MediaBrowserHelper {
        private MediaBrowserConnection(Context context) {
            super(context, MediaService.class);
        }

        @Override
        protected void onConnected(@NonNull MediaControllerCompat mediaController) {
            getMediaControllerFragment().getMediaSeekBar().setMediaController(mediaController);
        }

        @Override
        protected void onChildrenLoaded(@NonNull String parentId,
                                        @NonNull List<MediaBrowserCompat.MediaItem> children) {
            super.onChildrenLoaded(parentId, children);
            Log.d(TAG, "onChildrenLoaded: CALLED. adding media to queue.");

            final MediaControllerCompat mediaController = getMediaController();

            // Queue up all media items
            for (final MediaBrowserCompat.MediaItem mediaItem : children) {
                Log.d(TAG, "onChildrenLoaded: CALLED: queue item: " + mediaItem.getMediaId());
                mediaController.addQueueItem(mediaItem.getDescription());
            }
        }
    }

    private MediaControllerFragment getMediaControllerFragment(){
        MediaControllerFragment mediaControllerFragment = (MediaControllerFragment)getSupportFragmentManager()
                .findFragmentById(R.id.bottom_media_controller);
        if(mediaControllerFragment != null){
            return mediaControllerFragment;
        }
        return null;
    }

    private PlaylistFragment getPlaylistFragment(){
        PlaylistFragment playlistFragment = (PlaylistFragment)getSupportFragmentManager()
                .findFragmentByTag(getString(R.string.fragment_playlist));
        if(playlistFragment != null){
            return playlistFragment;
        }
        return null;
    }

    /**
     * Implementation of the {@link MediaControllerCompat.Callback} methods we're interested in.
     * <p>
     * Here would also be where one could override
     * {@code onQueueChanged(List<MediaSessionCompat.QueueItem> queue)} to get informed when items
     * are added or removed from the queue. We don't do this here in order to keep the UI
     * simple.
     */
    private class MediaBrowserListener extends MediaControllerCompat.Callback {

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat playbackState) {
            mIsPlaying = playbackState != null &&
                    playbackState.getState() == PlaybackStateCompat.STATE_PLAYING;
            if(getMediaControllerFragment() != null){
                getMediaControllerFragment().setIsPlaying(mIsPlaying);
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat mediaMetadata) {
            if (mediaMetadata == null) {
                return;
            }

            getMediaControllerFragment().setMediaItem(mediaMetadata);

        }

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
        }

        @Override
        public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
            super.onQueueChanged(queue);
            // Add new items to the queue? (aka playlist)
            Log.d(TAG, "onQueueChanged: CALLED.");

        }
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

    private void showFragment(Fragment fragment, boolean backwardsMovement){
        // Show selected fragment
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if(backwardsMovement){
            transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
        }
        transaction.show(fragment);
        transaction.commit();

        for(Fragment f: MainActivityFragmentManager.getInstance().getFragments()){
            if(f != null){
                if(!f.getTag().equals(fragment.getTag())){
                    FragmentTransaction t = getSupportFragmentManager().beginTransaction();
                    t.hide(f);
                    t.commit();
                }
            }
        }
    }


    @Override
    public void onBackPressed() {
        ArrayList<Fragment> fragments = new ArrayList<>(MainActivityFragmentManager.getInstance().getFragments());
        if(fragments.size() > 1){
            showFragment(fragments.get(fragments.size() - 2), true);
            MainActivityFragmentManager.getInstance().removeFragment(fragments.size() - 1);
        }
        super.onBackPressed();
    }


    @Override
    public void onMediaSelected(String newPlaylistId, MediaMetadataCompat mediaItem, int queuePosition) {
        if(mediaItem != null){
            Log.d(TAG, "onMediaSelected: CALLED: " + mediaItem.getDescription().getMediaId());

            String currentPlaylistId = mPreferenceManager.getPlaylistId();
            Log.d(TAG, "onMediaSelected: called: current playlist id: " + currentPlaylistId);
            Bundle bundle = new Bundle();
            bundle.putInt(MEDIA_QUEUE_POSITION, queuePosition);
            if(newPlaylistId.equals(currentPlaylistId)){
                mMediaBrowserHelper.getTransportControls().playFromMediaId(mediaItem.getDescription().getMediaId(), bundle);
            }
            else{
                bundle.putBoolean(QUEUE_NEW_PLAYLIST, true); // let the player know this is a new playlist
                mMediaBrowserHelper.subscribeToNewPlaylist(currentPlaylistId, newPlaylistId);
                mMediaBrowserHelper.getTransportControls().playFromMediaId(mediaItem.getDescription().getMediaId(), bundle);
            }
            mOnAppOpen = true;
        }
        else{
            Toast.makeText(this, "select something to play", Toast.LENGTH_SHORT).show();
        }
    }




    private class SeekBarBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
//            Log.d(TAG, "onReceive: " + getMediaControllerFragment().getMediaSeekBar().isTracking());
            long seekProgress = intent.getLongExtra(SEEK_BAR_PROGRESS, 0);
            long seekMax = intent.getLongExtra(SEEK_BAR_MAX, 0);
//            Log.d(TAG, "onReceive: " + seekProgress + " / " + seekMax);
            if(!getMediaControllerFragment().getMediaSeekBar().isTracking()){
                getMediaControllerFragment().getMediaSeekBar().setProgress((int)seekProgress);
                getMediaControllerFragment().getMediaSeekBar().setMax((int)seekMax);
//                Log.d(TAG, "onReceive: updating seekbar: " + seekProgress + ", max: " + seekMax);
            }
        }
    }

    private void initSeekBarBroadcastReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(getString(R.string.broadcast_seekbar_update));
        mSeekbarBroadcastReceiver = new SeekBarBroadcastReceiver();
        registerReceiver(mSeekbarBroadcastReceiver, intentFilter);
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
    protected void onDestroy() {
        super.onDestroy();
        MainActivityFragmentManager.getInstance().removeAllFragments();
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

    @Override
    protected void onResume() {
        super.onResume();
        initSeekBarBroadcastReceiver();
        initUpdateUIBroadcastReceiver();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("active_fragments", MainActivityFragmentManager.getInstance().getFragments().size());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        restoreFragmentView(savedInstanceState);
    }

    private void restoreFragmentView(Bundle bundle){
        int numFragments = bundle.getInt("active_fragments");
        if(numFragments > 0){
            HomeFragment homeFragment = (HomeFragment)getSupportFragmentManager()
                    .findFragmentByTag(getString(R.string.fragment_home));
            if(homeFragment != null){
                Log.d(TAG, "restoreFragmentView: HomeFragment is ALIVE");
                MainActivityFragmentManager.getInstance().addFragment(homeFragment);
            }
            CategoryFragment categoryFragment = (CategoryFragment)getSupportFragmentManager()
                    .findFragmentByTag(getString(R.string.fragment_category));
            if(categoryFragment != null){
                Log.d(TAG, "restoreFragmentView: CategoryFragment is ALIVE");
                MainActivityFragmentManager.getInstance().addFragment(categoryFragment);
            }
            PlaylistFragment playlistFragment = (PlaylistFragment)getSupportFragmentManager()
                    .findFragmentByTag(getString(R.string.fragment_playlist));
            if(playlistFragment != null){
                Log.d(TAG, "restoreFragmentView: PlaylistFragment is ALIVE");
                MainActivityFragmentManager.getInstance().addFragment(playlistFragment);
            }
        }
    }

    @Override
    public void showProgressBar(){
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgressBar(){
        mProgressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onCategorySelected(String category) {
        Log.d(TAG, "onCategorySelected: called.");
        loadFragment(CategoryFragment.newInstance(category), true);
    }

    @Override
    public void onArtistSelected(String category, Artist artist) {
        Log.d(TAG, "onArtistSelected: called.");
        loadFragment(PlaylistFragment.newInstance(category, artist), true);
    }

    @Override
    public MyApplication getMyApplicationInstance() {
        return mMyApplication;
    }

    @Override
    public MyPreferenceManager getMyPreferenceManager() {
        return mPreferenceManager;
    }

    @Override
    public void setActionBarTitle(String title) {
        getSupportActionBar().setTitle(title);
    }
}



















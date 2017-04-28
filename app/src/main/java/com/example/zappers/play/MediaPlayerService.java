package com.example.zappers.play;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.drm.DrmStore;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.StrictMode;
import android.support.v4.app.NotificationCompat.Style;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

import static android.content.ContentValues.TAG;

/**
 * Created by aman on 26/3/17.
 */

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener,MediaPlayer.OnPreparedListener,MediaPlayer.OnErrorListener,MediaPlayer.OnSeekCompleteListener,MediaPlayer.OnInfoListener,MediaPlayer.OnBufferingUpdateListener,AudioManager.OnAudioFocusChangeListener {

    private final IBinder iBinder = new LocalBinder();
    private AudioManager audioManager;
    private Boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;
    private ArrayList<Audio> audioList;
    private int audioIndex = -1;
    private Audio activeAudio; //an object of the currently playing audio
    public static final String ACTION_PLAY = "com.example.zappers.play.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.example.zappers.play.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "com.example.zappers.play.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "com.example.zappers.play.ACTION_NEXT";
    public static final String ACTION_STOP = "com.example.zappers.play.ACTIO_STOP";

    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;

    //audio player ntification id
    private static final int NOTIFICATION_ID = 101;






    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        //indicates buffering status of media being streamed over network
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        //invoked when playback is completed
        stopMedia();
        //stoppping service
        stopSelf();
    }

    //error handling functions
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        //inoked when error occurs during asynchronous play
        switch(what){
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error" , "MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK" + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayerError" , "MEDIA_ERROR_SERVER_DIED" + extra);
                break;
            case  MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayerError" , "MEDIA_ERROR_UNKNOWN" + extra);
        }

        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        //invoked when media player is ready for playback
        playMedia();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        //indiating completton of seek
    }

    @Override
    public void onAudioFocusChange(int focusState) {
        //invoked when audio focus is updated
        switch (focusState){
            case AudioManager.AUDIOFOCUS_GAIN:
                //resume the playback
                if(mediaPlayer == null)
                    initMediaPlayer();
                else if(!mediaPlayer.isPlaying())
                    mediaPlayer.start();
                mediaPlayer.setVolume(1.0f,1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                //lost focus for some time: stop playbak and release media player kudos
                if(mediaPlayer.isPlaying())
                    mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                //lost fous for some time, but we have to stop playback
                //we dont release media player coz playback is likely to resume
                if(mediaPlayer.isPlaying())
                    mediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                //lost focuus for short time, but its ok to keep playing
                if(mediaPlayer.isPlaying())
                    mediaPlayer.setVolume(0.1f , 0.1f);
                break;

        }
    }

    private boolean requestAudioFocus(){
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this , AudioManager.STREAM_MUSIC , AudioManager.AUDIOFOCUS_GAIN );
        if(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED ){
            //focus gained
            return true;
        }
        //could not gain focus
        return false;
    }

    private boolean removeAudioFocus(){
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.abandonAudioFocus(this);
    }

    //The system calls this method when an activity, requests the service be started
    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        try{
            StorageUtil storage = new StorageUtil(getApplicationContext());
            audioList = storage.loadAudio();
            audioIndex = storage.loadAudioIndex();

            if(audioIndex != 1 && audioIndex < audioList.size()){
                activeAudio = audioList.get(audioIndex);
            }
            else{
                stopSelf();
            }
        }catch (NullPointerException e){
            stopSelf();
        }

        if(requestAudioFocus() == false){
            stopSelf();
        }
        if(mediaSessionManager != null){
            try{
                initMediaSession();
                initMediaPlayer();
            }
            catch (RemoteException e){
                e.printStackTrace();
                stopSelf();
            }
            buildNotification(PlaybackStatus.PLAYING);
        }
        handleIncomingActions(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
        }
        removeAudioFocus();
        //Disable PhoneStateListener
        if(phoneStateListener != null){
            telephonyManager.listen(phoneStateListener, phoneStateListener.LISTEN_NONE);
        }
        removeNotification();

        //unregister BroacastReceivers
        unregisterReceiver(becomingNoisyReceiver);
        unregisterReceiver(playNewAudio);

        //clear cached playlist
        new StorageUtil(getApplicationContext()).clearCachedAudioPlaylist();
    }











    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    private  MediaPlayer mediaPlayer;
    private String mediaFile;

    private void initMediaPlayer(){
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        //resetting so it doesnt point to anything at start
        mediaPlayer.reset();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try
        {
            //set data source to media file
            mediaPlayer.setDataSource(activeAudio.getData());
        }
        catch(IOException e){
            e.printStackTrace();
            stopSelf();
        }
        mediaPlayer.prepareAsync();

    }
    private int resumePosition;

    private void playMedia(){
        if(!mediaPlayer.isPlaying()){
            mediaPlayer.start();
        }
    }

    private void stopMedia(){
        if(mediaPlayer == null)
            return;
        if(mediaPlayer.isPlaying()){
            mediaPlayer.stop();
        }

    }

    private void pauseMedia(){
        if(mediaPlayer.isPlaying()){
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
        }
    }

    private void resumeMedia(){
        if(!mediaPlayer.isPlaying()){
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
        }
    }



private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        //pausing audio on audio becoming noisy
        pauseMedia();
        buildNotification(PlaybackStatus.PAUSED);
    }
};

private void registerBecomingNoisyReceiver(){
    //register after getting audio focus
    IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    registerReceiver(becomingNoisyReceiver, intentFilter);
}

private void callStateListener(){
    //get telephone manager
    telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
    //start listening to phone state changes
    phoneStateListener = new PhoneStateListener(){
        @Override
        public void onCallStateChanged(int state, String incomingNumber){
            switch (state){
                //if at least one call comes or phone is ringing pause media
                case TelephonyManager.CALL_STATE_OFFHOOK:
                case TelephonyManager.CALL_STATE_RINGING:
                    if(mediaPlayer != null){
                        pauseMedia();
                        ongoingCall = true;
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    if(mediaPlayer != null)
                    {
                        if(ongoingCall){
                            ongoingCall = false;
                            resumeMedia();
                        }
                    }
                    break;

            }
        }
    };
    //register listener with telephony manager
    //listen for changes to device call state
    telephonyManager.listen(phoneStateListener, phoneStateListener.LISTEN_CALL_STATE);

}

private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        //get the new media from shared preferences
        audioIndex = new StorageUtil(getApplicationContext()).loadAudioIndex();
        if(audioIndex != -1 && audioIndex < audioList.size()) {
            activeAudio = audioList.get(audioIndex);
        }
        else{
            stopSelf();
        }
        //A PLAY_NEW_AUDIO action received
        //reset mediaPlayer to play new new Audio
        stopMedia();
        initMediaPlayer();
        mediaPlayer.reset();
        initMediaPlayer();
        updateMetaData();
        buildNotification(PlaybackStatus.PLAYING);

    }
};

    @Override
    public void onCreate() {
        super.onCreate();
        //manage incoming phone calls during playback
        //Pause MediaPlayer on incoming call
        //Resume on hangup
        callStateListener();
        //ACTION_AUDIO_BECOMING_NOISY
        registerBecomingNoisyReceiver();
        //Listen fr new Audio to play
        register_playNewAudio();
    }



    private void register_playNewAudio(){
    // Register playNewMedia receiver
    IntentFilter filter = new IntentFilter(MainActivity.Broadcast_PLAY_NEW_AUDIO);
    registerReceiver(playNewAudio, filter);
    }

    private void initMediaSession() throws RemoteException {
        if (mediaSessionManager != null) return;

        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        //create a new edia session
        mediaSession = new MediaSessionCompat(getApplicationContext(), "AudioPlayer");
        //ge tmedia session transport controls
        transportControls = mediaSession.getController().getTransportControls();
        //set MediaSession
        mediaSession.setActive(true);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        //set meta data
        updateMetaData();
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                resumeMedia();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onPause() {
                super.onPause();
                pauseMedia();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                skiptoNext();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                skipToPrevious();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onStop() {
                super.onStop();
                removeNotification();
                //stop the service
                stopSelf();
            }

            @Override
            public void onSeekTo(long pos) {
                super.onSeekTo(pos);
            }
        });
    }

    private void updateMetaData(){
        Bitmap albumArt = BitmapFactory.decodeResource(getResources(),R.drawable.image);
        //update current meta data
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, activeAudio.getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, activeAudio.getAlbum())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, activeAudio.getTitle())
                .build());
    }

    private void skipToPrevious(){
        if(audioIndex == 0){
            //if song is first in pallylist
            //set index to the last of audio list
            audioIndex = audioList.size() - 1;
            activeAudio = audioList.get(audioIndex);

        }
        else{
            activeAudio = audioList.get(--audioIndex);
        }
        //update stored index
        new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);
        stopMedia();
        //reset media player
        mediaPlayer.reset();
        initMediaPlayer();
    }

    private void skiptoNext(){
        if(audioIndex == audioList.size() - 1){
            //if song is last in playlist
            audioIndex = 0;
            activeAudio = audioList.get(audioIndex);

        }
        else{
            activeAudio = audioList.get(++audioIndex);
        }
        //update stored index
        new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);
        stopMedia();
        //reset media player
        mediaPlayer.reset();
        initMediaPlayer();
    }

    private void buildNotification(PlaybackStatus playbackStatus){
        int notificationAction = android.R.drawable.ic_media_pause;
        PendingIntent play_pauseAction = null;

        //build a new notif acc. to current state of MediaPlayer
        if(playbackStatus == PlaybackStatus.PLAYING) {
            notificationAction = android.R.drawable.ic_media_pause;
            //create pause action
            play_pauseAction = playbackAction(1);
        }
        else if(playbackStatus == PlaybackStatus.PAUSED){
            notificationAction = android.R.drawable.ic_media_play;
            //create play action
            play_pauseAction = playbackAction(0);
        }

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(),R.drawable.image);
        //create a new notification
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setShowWhen(false)
                //set notif style
        .setStyle(new NotificationCompat.MediaStyle()
                //attach our MediaSession token
                .setMediaSession(mediaSession.getSessionToken())
                //show our playback controls in compact notification
                .setShowActionsInCompactView(0, 1, 2))
                //set notif color
        .setColor(getResources().getColor(R.color.colorPrimary))
                //set large and small icons
        .setLargeIcon(largeIcon)
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                //set notif content info
        .setContentText(activeAudio.getArtist())
        .setContentTitle(activeAudio.getAlbum())
        .setContentInfo(activeAudio.getTitle())
        //add playback actions
        .addAction(android.R.drawable.ic_media_previous, "previous", playbackAction(3))
                .addAction(notificationAction, "pause", play_pauseAction)
                .addAction(android.R.drawable.ic_media_next, "next", playbackAction(2));
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notificationBuilder.build());

    }

    private void removeNotification(){
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private PendingIntent playbackAction(int actionNumber){
        Intent playbackAction = new Intent(this, MediaPlayerService.class);
        switch (actionNumber){
            case 0:
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 2:
                playbackAction.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 3:
                playbackAction.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                Log.e("CHoice", "Wrong Choice ");
                break;
        }
        return null;
    }

    private void handleIncomingActions(Intent playbackAction){
        if(playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();
        if(actionString.equalsIgnoreCase(ACTION_PLAY)){
            transportControls.play();
        }
        else if(actionString.equalsIgnoreCase(ACTION_PAUSE)){
            transportControls.pause();
        }
        else if(actionString.equalsIgnoreCase(ACTION_NEXT)){
            transportControls.skipToNext();
        }
        else if(actionString.equalsIgnoreCase(ACTION_PREVIOUS)){
            transportControls.skipToPrevious();
        }
        else if(actionString.equalsIgnoreCase(ACTION_STOP)){
            transportControls.stop();
        }
    }













































}




























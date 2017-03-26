package com.example.zappers.play;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

/**
 * Created by aman on 26/3/17.
 */

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener,MediaPlayer.OnPreparedListener,MediaPlayer.OnErrorListener,MediaPlayer.OnSeekCompleteListener,MediaPlayer.OnInfoListener,MediaPlayer.OnBufferingUpdateListener,AudioManager.OnAudioFocusChangeListener {

    private final IBinder iBinder = new LocalBinder();
    private AudioManager audioManager;

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
            //an audio fie is passed to the sevice using putExtra();
            mediaFile = intent.getExtras().getString("media");
        }
        catch(NullPointerException e)
        {
            stopSelf();
        }
        //request audio focus
        if(requestAudioFocus() == false){
            //could not gain focus
            stopSelf();
        }
        if(mediaFile != null && mediaFile != "")
            initMediaPlayer();

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
            mediaPlayer.setDataSource(mediaFile);
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

















































}




























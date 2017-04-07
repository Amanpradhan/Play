package com.example.zappers.play;

import java.io.Serializable;

/**
 * Created by aman on 26/3/17.
 */

public class Audio implements Serializable {

   /*

    May need to keep them public again
    private String data;
    private String title;
    private String album;
    private String artist;
    */
    public String data;
    public String title;
    public String album;
    public String artist;


    public Audio(String data, String title, String album, String artist){
        this.data = data;
        this.title = title;
        this.album = album;
        this.artist = artist;
    }

    public String getData(){
        return data;
    }

    public void setData(String data){
        this.data = data;
    }

    public String getTitle(){
        return title;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public String getAlbum(){
        return album;
    }

    public void setAlbum(String album){
        this.album = album;
    }

    public String getArtist(){
        return artist;
    }

    public void setArtist(String artist){
        this.artist = artist;
    }























}

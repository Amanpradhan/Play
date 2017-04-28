package com.example.zappers.play;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aman on 28/3/17.
 */

public class View_Holder extends RecyclerView.ViewHolder {

    //MainActivity obj = new MainActivity();
    CardView cv;
    TextView data;
    TextView title;
    TextView album;
    TextView artist;
    ImageView imageView;

    View_Holder(View itemView){
        super(itemView);
        cv = (CardView) itemView.findViewById(R.id.cardView);
        data = (TextView) itemView.findViewById(R.id.data);
        title = (TextView) itemView.findViewById(R.id.title);
        album = (TextView) itemView.findViewById(R.id.album);
        artist = (TextView) itemView.findViewById(R.id.artist);
        imageView = (ImageView) itemView.findViewById(R.id.imageView);
    }


}

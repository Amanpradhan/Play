package com.example.zappers.play;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collections;
import java.util.List;

/**
 * Created by aman on 28/3/17.
 */

public class Recycle_View_Adapter extends RecyclerView.Adapter<View_Holder> {

    List<Audio> list = Collections.emptyList();
    Context context;

    public Recycle_View_Adapter(List<Audio> list, Context context){
        this.list = list;
        this.context = context;

    }

    @Override
    public View_Holder onCreateViewHolder(ViewGroup parent, int viewType){
        //inflating layout and initializing View Holder
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_layout, parent, false);
        View_Holder holder = new View_Holder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(View_Holder holder, int position) {
        holder.data.setText(list.get(position).data);
        holder.title.setText(list.get(position).title);
        holder.album.setText(list.get(position).album);
        holder.artist.setText(list.get(position).artist);


        //here we will animate holder
    }

    @Override
    public int getItemCount(){
        //return no. of elements recycler view will display
        return list.size();

    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView){
        super.onAttachedToRecyclerView(recyclerView);
    }

    //inserting new item to RecyclerView on a predefined position
    public void insert(int position, Audio audio ){
        list.add(position, audio);
        notifyItemInserted(position);
    }

    //Remove a RecyclerView item containing a specified Data Object

    public void remove(Audio audio){
        int position = list.indexOf(audio);
        list.remove(position);
        notifyItemRemoved(position);
    }





}

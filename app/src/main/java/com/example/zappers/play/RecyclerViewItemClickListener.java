package com.example.zappers.play;

import android.view.View;

/**
 * Created by aman on 14/4/17.
 */

public interface RecyclerViewItemClickListener {
public void onClick(View view, int position);

    public void onLongClick(View view, int position);
}

package com.passbook.adapter.front_back;

import android.content.Context;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.passbook.R;

public class BackCardView extends RelativeLayout {

    private int mHeight;

    public BackCardView(Context context) {
        super(context);

        float density = context.getResources().getDisplayMetrics().density;
        mHeight = (int) (500 * density + 0.5f);

        inflate(context, R.layout.card_back, this);

        setBackgroundColor(Color.MAGENTA);
        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mHeight));
    }
}

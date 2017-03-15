package com.passbook.adapter.front_back;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.passbook.MainActivity;
import com.passbook.R;
import com.passbook.adapter.SamplePassbookAdapter;
import com.passbook.library.CardGroupFrameLayoutAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class FrontCardView extends RelativeLayout {

    private int mHeightMax;

    private int mHeight;

    private static Map<Integer, Integer> mCardColors = new HashMap<>();

    public FrontCardView(Context context, int cardNo) {
        super(context);

        float density = context.getResources().getDisplayMetrics().density;
        mHeightMax = (int) (450 * density + 0.5f);


        inflate(context, R.layout.card_front, this);

        int color;
        if (mCardColors.get(cardNo) != null)
            color = mCardColors.get(cardNo);
        else {
            Random random = new Random();
            color = 0xCCCCCC & random.nextInt() | 0xFF000000;
            mCardColors.put(cardNo, color);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            Drawable drawable = ContextCompat.getDrawable(context, R.drawable.card_shape);
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            findViewById(R.id.front_layout).setBackground(drawable);
        } else {
            Drawable drawable = context.getResources().getDrawable(R.drawable.card_shape);
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            findViewById(R.id.front_layout).setBackgroundDrawable(drawable);
        }
        findViewById(R.id.front_flip).setOnTouchListener(MainActivity.onButtonTouchListener);
        ((TextView) findViewById(R.id.front_text)).setText(String.format("FRONT (Card #%d)", cardNo));
    }

    public FrontCardView setVisibleHeight(int visibleHeight) {
        this.mHeight = (visibleHeight < 0) ? mHeightMax : Math.min(mHeightMax, visibleHeight);

        setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, mHeight));
        return this;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        findViewById(R.id.front_layout).layout(0, 0, getMeasuredWidth(), mHeightMax);
    }
}

package com.passbook.library;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.passbook.pascal.passbook.cardgroup.scrollview.R;

/**
 * @author Yonis Larsson  2015-12-22 email:yonis.larsson.biz@gmail.com
 */
class BackgroundLayout extends FrameLayout {

    public final static int MAX_ALPHA = 0x8f;

    private Context mContext;

    private Paint mFadePaint;

    private int alpha = 0x00;

    private CardGroupFrameLayout mCardGroupFrameLayout;

    private LinearLayout mPageViewer = null;

    public BackgroundLayout(Context context) {
        this(context, null);
    }

    public BackgroundLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }


    public BackgroundLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mContext = context;
        mFadePaint = new Paint();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mCardGroupFrameLayout.isDisplaying();
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        super.dispatchDraw(canvas);
        drawFade(canvas);
    }

    private void drawFade(Canvas canvas) {
        mFadePaint.setColor(Color.argb(alpha, 0, 0, 0));
        canvas.drawRect(0, 0, getMeasuredWidth(), getHeight(), mFadePaint);
    }

    public void fade(boolean fade) {
        this.alpha = fade ? MAX_ALPHA : 0x00;
        invalidate();
    }

    public void fade(int alpha) {
        this.alpha = alpha;
        invalidate();
    }

    public void setCardGroupScrollView(CardGroupFrameLayout cardGroupFrameLayout) {
        this.mCardGroupFrameLayout = cardGroupFrameLayout;

        if (getChildCount() > 0) {
            mPageViewer = (LinearLayout) getChildAt(0).findViewById(R.id.cardpageviewer);
            mPageViewer.setVisibility(INVISIBLE);
        }
    }

    public void setDotsCount(int count) {
        if (mPageViewer == null)
            return;

        mPageViewer.removeAllViews();

        for (int i = 0; i < count; i++) {
            ImageView dot = new ImageView(mContext);
            dot.setImageResource(R.drawable.ic_dot);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            mPageViewer.addView(dot, params);
        }
    }

    public void selectDot(int no) {
        if (mPageViewer == null)
            return;

        for(int i = 0; i < mPageViewer.getChildCount(); i++) {
            int color = (i == no) ? 0xFFFFFFFF : 0x80FFFFFF;
            ((ImageView) mPageViewer.getChildAt(i)).setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
    }

    public void setVisibleOfPageViewer(boolean visible) {
        if (mPageViewer == null)
            return;

        ViewCompat.setY(mPageViewer, mCardGroupFrameLayout.getPageViewerYCenter());
        ViewCompat.setX(mPageViewer, 0);

        mPageViewer.setVisibility(visible ? VISIBLE : INVISIBLE);
    }

}

package com.passbook.library;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.v4.BuildConfig;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.passbook.pascal.passbook.cardgroup.scrollview.R;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Yonis Larsson  2015-12-22 email:yonis.larsson.biz@gmail.com
 */
//@SuppressWarnings("unused")
public class CardGroupFrameLayout extends FrameLayout {

    public static final String TAG = "CardGroupFrameLayout";

    private static final boolean DEBUG = BuildConfig.DEBUG;

    public static final int DRAG_NONE = -1;

    public static final int DRAG_HORIZONTALLY = 0;

    public static final int DRAG_VERTICALLY = 1;

    private static final int DEFAULT_CARD_MARGIN_TOP_DISPLAY = 30;

    private static final int DEFAULT_CARD_MARGIN_TOP_NO_DISPLAY = 40;

    private static final int DEFAULT_TITLE_BAR_HEIGHT_NO_DISPLAY_MIN_LIMIT = 40;

    private static final int DEFAULT_TITLE_BAR_HEIGHT_NO_DISPLAY_MAX_LIMIT = 150;

    private static final int DEFAULT_TITLE_BAR_HEIGHT_OFFSET_GROUP = 10;

    private static final int DEFAULT_CARD_MARGIN_BOTTOM = 50;

    private static final int DEFAULT_CARD_MARGIN_BOTTOM_GAP = 20;

    private static final int DEFAULT_MARGIN_BOTTOM_DISPLAY_WIDTH_OFFSET = 10;  // percent

    private static final int DEFAULT_MOVE_DISTANCE_TO_TRIGGER = 150;

    private static final int DEFAULT_DURATION = 350;

    private static final int MAX_CLICK_TIME = 300;

    private static float MAX_CLICK_DISTANCE = 5;

    private float mDensity;

    private float titleBarHeightNoDisplayMinLimit;

    private float titleBarHeightNoDisplayMaxLimit;

    private float titleBarHeightOffsetGroup;

    private float marginTopDisplay;

    private float marginTopNoDisplay;

    private float marginBottomDisplay;

    private float marginBottomDisplayGap;

    private float marginBottomDisplayWidthOffset;

    private int titleBarHeight;

    private int cardHeight;

    private VelocityTracker mVelocityTracker;

    private float deltaX;

    private float deltaY;

    private int whichCardOnTouch = -1;

    private float downX;

    private float downY;

    private float firstDownY;

    private float firstDownX;

    private boolean isDisplaying = false;

    private boolean isFlipped = false;

    private Interpolator interpolator = new AccelerateDecelerateInterpolator();

    private float mMoveDistanceToTrigger;

    private int mDisplayingCard  = -1;

    private int mDisplayCardGroupPosition = -1;

    private int mMaxVelocity;

    private int mMinVelocity;

    private int isDragging = DRAG_NONE;

    private boolean isScrolling = false;

    private float xVelocity;

    private float yVelocity;

    private OnCardStateChangeListener mOnCardStateChangeListener;

    private int mDuration;

    private boolean isAnimating = false;

    private BackgroundLayout mBackgroundLayout;

    private int mTouchSlop;

    private long mPressStartTime;

    private Context mContext;

    private boolean isNeedToLayout = false;

    private boolean isPreserveLayout = false;

    private boolean isFlippingForDelete = false;

    private CardGroupFrameLayoutAdapter mAdapter = null;

    public void setCardGroupFrameLayoutAdapter(CardGroupFrameLayoutAdapter adapter) {
        removeAllViews();
        initBackgroundView();

        this.mAdapter = adapter;
        initCardViews();


        // This is happened when adding a new card.
        mAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                DataSetLastChange lastChange = mAdapter.getLastChange();
                if (lastChange.getOperation() != DataSetLastChange.OPERATION_ADD || isDisplaying == false) {
                    removeAllViews();
                    initBackgroundView();
                    initCardViews();
                } else {
                    viewTops.clear();

                    int addedGroupPosition = lastChange.getGroupPosition();
                    if (addedGroupPosition <= mDisplayCardGroupPosition) {
                        mDisplayingCard ++;
                        if (addedGroupPosition == -1) // if new group was added
                            mDisplayCardGroupPosition++;
                    }

                    calcLayoutParams();
                    if (addedGroupPosition == -1)
                        addedGroupPosition = 0;
                    int addedCardPositionOfGroup = mAdapter.getCardCountOfGroup(addedGroupPosition) - 1;
                    int which = mAdapter.getCardViewPosition(addedGroupPosition, addedCardPositionOfGroup);
                    int visibleHeight;
                    if (addedGroupPosition == mDisplayCardGroupPosition)
                        visibleHeight = -1;
                    else
                        visibleHeight = getVisibleHeightOfSliceView(-1);

                    View cardView = replaceCardView(addedGroupPosition, addedCardPositionOfGroup, visibleHeight, null, true);
                    addView(cardView, which + 1);

                    if (addedGroupPosition == mDisplayCardGroupPosition) {
                        ViewCompat.setX(cardView, getCardViewLeftWhenDisplayed(which));
                        ViewCompat.setY(cardView, marginTopDisplay);
                        mBackgroundLayout.setDotsCount(mAdapter.getCardCountOfGroup(addedGroupPosition));
                        mBackgroundLayout.selectDot(mAdapter.getDisplayingCardPositionOfGroup(addedGroupPosition));
                        mBackgroundLayout.setVisibleOfPageViewer(true);
                    } else {
                        ViewCompat.setX(cardView, 0);
                        ViewCompat.setY(cardView, getMeasuredHeight());
                    }
                    isPreserveLayout = true;
                }

                isNeedToLayout = true;
                requestLayout();
            }
        });
    }

    public CardGroupFrameLayout(Context context) {
        this(context, null);
    }

    public CardGroupFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CardGroupFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        ViewConfiguration vc = ViewConfiguration.get(mContext);
        mMaxVelocity = vc.getScaledMaximumFlingVelocity();
        mMinVelocity = vc.getScaledMinimumFlingVelocity() * 8;
        mTouchSlop = vc.getScaledTouchSlop();
        mDensity = mContext.getResources().getDisplayMetrics().density;
        TypedArray a = mContext.obtainStyledAttributes(attrs, R.styleable.CardGroupFrameLayout, defStyleAttr, 0);
        MAX_CLICK_DISTANCE = titleBarHeightNoDisplayMinLimit = a.getDimension(R.styleable.CardGroupFrameLayout_title_bar_height_no_display_min_limit,dip2px(DEFAULT_TITLE_BAR_HEIGHT_NO_DISPLAY_MIN_LIMIT));
        titleBarHeightNoDisplayMaxLimit = a.getDimension(R.styleable.CardGroupFrameLayout_title_bar_height_no_display_max_limit,dip2px(DEFAULT_TITLE_BAR_HEIGHT_NO_DISPLAY_MAX_LIMIT));
        titleBarHeightOffsetGroup = a.getDimension(R.styleable.CardGroupFrameLayout_title_bar_height_offset_group,dip2px(DEFAULT_TITLE_BAR_HEIGHT_OFFSET_GROUP));
        marginTopDisplay = a.getDimension(R.styleable.CardGroupFrameLayout_margin_top_display, dip2px(DEFAULT_CARD_MARGIN_TOP_DISPLAY));
        marginTopNoDisplay = a.getDimension(R.styleable.CardGroupFrameLayout_margin_top_no_display, dip2px(DEFAULT_CARD_MARGIN_TOP_NO_DISPLAY));
        marginBottomDisplay = a.getDimension(R.styleable.CardGroupFrameLayout_margin_bottom_display, dip2px(DEFAULT_CARD_MARGIN_BOTTOM));
        marginBottomDisplayGap = a.getDimension(R.styleable.CardGroupFrameLayout_margin_bottom_display_gap, dip2px(DEFAULT_CARD_MARGIN_BOTTOM_GAP));
        marginBottomDisplayWidthOffset = a.getFloat(R.styleable.CardGroupFrameLayout_margin_bottom_display_width_offset, DEFAULT_MARGIN_BOTTOM_DISPLAY_WIDTH_OFFSET);
        mMoveDistanceToTrigger = a.getDimension(R.styleable.CardGroupFrameLayout_move_distance_to_trigger, dip2px(DEFAULT_MOVE_DISTANCE_TO_TRIGGER));
        mDuration = a.getInt(R.styleable.CardGroupFrameLayout_animator_duration, DEFAULT_DURATION);
        a.recycle();
        initBackgroundView();
    }

    private void initBackgroundView() {
        mBackgroundLayout = new BackgroundLayout(mContext);
        mBackgroundLayout.addView(LayoutInflater.from(mContext).inflate(R.layout.cardgroup_background, null));
        mBackgroundLayout.setCardGroupScrollView(this);
        addView(mBackgroundLayout);
    }

    private void initCardViews() {
        calcLayoutParams();

        int cardCount = mAdapter.getCardTotalCount();
        int groupCount = mAdapter.getGroupCount();
        if (groupCount == 1) {
            mDisplayingCard = mAdapter.getCardViewPosition(0, mAdapter.getDisplayingCardPositionOfGroup(0));
            isDisplaying = true;
            mBackgroundLayout.setDotsCount(cardCount);
            mBackgroundLayout.selectDot(mAdapter.getDisplayingCardPositionOfGroup(0));
            mBackgroundLayout.setVisibleOfPageViewer(cardCount > 1);
        } else {
            mDisplayingCard = -1;
            isDisplaying = false;
            mBackgroundLayout.setVisibleOfPageViewer(false);
        };
        isFlipped = false;
        isAnimating = false;

        for (int i = 0; i < cardCount; i ++) {
            int groupPosition = mAdapter.getGroupPositionFromCardViewPosition(i);
            int cardPosition = mAdapter.getCardPositionOfGroupFromCardViewPosition(i);
            int displayingCardOfGroup = mAdapter.getDisplayingCardPositionOfGroup(groupPosition);
            int visibleHeight;
            if (i == cardCount - 1 || groupCount == 1)
                visibleHeight = -1;
            else if (cardPosition == displayingCardOfGroup)
                visibleHeight = getVisibleHeightOfHeaderView();
            else
                visibleHeight = getVisibleHeightOfSliceView(groupPosition);

            View cardView = replaceCardView(groupPosition, cardPosition, visibleHeight, null, true);
            addView(cardView);

            if (groupCount == 1) {
                ViewCompat.setX(cardView, getCardViewLeftWhenDisplayed(i));
                ViewCompat.setY(cardView, marginTopDisplay);
            }
        }
    }

    /**
     * This is called when visible portion of card view has been changed.
     * @param groupPosition
     * @param cardPositionOfGroup
     * @param visibleHeight indicates how high the view will be. if -1, entire view will be visible.
     * @param cardView is current view.
     * @param isFrontView
     * @return
     */
    private View replaceCardView(int groupPosition, int cardPositionOfGroup, int visibleHeight, View cardView, boolean isFrontView) {
        View newCardView;

        if (isFrontView)
            newCardView = mAdapter.getFrontView(groupPosition, cardPositionOfGroup, visibleHeight, cardView);
        else
            newCardView = mAdapter.getBackView(groupPosition, cardPositionOfGroup);

        if (cardView != null && newCardView != cardView) {
            int index = indexOfChild(cardView);
            removeViewAt(index);
            addView(newCardView, index);
        }
        return newCardView;
    }

    /**
     *
     * @param groupPosition indicates which group current card belongs to so that it's used to calculate visible height of the slice view.
     *                      If -1, it means the card slided down by displaying another one.
     * @return visible height to be displayed.
     */
    private int getVisibleHeightOfSliceView(int groupPosition) {
        int ret;
        if (groupPosition != -1) {
            int cardCountOfGroup = mAdapter.getCardCountOfGroup(groupPosition);
            ret = Math.max(2, (int) (titleBarHeightOffsetGroup / cardCountOfGroup));
        } else {
            int groupPositionOfDisplaying = mAdapter.getGroupPositionFromCardViewPosition(mDisplayingCard);
            int cardCountAtBottom = mAdapter.getCardTotalCount() - mAdapter.getCardCountOfGroup(groupPositionOfDisplaying);
            ret = (int) (marginBottomDisplay / cardCountAtBottom);
        }

        return ret;
    }

    /**
     * This function calculates the height of Card's header. When scrolling, it can be larger or smaller.
     * If calls this function on each time of scrolling, it's not good for performance.
     * So this function returns larger height than original so that the intended view will be displayed with extended visible height in advance.
     * @return visible height to be displayed.
     */
    private int getVisibleHeightOfHeaderView() {
        int ret = (int) (titleBarHeight + titleBarHeightNoDisplayMinLimit);
        return ret;
    }

    private void calcLayoutParams() {
        int height = getMeasuredHeight();
        if (height == 0)
            height = mContext.getResources().getDisplayMetrics().heightPixels;

        cardHeight = (int) (height - marginTopDisplay - marginBottomDisplay - marginBottomDisplayGap);

        if (mAdapter == null)
            return;

        int displayHeight = (int) (height - marginTopNoDisplay);
        int groupCount = mAdapter.getGroupCount();
        if (groupCount > 0)
            titleBarHeight = (int) Math.min(cardHeight, Math.max(titleBarHeightNoDisplayMinLimit, displayHeight / groupCount));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (DEBUG) {
            Log.i(TAG, "onLayout:" + changed);
        }
        if ((!isNeedToLayout && !changed) || mAdapter == null)
            return;

        int cardCount = mAdapter.getCardTotalCount();
        Point positions[] = null;
        if (isDisplaying || isAnimating || isPreserveLayout) {
            positions = new Point[cardCount];
            for (int i = 0; i < cardCount; i++) {
                positions[i] = new Point();
                positions[i].x = (int) ViewCompat.getX(getChildAt(i + 1));
                positions[i].y = (int) ViewCompat.getY(getChildAt(i + 1));
            }
        }

        super.onLayout(changed, left, top, right, bottom);

        isNeedToLayout = false;

        if (isDisplaying || isAnimating || isPreserveLayout) {
            for (int i = 0; i < cardCount; i++) {
                ViewCompat.setX(getChildAt(i + 1), positions[i].x);
                ViewCompat.setY(getChildAt(i + 1), positions[i].y);
            }
            isPreserveLayout = false;
            return;
        }


        calcLayoutParams();

        ViewGroup backgroundView = (ViewGroup) getChildAt(0); // background view
        backgroundView.layout(0, 0, backgroundView.getMeasuredWidth(), backgroundView.getMeasuredHeight());

        if (cardCount == 0) return;

        for (int i = 0; i < cardCount; i++) {
            View cardView = getChildAt(i + 1);
            int t = (int) getCardViewTop(i);
            ViewCompat.setX(cardView, 0);
            ViewCompat.setY(cardView, t); //.layout(0, t, cardView.getMeasuredWidth(), cardHeight + t);
        }
    }

    private float getCardViewTop(int which) {
        if (mAdapter == null)
            return 0;

        int cardCount = mAdapter.getCardTotalCount();

        int groupCardCount;
        int offSetCardPositionOfGroup;
        int groupPosition;
        if (which < cardCount) {
            groupPosition = mAdapter.getGroupPositionFromCardViewPosition(which);
            groupCardCount = mAdapter.getCardCountOfGroup(groupPosition);
            offSetCardPositionOfGroup = mAdapter.getCardPositionOfGroupFromCardViewPosition(which);
            int displayingCardPositionOfGroup = mAdapter.getDisplayingCardPositionOfGroup(groupPosition);
            if (offSetCardPositionOfGroup <= displayingCardPositionOfGroup)
                offSetCardPositionOfGroup = displayingCardPositionOfGroup - offSetCardPositionOfGroup;
        } else {
            groupPosition = mAdapter.getGroupCount() + which - cardCount;
            offSetCardPositionOfGroup = 0;
            groupCardCount = 1;
        }
        return marginTopNoDisplay + titleBarHeight * groupPosition - titleBarHeightOffsetGroup * offSetCardPositionOfGroup / groupCardCount;
    }

    private float getCardViewLeftWhenDisplayed(int which) {
        int groupPosition = mAdapter.getGroupPositionFromCardViewPosition(which);
        int cardPositionOfGroup = mAdapter.getCardPositionOfGroupFromCardViewPosition(which);
        int displayingCardPositionOfGroup = mAdapter.getDisplayingCardPositionOfGroup(groupPosition);

        return getMeasuredWidth() * 1.03f * (cardPositionOfGroup - displayingCardPositionOfGroup);
    }


    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent event) {
        initVelocityTracker(event);
        boolean isConsume = false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isConsume = handleActionDown(event);
                break;
            case MotionEvent.ACTION_MOVE:
                handleActionMove(event);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                handleActionUp(event);
                releaseVelocityTracker();
                break;
        }
        return isConsume || super.dispatchTouchEvent(event);
    }

    private boolean handleActionDown(MotionEvent event) {
        if (mAdapter == null || isAnimating)
            return false;

        boolean isConsume = false;
        mPressStartTime = System.currentTimeMillis();
        firstDownX = downX = event.getX();
        firstDownY = downY = event.getY();
        int cardTotalCount =  mAdapter.getCardTotalCount();
        //Judge which card on touching
        if(!isDisplaying && cardTotalCount > 0 && downY > ViewCompat.getY(getChildAt(1)) && downY < ViewCompat.getY(getChildAt(cardTotalCount - 1 + 1)) + cardHeight) {
            for(int i = cardTotalCount - 1; i >= 0; i --) {
                if (downY > ViewCompat.getY(getChildAt(i + 1))) {
                    whichCardOnTouch = i;
                    if (mOnCardStateChangeListener != null)
                        mOnCardStateChangeListener.onTouchCard(whichCardOnTouch);
                    isConsume = true;
                    break;
                }
            }
        //Judge if touches on bottom cards folded
        }else if (isDisplaying && downY > getMeasuredHeight() - marginBottomDisplay) {
            whichCardOnTouch = mDisplayingCard + 1;
        //on Displayed Card
        }else if (isDisplaying && mDisplayingCard >= 0 && downY > marginTopDisplay && downY < marginTopDisplay + cardHeight) {
            whichCardOnTouch = mDisplayingCard;
            if (DEBUG) Log.d(TAG, "whichCardOnTouch:" + whichCardOnTouch);
        }
            //outside of Displayed Card
//        else if(isDisplaying && (downY < marginTopDisplay || (mDisplayingCard >= 0 && (downY > marginTopDisplay + cardHeight)))) {
//            hideCard(mDisplayingCard);
//        }

        return isConsume;
    }

    private void handleActionMove(MotionEvent event) {

        if(whichCardOnTouch == -1 || mAdapter == null || isAnimating) return;
        if(mDisplayingCard != -1 && eventForDisplayingCard(event)){
            if(DEBUG) Log.d(TAG, "eventForDisplayingCard:" + true);
            return;
        }

        deltaX = event.getX() - downX;
        deltaY = event.getY() - downY;

        int realChildCount = mAdapter.getCardTotalCount();

        if (mDisplayingCard == -1 || whichCardOnTouch == mDisplayingCard) {
            if (realChildCount > 1 && isDragging == DRAG_NONE && !isScrolling &&
                    (Math.abs(event.getY() - firstDownY) > mTouchSlop || Math.abs(event.getX() - firstDownX) > mTouchSlop)) {

                computeVelocity();
                if (isDisplaying) {
                    if (Math.abs(yVelocity) < Math.abs(xVelocity)) {
                        int groupPosition = mAdapter.getGroupPositionFromCardViewPosition(whichCardOnTouch);
                        int cardCountOfGroup = mAdapter.getCardCountOfGroup(groupPosition);
                        if (cardCountOfGroup > 1) {
                            isDragging = DRAG_HORIZONTALLY;

//                            for (int i = 0; i < cardCountOfGroup; i++) {
//                                getChildAt(whichCardOnTouch - i + 1).setScaleX(0.97f);
//                            }
                        }
                    } else if (mAdapter.getGroupCount() > 1)
                        isDragging = DRAG_VERTICALLY;
                } else if (Math.abs(yVelocity) > Math.abs(xVelocity))
                    isScrolling = true;
            }

            if (isDragging == DRAG_VERTICALLY) {
                View touchingChildView = getChildAt(whichCardOnTouch + 1);

                float destScaleX = 1.0f;
                if (mDisplayingCard < mAdapter.getCardTotalCount() - 1) {
                    View nextView = getChildAt(mDisplayingCard + 2);
                    destScaleX = ViewCompat.getScaleX(nextView);
                }

                float curY = ViewCompat.getY(touchingChildView);
//                if (deltaY < 0 && curY < 0)
//                    deltaY /= 4.0f;
                curY += deltaY;
                float curScaleX = 1.0f - (1.0f - destScaleX) * (curY - marginTopDisplay) / mMoveDistanceToTrigger;
                curScaleX = Math.max(destScaleX, Math.min(curScaleX, 1.0f));

                touchingChildView.offsetTopAndBottom((int) deltaY);
                touchingChildView.setScaleX(curScaleX);
            } else if (isDragging == DRAG_HORIZONTALLY) {
                int groupPosition = mAdapter.getGroupPositionFromCardViewPosition(whichCardOnTouch);
                int displayingCardPositionOfGroup = mAdapter.getDisplayingCardPositionOfGroup(groupPosition);
                int cardCountOfGroup = mAdapter.getCardCountOfGroup(groupPosition);
                float x = ViewCompat.getX(getChildAt(whichCardOnTouch + 1));
                if ((displayingCardPositionOfGroup == cardCountOfGroup - 1 && x < 0 && deltaX < 0) ||
                    (displayingCardPositionOfGroup == 0 && x > 0 && deltaX > 0))
                    deltaX /= 4.0f;

                for (int i = 0; i < cardCountOfGroup; i ++) {
                    getChildAt(whichCardOnTouch - i + 1).offsetLeftAndRight((int) deltaX);
                }
            } else if (isScrolling) {
                scrollCards(deltaY);
            }
        }
        downX = event.getX();
        downY = event.getY();
    }

    private void handleActionUp(MotionEvent event) {
        if(whichCardOnTouch == -1 || mAdapter == null || isAnimating) return;
        long pressDuration = System.currentTimeMillis() - mPressStartTime;
        computeVelocity();

        int cardTotalCount = mAdapter.getCardTotalCount();
        if(!isDisplaying && cardTotalCount > 0 && pressDuration < MAX_CLICK_TIME &&   //means click
                distance(firstDownX,firstDownY,event.getX(),event.getY()) < MAX_CLICK_DISTANCE) {
            displayCard(whichCardOnTouch);
        } else if (!isDisplaying && isScrolling) {
            long duration = (long) (mDuration * Math.min(Math.abs(yVelocity) / dip2px(20), 0.8f));
            ValueAnimator valueAnimator = ValueAnimator.ofFloat(yVelocity, 0).setDuration(duration);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float value = (float) valueAnimator.getAnimatedValue();
                    scrollCards(value * 10f);
                }
            });
            AnimatorSet set = new AnimatorSet();
            set.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {
                    isAnimating = true;
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    isAnimating = false;
                    settleCardPositions(false);
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                    isAnimating = false;
                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }
            });
            set.setInterpolator(interpolator);
            set.playTogether(valueAnimator);
            set.start();

        }else if(isDisplaying && !eventForDisplayingCard(event)) {
            if (pressDuration < MAX_CLICK_TIME &&   //means click
                    distance(firstDownX,firstDownY,event.getX(),event.getY()) < MAX_CLICK_DISTANCE) {
                if (mAdapter.getGroupCount() > 1)
                    hideCard(mDisplayingCard);
            }else if (whichCardOnTouch == mDisplayingCard) {
                if (isDragging == DRAG_VERTICALLY) {
                    float currentY = ViewCompat.getY(getChildAt(mDisplayingCard + 1));

                    // reset Y position to standard displaying position
                    if (currentY < marginTopDisplay || currentY <= (marginTopDisplay + mMoveDistanceToTrigger)) {
                        ObjectAnimator.ofFloat(getChildAt(mDisplayingCard + 1), "y",
                                currentY, marginTopDisplay)
                                .setDuration(mDuration)
                                .start();
                        ObjectAnimator.ofFloat(getChildAt(mDisplayingCard + 1), "scaleX", 1.0f)
                                .setDuration(mDuration)
                                .start();
                    // user dragged down in order to hide the card
                    } else if (currentY > (marginTopDisplay + mMoveDistanceToTrigger)) {
                        float destY = getMeasuredHeight();
                        float destScaleX = 1.0f;
                        if (mDisplayingCard < mAdapter.getCardTotalCount() - 1) {
                            View nextView = getChildAt(mDisplayingCard + 2);
                            destY = ViewCompat.getY(nextView);
                            destScaleX = ViewCompat.getScaleX(nextView);
                        }

                        List<Animator> animators = new ArrayList<>(2);
                        animators.add(ObjectAnimator
                                .ofFloat(getChildAt(mDisplayingCard + 1), "y", ViewCompat.getY(getChildAt(mDisplayingCard + 1)), destY)
                                .setDuration(mDuration));
                        animators.add(ObjectAnimator
                                .ofFloat(getChildAt(mDisplayingCard + 1), "scaleX", destScaleX)
                                .setDuration(mDuration));

                        AnimatorSet set = new AnimatorSet();
                        set.addListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animator) {
                                isAnimating = true;
                            }

                            @Override
                            public void onAnimationEnd(Animator animator) {
                                int groupPosition = mAdapter.getGroupPositionFromCardViewPosition(mDisplayingCard);
                                int cardCountOfGroup = mAdapter.getCardCountOfGroup(groupPosition);
                                float x = ViewCompat.getX(getChildAt(mDisplayingCard + 1));
                                float y = ViewCompat.getY(getChildAt(mDisplayingCard + 1));
                                float scaleX = ViewCompat.getScaleX(getChildAt(mDisplayingCard + 1));
                                for (int i = 0; i < cardCountOfGroup; i++) {
                                    View view = getChildAt(mDisplayingCard + 1 - i);
                                    ViewCompat.setX(view, x);
                                    ViewCompat.setY(view, y);
                                    ViewCompat.setScaleX(view, scaleX);
                                }

                                isAnimating = false;
                                hideCard(mDisplayingCard);
                            }

                            @Override
                            public void onAnimationCancel(Animator animator) {
                                int groupPosition = mAdapter.getGroupPositionFromCardViewPosition(mDisplayingCard);
                                int cardCountOfGroup = mAdapter.getCardCountOfGroup(groupPosition);
                                float x = ViewCompat.getX(getChildAt(mDisplayingCard + 1));
                                float y = ViewCompat.getY(getChildAt(mDisplayingCard + 1));
                                float scaleX = ViewCompat.getScaleX(getChildAt(mDisplayingCard + 1));
                                for (int i = 0; i < cardCountOfGroup; i++) {
                                    View view = getChildAt(mDisplayingCard + 1 - i);
                                    ViewCompat.setX(view, x);
                                    ViewCompat.setY(view, y);
                                    ViewCompat.setScaleX(view, scaleX);
                                }
                                isAnimating = false;
                                hideCard(mDisplayingCard);
                            }

                            @Override
                            public void onAnimationRepeat(Animator animator) {

                            }
                        });
                        set.setInterpolator(interpolator);
                        set.playTogether(animators);
                        set.start();
                    }
                } else if (isDragging == DRAG_HORIZONTALLY) {
                    int groupPosition = mAdapter.getGroupPositionFromCardViewPosition(whichCardOnTouch);
                    int displayingCardPositionOfGroup = mAdapter.getDisplayingCardPositionOfGroup(groupPosition);
                    int cardCountOfGroup = mAdapter.getCardCountOfGroup(groupPosition);

                    float currentX = ViewCompat.getX(getChildAt(mDisplayingCard + 1));
                    if (currentX >= getMeasuredWidth() / 2 && displayingCardPositionOfGroup > 0) {
                        int newPosition = mDisplayingCard - displayingCardPositionOfGroup;
                        mAdapter.swipeLeft(groupPosition);
                        mBackgroundLayout.selectDot(mAdapter.getDisplayingCardPositionOfGroup(groupPosition));

                        View curDisplayedCard = getChildAt(mDisplayingCard + 1);
                        removeView(curDisplayedCard);
                        addView(curDisplayedCard, newPosition + 1);

                    } else if (currentX <= -getMeasuredWidth() / 2 && displayingCardPositionOfGroup < cardCountOfGroup - 1) {
                        int oldPosition = mDisplayingCard - displayingCardPositionOfGroup - 1;
                        mAdapter.swipeRight(groupPosition);
                        mBackgroundLayout.selectDot(mAdapter.getDisplayingCardPositionOfGroup(groupPosition));

                        View newDisplayedCard = getChildAt(oldPosition + 1);
                        removeView(newDisplayedCard);
                        addView(newDisplayedCard, mDisplayingCard + 1);
                    }

                    for (int i = 0; i < cardCountOfGroup; i ++) {
                        int card = mDisplayingCard - i;
                        View cardView = getChildAt(card + 1);

//                        ObjectAnimator.ofFloat(cardView, "scaleX",
//                                ViewCompat.getScaleX(cardView), 1.0f)
//                                .setDuration(mDuration)
//                                .start();
                        ObjectAnimator.ofFloat(cardView, "x",
                                ViewCompat.getX(cardView), getCardViewLeftWhenDisplayed(card))
                                .setDuration(mDuration)
                                .start();
                    }
                }
            }
        }

        whichCardOnTouch = -1;
        deltaX = 0;
        deltaY = 0;
        isDragging = DRAG_NONE;
        isScrolling = false;
    }


    /**
     * @param event  touch event
     * @return true if need dispatch touch event to child view,otherwise
     */
    private boolean eventForDisplayingCard(MotionEvent event) {
        if (isFlipped)
            return true;

        View view = getChildAt(mDisplayingCard + 1);
        while (view instanceof ViewGroup) {
            View childView = findTopChildUnder((ViewGroup) view, firstDownX, firstDownY - marginTopDisplay);
            if (childView == null)
                break;
            view = childView;
        }

        if (view instanceof ImageButton || view instanceof Button)
            return true;

        return false;
    }

//    /**
//     * Copy From AbsListView (API Level >= 19)
//     * @param absListView AbsListView
//     * @param direction Positive to check scrolling up, negative to check
//     *                  scrolling down.
//     * @return true if the list can be scrolled in the specified direction,
//     *         false otherwise
//     */
//    private boolean absListViewCanScrollList(AbsListView absListView,int direction) {
//        final int childCount = absListView.getChildCount();
//        if (childCount == 0) {
//            return false;
//        }
//        final int firstPosition = absListView.getFirstVisiblePosition();
//        if (direction > 0) {//can scroll down
//            final int lastBottom = absListView.getChildAt(childCount - 1).getBottom();
//            final int lastPosition = firstPosition + childCount;
//            return lastPosition < absListView.getCount() || lastBottom > absListView.getHeight() - absListView.getPaddingTop();
//        } else {//can scroll  up
//            final int firstTop = absListView.getChildAt(0).getTop();
//            return firstPosition > 0 || firstTop < absListView.getPaddingTop();
//        }
//    }
//
//    /**
//     *  Copy From ScrollView (API Level >= 14)
//     * @param direction Positive to check scrolling up, negative to check
//     *                  scrolling down.
//     *   @return true if the scrollView can be scrolled in the specified direction,
//     *         false otherwise
//     */
//    private  boolean scrollViewCanScrollVertically(ScrollView scrollView,int direction) {
//        final int offset = Math.max(0, scrollView.getScrollY());
//        final int range = computeVerticalScrollRange(scrollView) - scrollView.getHeight();
//        if (range == 0) return false;
//        if (direction < 0) { //scroll up
//            return offset > 0;
//        } else {//scroll down
//            return offset < range - 1;
//        }
//    }
//
//    /**
//     * Copy From ScrollView (API Level >= 14)
//     * <p>The scroll range of a scroll view is the overall height of all of its
//     * children.</p>
//     */
//    private int computeVerticalScrollRange(ScrollView scrollView) {
//        final int count = scrollView.getChildCount();
//        final int contentHeight = scrollView.getHeight() - scrollView.getPaddingBottom() - scrollView.getPaddingTop();
//        if (count == 0) {
//            return contentHeight;
//        }
//
//        int scrollRange = scrollView.getChildAt(0).getBottom();
//        final int scrollY = scrollView.getScrollY();
//        final int overScrollBottom = Math.max(0, scrollRange - contentHeight);
//        if (scrollY < 0) {
//            scrollRange -= scrollY;
//        } else if (scrollY > overScrollBottom) {
//            scrollRange += scrollY - overScrollBottom;
//        }
//
//        return scrollRange;
//    }

    private void computeVelocity() {
        mVelocityTracker.computeCurrentVelocity(1, mMaxVelocity);
        yVelocity = mVelocityTracker.getYVelocity();
        xVelocity = mVelocityTracker.getXVelocity();
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (DEBUG) {
            Log.i(TAG, "isDragging:" + isDragging);
            Log.i(TAG, "scrolling:" + isScrolling);
        }

        return isDragging != DRAG_NONE || isScrolling;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        return true;
    }

    public void show(int index) {
        if(index >= mAdapter.getCardTotalCount()) throw new IllegalArgumentException("Card Index Not Exist");
        if(index == mDisplayingCard || isDisplaying) return;
        displayCard(index);
    }

    public void hide(int index) {
        if(index >= mAdapter.getCardTotalCount()) throw new IllegalArgumentException("Card Index Not Exist");
        if(index != mDisplayingCard || !isDisplaying) return;
        hideCard(index);
    }

    public void flip(int index) {
        if(index >= mAdapter.getCardTotalCount()) throw new IllegalArgumentException("Card Index Not Exist");
        if(index != mDisplayingCard || !isDisplaying) return;
        flipCard(index);
    }

    public void delete(int index) {
        if(index >= mAdapter.getCardTotalCount()) throw new IllegalArgumentException("Card Index Not Exist");
        if(index != mDisplayingCard || !isDisplaying) return;
        deleteCard(index);
    }

    public void setOnDisplayOrHideListener(OnCardStateChangeListener onCardStateChangeListener) {
        this.mOnCardStateChangeListener = onCardStateChangeListener;
    }

    public void setAnimatorInterpolator(Interpolator interpolator) {
        this.interpolator = interpolator;
    }

    /**
     *
     * @return less than 0 :No Display Card
     */
    public int getDisplayingCard() {
        return mDisplayingCard;
    }

    public boolean isDisplaying() {
        return isDisplaying;
    }

    public boolean isFlipped() {
        return isFlipped;
    }

    /**
     *
     * @return marginTop unit:dip
     */
    public int getMarginTop() {
        return px2dip(marginTopDisplay);
    }

    /**
     *
     * @param marginTop unit:dip
     */
    public void setMarginTop(int marginTop) {
        this.marginTopDisplay = dip2px(marginTop);
    }

    /**
     *
     * @return unit:dip
     */
    public int getMoveDistanceToTrigger() {
        return px2dip(mMoveDistanceToTrigger);
    }

    /**
     *
     * @param moveDistanceToTrigger unit:dip
     */
    public void setMoveDistanceToTrigger(int moveDistanceToTrigger) {
        this.mMoveDistanceToTrigger = moveDistanceToTrigger;
    }

    public float getPageViewerYCenter() {
        return getMeasuredHeight() - (marginBottomDisplay + marginBottomDisplayGap);
    }

    public void setAnimatorDuration(int duration) {
        this.mDuration = duration;
    }

    public int getAnimatorDuration() {
        return this.mDuration;
    }

    private void initVelocityTracker(MotionEvent event) {
        if(mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
    }

    private void releaseVelocityTracker() {
        if(mVelocityTracker != null) {
            mVelocityTracker.clear();
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private double distance(float x1, float y1, float x2, float y2) {
        float deltaX = x2 - x1;
        float deltaY = y2 - y1;
        return  Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    private void settleCardPositions(final boolean isReset) {
        if(isDisplaying || isAnimating || mAdapter == null) return;

        int cardCount = mAdapter.getCardTotalCount();
        int groupCount = mAdapter.getGroupCount();
        List<Animator> animators = new ArrayList<>(cardCount * 2);
        viewTops.clear();
        for (int i = 0; i < cardCount; i++)
            viewTops.add(0.0f);
        for(int i = cardCount - 1; i >= 0; i--) {
            View cardView = getChildAt(i + 1);
            float t;
            if (isReset) {
                t = getCardViewTop(i);

                int groupPosition = mAdapter.getGroupPositionFromCardViewPosition(i);
                int cardPositionOfGroup = mAdapter.getCardPositionOfGroupFromCardViewPosition(i);
                int displayingCardOfGroup = mAdapter.getDisplayingCardPositionOfGroup(groupPosition);
                int visibleHeight;
                if (i == cardCount - 1)
                    visibleHeight = -1;
                else if (displayingCardOfGroup == cardPositionOfGroup)
                    visibleHeight = getVisibleHeightOfHeaderView();
                else
                    visibleHeight = getVisibleHeightOfSliceView(groupPosition);
                cardView = replaceCardView(groupPosition, cardPositionOfGroup, visibleHeight, cardView, true);
            } else {
                int groupPosition = mAdapter.getGroupPositionFromCardViewPosition(i);

                if (i == cardCount - 1) {
                    t = Math.max(ViewCompat.getY(cardView), getMeasuredHeight() - titleBarHeightNoDisplayMaxLimit);
                    t = Math.min(t, getCardViewTop(i));
                } else {
                    int nextCardGroupPosition = mAdapter.getGroupPositionFromCardViewPosition(i + 1);
                    if (nextCardGroupPosition == groupPosition) {
                        int j = mAdapter.getCardViewPosition(groupPosition, 0);
                        t = Math.max(0, viewTops.get(j) - titleBarHeightOffsetGroup * (j - i) / mAdapter.getCardCountOfGroup(groupPosition));
                    } else {
                        t = Math.max(0, viewTops.get(cardCount - 1) - (groupCount - groupPosition - 1) * titleBarHeight);
                    }
                }
            }
            viewTops.set(i, t);
            animators.add(ObjectAnimator
                    .ofFloat(cardView, "y", ViewCompat.getY(cardView), t)
                    .setDuration(mDuration));
            animators.add(ObjectAnimator
                    .ofFloat(cardView, "scaleX", 1.0f)
                    .setDuration(mDuration));
        }
        AnimatorSet set = new AnimatorSet();
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                isAnimating = true;
                if (isReset) {
                    isNeedToLayout = true;
                    requestLayout();
                }
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                isAnimating = false;
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                isAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        set.setInterpolator(interpolator);
        set.playTogether(animators);
        set.start();
    }

    private ArrayList<Float> viewTops = new ArrayList<>();
    private void displayCard(final int which) {
        if(isDisplaying || isAnimating) return;

        mDisplayCardGroupPosition = mAdapter.getGroupPositionFromCardViewPosition(which);
        final int displayingCardPosition = mAdapter.getDisplayingCardPositionOfGroup(mDisplayCardGroupPosition);
        int card = mAdapter.getCardViewPosition(mDisplayCardGroupPosition, displayingCardPosition);
        final int cardCount = mAdapter.getCardTotalCount();

        viewTops.clear();
        for (int i = 0; i < cardCount; i ++)
            viewTops.add(i, ViewCompat.getY(getChildAt(i + 1)));

        List<Animator> animators = new ArrayList<>(cardCount * 2);
        int n = mAdapter.getCardTotalCount() - mAdapter.getCardCountOfGroup(mDisplayCardGroupPosition);
        for(int i = 0, j = 1, k = 0; i < cardCount; i++) {
            View cardView = getChildAt(i + 1);
            int groupPositionOfI = mAdapter.getGroupPositionFromCardViewPosition(i);
            int cardPosition = mAdapter.getCardPositionOfGroupFromCardViewPosition(i);
            if (groupPositionOfI == mDisplayCardGroupPosition) {
                cardView = replaceCardView(mDisplayCardGroupPosition, cardPosition, (cardPosition == displayingCardPosition) ? -1 : getVisibleHeightOfHeaderView(), cardView, true);
                animators.add(ObjectAnimator.ofFloat(cardView, "y", ViewCompat.getY(cardView), marginTopDisplay + titleBarHeightNoDisplayMinLimit * k++).setDuration(mDuration));
            } else {
                animators.add(ObjectAnimator
                        .ofFloat(cardView, "y", ViewCompat.getY(cardView), getMeasuredHeight() - marginBottomDisplay * (n - j + 1) / n)
                        .setDuration(mDuration));

                animators.add(ObjectAnimator
                        .ofFloat(cardView, "scaleX", (100f - marginBottomDisplayWidthOffset) / 100f + marginBottomDisplayWidthOffset / 100f * j / n)
                        .setDuration(mDuration));

                j++;
            }
        }

        AnimatorSet set = new AnimatorSet();
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                isAnimating = true;
                isNeedToLayout = true;
                requestLayout();
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                List<Animator> animators = new ArrayList<>(cardCount);
                for (int i = 0; i < cardCount; i++) {
                    int groupPositionOfI = mAdapter.getGroupPositionFromCardViewPosition(i);
                    int cardPosition = mAdapter.getCardPositionOfGroupFromCardViewPosition(i);
                    View cardView = getChildAt(i + 1);
                    if (groupPositionOfI == mDisplayCardGroupPosition) {
                        if (cardPosition != displayingCardPosition)
                            cardView = replaceCardView(mDisplayCardGroupPosition, cardPosition, -1, cardView, true);

                        animators.add(ObjectAnimator
                                .ofFloat(cardView, "y", ViewCompat.getY(cardView), marginTopDisplay)
                                .setDuration(mDuration));
                        animators.add(ObjectAnimator
                                .ofFloat(cardView, "x", ViewCompat.getX(cardView), getCardViewLeftWhenDisplayed(i))
                                .setDuration(mDuration));
                    } else {
                        replaceCardView(groupPositionOfI, cardPosition, getVisibleHeightOfSliceView(-1), cardView, true);
                    }
                }
                isNeedToLayout = true;
                requestLayout();
                AnimatorSet set = new AnimatorSet();
                set.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        if (mAdapter.getCardCountOfGroup(mDisplayCardGroupPosition) > 1) { // Display Page Viewer
                            mBackgroundLayout.setDotsCount(mAdapter.getCardCountOfGroup(mDisplayCardGroupPosition));
                            mBackgroundLayout.selectDot(mAdapter.getDisplayingCardPositionOfGroup(mDisplayCardGroupPosition));
                            mBackgroundLayout.setVisibleOfPageViewer(true);
                            isNeedToLayout = true;
                            isPreserveLayout = true;
                            requestLayout();
                        }
                        isAnimating = false;
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {
                        isAnimating = false;
                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {

                    }
                });
                set.setInterpolator(interpolator);
                set.playTogether(animators);
                set.setStartDelay(mDuration);
                set.start();

            }

            @Override
            public void onAnimationCancel(Animator animator) {
                isAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        set.setInterpolator(interpolator);
        set.playTogether(animators);
        set.start();
        isDisplaying = true;
        mDisplayingCard = card;
        if(mOnCardStateChangeListener != null)
            mOnCardStateChangeListener.onDisplay(card);
    }

    private void hideCard(int which) {
        if(isAnimating) return;

        final int cardCount = mAdapter.getCardTotalCount();
        List<Animator> animators = new ArrayList<>(cardCount * 3);
        for(int i = 0; i < cardCount; i ++) {
            View cardView = getChildAt(i + 1);

            int groupPositionOfI = mAdapter.getGroupPositionFromCardViewPosition(i);

            if (mDisplayCardGroupPosition != groupPositionOfI) {
                int cardPositionOfGroup = mAdapter.getCardPositionOfGroupFromCardViewPosition(i);
                int displayingCardOfGroup = mAdapter.getDisplayingCardPositionOfGroup(groupPositionOfI);

                int visibleHeight;
                if (i == cardCount - 1)
                    visibleHeight = -1;
                else if (cardPositionOfGroup == displayingCardOfGroup)
                    visibleHeight = getVisibleHeightOfHeaderView();
                else
                    visibleHeight = getVisibleHeightOfSliceView(groupPositionOfI);
                replaceCardView(groupPositionOfI, cardPositionOfGroup, visibleHeight, cardView, true);
            }

            if (viewTops.size() == 0)
                animators.add(ObjectAnimator
                        .ofFloat(cardView, "y", ViewCompat.getY(cardView), getCardViewTop(i))
                        .setDuration(mDuration));
            else
                animators.add(ObjectAnimator
                        .ofFloat(cardView, "y", ViewCompat.getY(cardView), viewTops.get(i))
                        .setDuration(mDuration));

            animators.add(ObjectAnimator
                    .ofFloat(cardView, "x", ViewCompat.getX(cardView), 0)
                    .setDuration(mDuration));

            animators.add(ObjectAnimator
                    .ofFloat(cardView, "scaleX", 1.0f)
                    .setDuration(mDuration));
        }
        isNeedToLayout = true;
        requestLayout();

        AnimatorSet set = new AnimatorSet();
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                isAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mBackgroundLayout.setVisibleOfPageViewer(false);

                int cardCountOfGroup = mAdapter.getCardCountOfGroup(mDisplayCardGroupPosition);
                int displayingCardOfGroup = mAdapter.getDisplayingCardPositionOfGroup(mDisplayCardGroupPosition);
                for (int i = 0; i < cardCountOfGroup; i++) {
                    int cardViewPosition = mAdapter.getCardViewPosition(mDisplayCardGroupPosition, i);
                    View cardView = getChildAt(cardViewPosition + 1);

                    int visibleHeight;
                    if (i == displayingCardOfGroup) {
                        if (mDisplayCardGroupPosition == mAdapter.getGroupCount() - 1)
                            visibleHeight = -1;
                        else
                            visibleHeight = getVisibleHeightOfHeaderView();
                    } else
                        visibleHeight = getVisibleHeightOfSliceView(mDisplayCardGroupPosition);

                    replaceCardView(mDisplayCardGroupPosition, i, visibleHeight, cardView, true);
                }
                isDisplaying = false;
                mDisplayingCard = -1;
                mDisplayCardGroupPosition = -1;
                isNeedToLayout = true;
                isPreserveLayout = true;
                requestLayout();

                isAnimating = false;
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                isAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        set.setInterpolator(interpolator);
        set.playTogether(animators);
        set.start();
        if(mOnCardStateChangeListener != null)
            mOnCardStateChangeListener.onHide(which);
    }

    private void flipCard(final int which) {
        if (isAnimating) return;

        List<Animator> animators = new ArrayList<>(4);
        View cardView = getChildAt(which + 1);

        animators.add(ObjectAnimator
                .ofFloat(cardView, "alpha", 1, 0.5f)
                .setDuration(mDuration));

        animators.add(ObjectAnimator
                .ofFloat(cardView, "scaleX", 1, 0)
                .setDuration(mDuration));

//        animators.add(ObjectAnimator
//                .ofFloat(cardView, "scaleY", 1, 0.8f)
//                .setDuration(mDuration));

        animators.add(ObjectAnimator
                .ofFloat(cardView, "rotationY", 0, 90)
                .setDuration(mDuration));

        AnimatorSet set = new AnimatorSet();
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                isAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                View cardView = getChildAt(which + 1);
                int groupPosition = mAdapter.getGroupPositionFromCardViewPosition(which);
                int cardPosition = mAdapter.getCardPositionOfGroupFromCardViewPosition(which);
                if (isFlipped)
                    cardView = replaceCardView(groupPosition, cardPosition, -1, cardView, false);
                else
                    cardView = replaceCardView(groupPosition, cardPosition, -1, cardView, true);
                ViewCompat.setY(cardView, marginTopDisplay);
                isNeedToLayout = true;

                List<Animator> animators1 = new ArrayList<>(4);

                animators1.add(ObjectAnimator
                        .ofFloat(cardView, "alpha", 0.5f, 1)
                        .setDuration(mDuration));

                animators1.add(ObjectAnimator
                        .ofFloat(cardView, "scaleX", 0, 1)
                        .setDuration(mDuration));

//                animators1.add(ObjectAnimator
//                        .ofFloat(displayingCard, "scaleY", 0.8f, 1)
//                        .setDuration(mDuration));

                animators1.add(ObjectAnimator
                        .ofFloat(cardView, "rotationY", -90, 0)
                        .setDuration(mDuration));

                AnimatorSet set1 = new AnimatorSet();
                set1.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        for (int i = 0; i < mAdapter.getCardTotalCount(); i++)
                            if (i != which)
                                getChildAt(i + 1).setAlpha(isFlipped ? 0f : 1f);
                        isAnimating = false;

                        if (isFlippingForDelete)
                            deleteCard(which);
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {
                        isAnimating = false;
                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {

                    }
                });
                set1.setInterpolator(interpolator);
                set1.playTogether(animators1);
                set1.start();
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                isAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        set.setInterpolator(interpolator);
        set.playTogether(animators);
        set.start();

        isFlipped = !isFlipped;
        if(mOnCardStateChangeListener != null)
            mOnCardStateChangeListener.onFlip(which);
    }

    private void deleteCard(final int which) {
        if (!isFlippingForDelete) {
            isFlippingForDelete = true;
            flipCard(which);
        } else {
            isFlippingForDelete = false;
            if (isAnimating) return;

            AnimatorSet set = new AnimatorSet();
            set.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {
                    isAnimating = true;
                }

                @Override
                public void onAnimationEnd(Animator animator) {

                    isAnimating = false;

                    int groupPosition = mAdapter.getGroupPositionFromCardViewPosition(which);
                    int cardCountOfGroup = mAdapter.getCardCountOfGroup(groupPosition);
                    if (viewTops.size() > 0)
                        viewTops.remove(which - (mAdapter.getCardCountOfGroup(groupPosition) - 1));
                    mAdapter.removeCard(groupPosition);
                    mDisplayingCard --;
                    cardCountOfGroup --;

                    isNeedToLayout = true;
                    removeViewAt(which + 1);

                    calcLayoutParams();
                    if (cardCountOfGroup > 0) {

                        if (cardCountOfGroup == 1) {
                            mBackgroundLayout.setVisibleOfPageViewer(false);
                        } else {
                            // Page Viewer Reset
                            mBackgroundLayout.setDotsCount(cardCountOfGroup);
                            mBackgroundLayout.selectDot(mAdapter.getDisplayingCardPositionOfGroup(groupPosition));
                        }

                        for (int i = 0; i < cardCountOfGroup; i++) {
                            int card = mDisplayingCard - i;
                            View cardView = getChildAt(card + 1);

                            ObjectAnimator.ofFloat(cardView, "x",
                                    ViewCompat.getX(cardView), getCardViewLeftWhenDisplayed(card))
                                    .setDuration(mDuration)
                                    .start();
                        }
                    } else {
                        // only one group is remained
                        if (mAdapter.getGroupCount() == 1) {
                            int cardCount = mAdapter.getCardTotalCount();
                            mDisplayingCard = cardCount - 1;
                            for (int i = 0; i < cardCount; i++) {
                                View cardView = getChildAt(i + 1);

                                replaceCardView(0, i, -1, cardView, true);

                                ObjectAnimator.ofFloat(cardView, "x",
                                        ViewCompat.getX(cardView), getCardViewLeftWhenDisplayed(i))
                                        .setDuration(mDuration)
                                        .start();
                                ObjectAnimator.ofFloat(cardView, "y",
                                        ViewCompat.getY(cardView), marginTopDisplay)
                                        .setDuration(mDuration)
                                        .start();
                                ObjectAnimator.ofFloat(cardView, "scaleX", 1.0f)
                                        .setDuration(mDuration)
                                        .start();
                            }

                            mBackgroundLayout.setDotsCount(cardCount);
                            mBackgroundLayout.selectDot(mAdapter.getDisplayingCardPositionOfGroup(0));
                            mBackgroundLayout.setVisibleOfPageViewer(cardCount > 1);
                        } else {
                            mDisplayingCard = -1;
                            isDisplaying = false;
                            settleCardPositions(true);
                        }
                    }
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                    isAnimating = false;
                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }
            });
            set.setInterpolator(interpolator);
            set.playTogether(ObjectAnimator.ofFloat(getChildAt(which + 1), "scaleY", 0));
            set.start();


            if(mOnCardStateChangeListener != null)
                mOnCardStateChangeListener.onDelete(which);
        }
    }

    private void scrollCards(float deltaY) {
        int cardCount = mAdapter.getCardTotalCount();
        if (deltaY < 0) {
            deltaY = -deltaY;
            float scrollRoomY = Math.max(0, ViewCompat.getY(getChildAt(cardCount)) - (getMeasuredHeight() - titleBarHeightNoDisplayMaxLimit));
            float realDeltaY = Math.min(deltaY, scrollRoomY);
            realDeltaY += Math.max(0, deltaY - scrollRoomY) * 0.2f;

            for (int i = 0; i < cardCount; i++) {
                float deltaPerChild = Math.min(ViewCompat.getY(getChildAt(i + 1)), realDeltaY);
                getChildAt(i + 1).offsetTopAndBottom((int) -deltaPerChild);

//                if (i != 0) {
//                    View cardView = getChildAt(i);
//                    int groupPosition = mAdapter.getGroupPositionFromCardViewPosition(i - 1);
//                    int cardPositionOfGroup = mAdapter.getCardPositionOfGroupFromCardViewPosition(i - 1);
//                    int displayingCardOfGroup = mAdapter.getDisplayingCardPositionOfGroup(groupPosition);
//                    if (cardPositionOfGroup == displayingCardOfGroup) {
//                        int visibleHeight = (int) (ViewCompat.getY(getChildAt(i + 1)) - ViewCompat.getY(cardView));
//                        replaceCardView(groupPosition, cardPositionOfGroup, visibleHeight, cardView, true);
//                    }
//                }
            }
        } else {
            float scrollRoomY = Math.max(0, getCardViewTop(cardCount - 1) - ViewCompat.getY(getChildAt(cardCount - 1 + 1)));
            float realDeltaY = Math.min(deltaY, scrollRoomY);
            float extraDeltaY = 0;
            if ((int)realDeltaY == 0)
                extraDeltaY = deltaY * 0.05f;

            int groupCount = mAdapter.getGroupCount();
            for (int i = cardCount - 1; i >= 0; i--) {
                View cardView = getChildAt(i + 1);

                float deltaForThisCard;
                if (i == cardCount - 1) {
                    deltaForThisCard = realDeltaY + extraDeltaY * groupCount;
                    cardView.offsetTopAndBottom((int) deltaForThisCard);
                } else {
                    int groupPosition = mAdapter.getGroupPositionFromCardViewPosition(i);
                    int nextCardGroupPosition = mAdapter.getGroupPositionFromCardViewPosition(i + 1);
                    if (nextCardGroupPosition == groupPosition) {
                        int j = mAdapter.getCardViewPosition(groupPosition, 0);
                        float t = Math.max(0, ViewCompat.getY(getChildAt(j + 1)) - titleBarHeightOffsetGroup * (j - i) / mAdapter.getCardCountOfGroup(groupPosition));
                        ViewCompat.setY(cardView, t);
                    } else {
                        groupCount--;
                        float t;
                        if (realDeltaY > 0) {
                            t = Math.max(0, ViewCompat.getY(getChildAt(cardCount - 1 + 1)) - titleBarHeight * (mAdapter.getGroupCount() - groupCount));
                            ViewCompat.setY(cardView, t);
                        } else {
                            deltaForThisCard = extraDeltaY * groupCount;
                            cardView.offsetTopAndBottom((int) deltaForThisCard);
                        }

//                        int visibleHeight = (int) (ViewCompat.getY(getChildAt(i + 2)) - ViewCompat.getY(cardView));
//                        int cardPositionOfGroup = mAdapter.getCardPositionOfGroupFromCardViewPosition(i);
//                        replaceCardView(groupPosition, cardPositionOfGroup, visibleHeight, cardView, true);
                    }
                }
            }
        }

//        isNeedToLayout = true;
//        requestLayout();
    }

    private View findTopChildUnder(ViewGroup parentView, float x, float y) {
        final int childCount = parentView.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            final View child = parentView.getChildAt(i);
            if (child.getVisibility() == VISIBLE
                && x >= child.getLeft() && x < child.getRight() && y >= child.getTop() && y < child.getBottom()) {
                if(DEBUG) Log.d(TAG, "findTopChildUnder:" + child.getTop() + ":" + child.getBottom());
                return child;
            }
        }
        return null;
    }

    private int px2dip(float pxVal) {
        return (int)(pxVal/mDensity + 0.5f);
    }

    private int dip2px(int dipVal) {
        return (int)(dipVal * mDensity + 0.5f);
    }

    public interface OnCardStateChangeListener {

        void onDisplay(int which);

        void onHide(int which);

        void onTouchCard(int which);

        void onFlip(int which);

        void onDelete(int which);
    }



    public static class DataSetLastChange {
        public static final int OPERATION_NONE = 0;
        public static final int OPERATION_ADD = 1;

        private int operation;
        private int groupPosition;

        public DataSetLastChange() {
            operation = OPERATION_NONE;
        }

        public void setLastChange(int operation, int groupPosition) {
            this.operation = operation;
            this.groupPosition = groupPosition;
        }

        public int getOperation() {
            return operation;
        }

        public int getGroupPosition() {
            return groupPosition;
        }
    }
}

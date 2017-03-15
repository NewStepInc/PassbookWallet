package com.passbook;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.passbook.adapter.Card;
import com.passbook.adapter.SamplePassbookAdapter;
import com.passbook.library.CardGroupFrameLayout;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    SamplePassbookAdapter mCardGroupFrameLayoutAdapter = null;
    CardGroupFrameLayout cardGroupFrameLayout;

    public static View.OnTouchListener onButtonTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE)
                v.setAlpha(0.3f);
            else
                v.setAlpha(1f);
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        cardGroupFrameLayout = (CardGroupFrameLayout) findViewById(R.id.multi_card_menu);
        mCardGroupFrameLayoutAdapter = new SamplePassbookAdapter(this);
        cardGroupFrameLayout.setCardGroupFrameLayoutAdapter(mCardGroupFrameLayoutAdapter);
        cardGroupFrameLayout.setOnDisplayOrHideListener(new CardGroupFrameLayout.OnCardStateChangeListener() {
            @Override
            public void onDisplay(int which) {
                Log.d(TAG, "onDisplay:" + which);
            }

            @Override
            public void onHide(int which) {
                Log.d(TAG, "onHide:" + which);
            }

            @Override
            public void onTouchCard(int which) {
                Log.d(TAG, "onTouchCard:" + which);
            }

            @Override
            public void onFlip(int which) {
                Log.d(TAG, "onFlipCard:" + which);
            }

            @Override
            public void onDelete(int which) {
                Log.d(TAG, "onDeleteCard:" + which);
            }
        });


    }

    public void onAdd(View view) {
//        if (cardGroupFrameLayout.isDisplaying() && mCardGroupFrameLayoutAdapter.getGroupCount() >= 2 || cardGroupFrameLayout.isFlipped())
//            return;
//
//        Random random = new Random();
//        mCardGroupFrameLayoutAdapter.addCard(random.nextInt(mCardGroupFrameLayoutAdapter.getGroupCount() + 1) - 1, new Card(this));

        // new group
//        mCardGroupFrameLayoutAdapter.addCard(-1, new Card(this));

        // add into existing group
        mCardGroupFrameLayoutAdapter.addCard(1, new Card(this));

        Log.d(TAG, "onAddCard_CardCount:" + mCardGroupFrameLayoutAdapter.getCardTotalCount());
    }

    public void onFlip(View view) {
        cardGroupFrameLayout.flip(cardGroupFrameLayout.getDisplayingCard());
    }

    public void onDelete(View view) {
        cardGroupFrameLayout.delete(cardGroupFrameLayout.getDisplayingCard());
    }

    public void hide(View view) {
        cardGroupFrameLayout.hide(cardGroupFrameLayout.getDisplayingCard());
    }

    @Override
    public void onBackPressed() {
        if (cardGroupFrameLayout.isDisplaying()) {
            if (cardGroupFrameLayout.isFlipped())
                cardGroupFrameLayout.flip(cardGroupFrameLayout.getDisplayingCard());
            else
                cardGroupFrameLayout.hide(cardGroupFrameLayout.getDisplayingCard());
        } else {
            super.onBackPressed();
        }
    }
}

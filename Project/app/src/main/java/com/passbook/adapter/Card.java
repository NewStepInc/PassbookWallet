package com.passbook.adapter;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.passbook.adapter.front_back.BackCardView;
import com.passbook.adapter.front_back.FrontCardView;
import com.passbook.R;

public class Card {

    public static int CARD_COUNT = 0;

    private Context mContext = null;

    private int mCardNo;

    public Card(Context context) {
        this.mContext = context;
        this.mCardNo = ++CARD_COUNT;
    }

    View getFrontView(int visibleHeight, View reusuableView) {
        if (reusuableView != null && reusuableView instanceof FrontCardView)
            return ((FrontCardView) reusuableView).setVisibleHeight(visibleHeight);

        if (mContext == null)
            return null;

        FrontCardView frontCardView = new FrontCardView(mContext, mCardNo);
        frontCardView.setVisibleHeight(visibleHeight);

        return frontCardView;
    }

    View getBackView() {
        if (mContext == null)
            return null;

        BackCardView backCardView = new BackCardView(mContext);
        ((TextView) backCardView.findViewById(R.id.back_text)).setText(String.format("BACK (Card #%d)", mCardNo));

        return backCardView;
    }
}

package com.passbook.adapter;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;

import com.passbook.library.CardGroupFrameLayout.DataSetLastChange;
import com.passbook.library.CardGroupFrameLayoutAdapter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SamplePassbookAdapter implements CardGroupFrameLayoutAdapter<PassGroup, Card> {

    private static List<PassGroup> cards = new ArrayList<>();

    private Context mContext = null;

    private ArrayList<DataSetObserver> observers = new ArrayList<>();

    private DataSetLastChange lastChange = new DataSetLastChange();


    public SamplePassbookAdapter(Context context) {
        mContext = context;
        cards.add(new PassGroup(new Card(context), new Card(context), new Card(context)));
        cards.add(new PassGroup(new Card(context)));
        cards.add(new PassGroup(new Card(context)));
        cards.add(new PassGroup(new Card(context), new Card(context), new Card(context), new Card(context), new Card(context), new Card(context), new Card(context)));
        cards.add(new PassGroup(new Card(context), new Card(context), new Card(context)));
        cards.add(new PassGroup(new Card(context)));
    }

    @Override
    public DataSetLastChange getLastChange() {
        return lastChange;
    }

    public void addCard(int groupPosition, Card card) {
        if (groupPosition == -1) // new group
            cards.add(0, new PassGroup(card));
        else
            getGroup(groupPosition).addCard(card);

        lastChange.setLastChange(DataSetLastChange.OPERATION_ADD, groupPosition);
        notifyDataSetChanged();
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        observers.add(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyDataSetChanged() {
        for (DataSetObserver observer : observers) {
            observer.onChanged();
        }
    }

    @Override
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    @Override
    public int getGroupCount() {
        return cards.size();
    }

    @Override
    public int getCardCountOfGroup(int groupPosition) {
        return getGroup(groupPosition).getCardCount();
    }

    @Override
    public int getCardTotalCount() {
        int totalCount = 0;
        for (Iterator<PassGroup> it = cards.iterator(); it.hasNext(); )
            totalCount += it.next().getCardCount();
        return totalCount;
    }

    @Override
    public PassGroup getGroup(int groupPosition) {
        return cards.get(groupPosition);
    }

    @Override
    public void sortGroup(int groupPosition, int offset) {
        if (groupPosition + offset < 0 || groupPosition + offset >= getGroupCount())
            return;

        PassGroup passGroup = getGroup(groupPosition);
        cards.remove(passGroup);
        cards.add(groupPosition + offset, passGroup);
    }

    @Override
    public Card getCard(int groupPosition, int cardPosition) {
        return getGroup(groupPosition).getCard(cardPosition);
    }

    @Override
    public int getGroupPositionFromCardViewPosition(int cardViewPosition) {
        if (isEmpty())
            return -1;

        int groupPosition = -1;
        int cardCount = -1;
        do {
            cardCount += getGroup(++groupPosition).getCardCount();
        } while (cardCount < cardViewPosition && groupPosition < getGroupCount() - 1);

        if (cardCount < cardViewPosition)
            return -1;

        return groupPosition;
    }

    @Override
    public int getCardPositionOfGroupFromCardViewPosition(int cardViewPosition) {
        int groupPosition = getGroupPositionFromCardViewPosition(cardViewPosition);
        if (groupPosition == -1)
            return -1;

        for (int i = 0; i < groupPosition; i++)
            cardViewPosition -= getGroup(i).getCardCount();

        return getGroup(groupPosition).getCardPositionFromCardViewPosition(cardViewPosition);
    }

    @Override
    public int getDisplayingCardPositionOfGroup(int groupPosition) {
        return getGroup(groupPosition).getDisplayingCardPosition();
    }

    @Override
    public int getCardViewPosition(int groupPosition, int cardPosition) {
        if (groupPosition < 0 || groupPosition >= getGroupCount())
            return -1;

        PassGroup passGroup = getGroup(groupPosition);
        if (cardPosition < 0 || cardPosition >= passGroup.getCardCount())
            return -1;

        int cardViewPosition = -1;
        for (int i = 0; i <= groupPosition; i++)
            cardViewPosition += getCardCountOfGroup(i);

        int displayingCardPosition = passGroup.getDisplayingCardPosition();
        if (cardPosition <= displayingCardPosition)
            cardViewPosition -= displayingCardPosition - cardPosition;
        else
            cardViewPosition -= cardPosition;

        return cardViewPosition;
    }

    @Override
    public void swipeLeft(int groupPosition) {
        getGroup(groupPosition).swipeLeft();
    }

    @Override
    public void swipeRight(int groupPosition) {
        getGroup(groupPosition).swipeRight();
    }

    @Override
    public void removeCard(int groupPosition) {
        if (getGroup(groupPosition).removeCard() == 0)
            cards.remove(groupPosition);
    }

    @Override
    public View getFrontView(int groupPosition, int cardPosition, int visibleHeight, View reusableView) {
        Card card = getCard(groupPosition, cardPosition);
        return card.getFrontView(visibleHeight, reusableView);
    }

    @Override
    public View getBackView(int groupPosition, int cardPosition) {
        Card card = getCard(groupPosition, cardPosition);

        return card.getBackView();
    }
}

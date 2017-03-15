package com.passbook.library;

import android.app.Fragment;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;

public interface CardGroupFrameLayoutAdapter<G, C> {

    /* Based on Adapter */
    void registerDataSetObserver(DataSetObserver observer);
    void unregisterDataSetObserver(DataSetObserver observer);

    void notifyDataSetChanged();

    int getGroupCount();
    G getGroup(int groupPosition);
    void sortGroup(int groupPosition, int offset);

    int getCardCountOfGroup(int groupPosition);
    int getDisplayingCardPositionOfGroup(int groupPosition);

    int getCardTotalCount();
    C getCard(int groupPosition, int cardPosition);

    int getCardViewPosition(int groupPosition, int cardPosition);

    /**
    * @return true if this adapter doesn't contain any data.  This is used to determine
    * whether the empty view should be displayed.  A typical implementation will return
    * getCount() == 0 but since getCount() includes the headers and footers, specialized
    * adapters might want a different behavior.
    */
    boolean isEmpty();

    int getGroupPositionFromCardViewPosition(int cardViewPosition);

    int getCardPositionOfGroupFromCardViewPosition(int cardViewPosition);

    void swipeLeft(int groupPosition);

    void swipeRight(int groupPosition);

    void removeCard(int groupPosition);


    View getBackView(int groupPosition, int cardPosition);

//    public static final int ITEM_VIEW_TYPE_HEADER_SLICE = 1; /* Top slice of passes inside a group */
//    public static final int ITEM_VIEW_TYPE_HEADER = 2; /* header of pass for first pass in group */
//    public static final int ITEM_VIEW_TYPE_BODY= 3; /* header + body when having few passes
    /**
     * Get a View that displays the data at the specified position in the data set. You can either
     * create a View manually or inflate it from an XML layout file. When the View is inflated, the
     * parent View (GridView, ListView...) will apply default layout parameters unless you use
     * {@link android.view.LayoutInflater#inflate(int, android.view.ViewGroup, boolean)}
     * to specify a root view and to prevent attachment to the root.
     *
     * @param groupPosition The position of the group within the adapter's data set of the item whose view
     *        we want.
     * @param cardPosition The position of the card within the adapter's data set of the item whose view
     *        we want.
     * @param visibleHeight The visible height of view we want. If -1, full view will be displayed.
     * @param reusableView The old view to reuse, if possible. Note: You should check that this view
     *        is non-null and of an appropriate type before using. If it is not possible to convert
     *        this view to display the correct data, this method can create a new view.
     * @return A View corresponding to the data at the specified position.
     */
    View getFrontView(int groupPosition, int cardPosition, int visibleHeight, View reusableView);

    CardGroupFrameLayout.DataSetLastChange getLastChange();


  /* Based on FragmentStatePagerAdapter */
//  Fragment getFrontFragment(int groupPosition, int cardPosition);
//  Fragment getBackFragment(int groupPosition, int cardPosition);
}

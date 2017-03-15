package com.passbook.adapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PassGroup {

  int displayingCardPosition;

  public List<Card> cards;

  public PassGroup(Card... cards) {
    this.cards = new ArrayList<>(Arrays.asList(cards));
    displayingCardPosition = 0;
  }

  public int getCardCount() {
    return cards.size();
  }

  public Card getCard(int cardPosition) {
    return cards.get(cardPosition);
  }

  public int addCard(Card card) {
      cards.add(card);
      return cards.size();
  }

  public int removeCard() {
      cards.remove(displayingCardPosition);
      if (displayingCardPosition > 0)
        displayingCardPosition--;
      return cards.size();
  }

  public int getCardPosition(Card card) {
      return cards.indexOf(card);
  }

  public void swipeLeft() {
    displayingCardPosition = Math.max(0, displayingCardPosition - 1);
  }

  public void swipeRight() {
    displayingCardPosition = Math.min(getCardCount() - 1, displayingCardPosition + 1);
  }

  public int getCardPositionFromCardViewPosition(int cardViewPosition) {
    if (cardViewPosition >= getCardCount())
      return -1;

    if (cardViewPosition < getCardCount() - displayingCardPosition - 1)
      return getCardCount() - cardViewPosition - 1;

    return cardViewPosition - (getCardCount() - displayingCardPosition - 1);
  }

  public int getDisplayingCardPosition() {
    return displayingCardPosition;
  }
}

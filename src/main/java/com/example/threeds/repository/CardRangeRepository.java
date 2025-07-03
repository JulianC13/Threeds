package com.example.threeds.repository;

import java.util.List;
import org.springframework.stereotype.Repository;

import com.example.threeds.model.CardRange;
import com.example.threeds.model.IntervalTree;

/**
 * Repository layer for card range operations using an in-memory interval tree.
 * 
 * @author Julian Camilo
 * @version 1.0
 * @since 2025
 */
@Repository
public class CardRangeRepository {

    private final IntervalTree intervalTree = new IntervalTree();

    /**
     * Finds the card range that contains the specified PAN.
     * 
     * @param pan the Primary Account Number to search for
     * @return the matching card range, or null if no match is found
     */
    public CardRange findCardRange(long pan) {
        return intervalTree.findRange(pan);
    }

    /**
     * Saves multiple card ranges to the repository.
     * 
     * @param cardRanges the list of card ranges to store
     * @throws IllegalArgumentException if cardRanges is null
     */
    public void saveAll(List<CardRange> cardRanges) {
        if (cardRanges == null) {
            throw new IllegalArgumentException("Card ranges list cannot be null");
        }

        intervalTree.insertAll(cardRanges);
    }

    /**
     * Clears all card ranges from the repository.
     * 
     */
    public void clear() {
        intervalTree.clear();
    }
}

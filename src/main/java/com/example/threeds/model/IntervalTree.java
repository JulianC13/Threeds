package com.example.threeds.model;

import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe interval tree implementation for efficient card range lookups.
 * 
 * Performance Characteristics:
 * Insertion: O(log n) average case
 * Lookup: O(log n) average case
 * Space: O(n) for n card ranges
 * 
 * @author Julian Camilo
 * @version 1.0
 * @since 2025
 */
public class IntervalTree {

    private Node root;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private static final int DEFAULT_BATCH_SIZE = 100_000;

    private static class Node {
        CardRange range;
        long maxEnd;
        Node left;
        Node right;

        /**
         * Constructs a new node with the given card range.
         * 
         * @param range the card range to store in this node
         */
        Node(CardRange range) {
            this.range = range;
            this.maxEnd = range.getEndRange();
        }
    }

    /**
     * Inserts multiple card ranges in batches for memory efficiency.
     * 
     * Batch processing reduces lock contention
     * and memory allocation overhead for large datasets.
     * 
     * @param cardRanges the list of card ranges to insert
     * @throws IllegalArgumentException if cardRanges is null
     */
    public void insertAll(List<CardRange> cardRanges) {
        if (cardRanges == null) {
            throw new IllegalArgumentException("Card ranges list cannot be null");
        }

        lock.writeLock().lock();
        try {
            for (int i = 0; i < cardRanges.size(); i += DEFAULT_BATCH_SIZE) {
                int endIndex = Math.min(i + DEFAULT_BATCH_SIZE, cardRanges.size());
                List<CardRange> batch = cardRanges.subList(i, endIndex);

                for (CardRange range : batch) {
                    root = insertNode(root, range);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Inserts a single card range into the tree.
     * 
     * @param cardRange the card range to insert
     * @throws IllegalArgumentException if cardRange is null
     */
    public void insert(CardRange cardRange) {
        if (cardRange == null) {
            throw new IllegalArgumentException("Card range cannot be null");
        }

        lock.writeLock().lock();
        try {
            root = insertNode(root, cardRange);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Recursively inserts a card range into the subtree rooted at the given node.
     * 
     * @param node      the root of the subtree to insert into (can be null)
     * @param cardRange the card range to insert
     * @return the new root of the modified subtree
     */
    private Node insertNode(Node node, CardRange cardRange) {
        if (node == null) {
            return new Node(cardRange);
        }

        long newStart = cardRange.getStartRange();
        long nodeStart = node.range.getStartRange();

        if (newStart < nodeStart) {
            node.left = insertNode(node.left, cardRange);
        } else {
            node.right = insertNode(node.right, cardRange);
        }

        long leftMax = node.left != null ? node.left.maxEnd : Long.MIN_VALUE;
        long rightMax = node.right != null ? node.right.maxEnd : Long.MIN_VALUE;
        node.maxEnd = Math.max(node.range.getEndRange(), Math.max(leftMax, rightMax));

        return node;
    }

    /**
     * Finds the card range that contains the given PAN.
     * 
     * @param pan the Primary Account Number to search for
     * @return the matching card range, or null if no match is found
     */
    public CardRange findRange(long pan) {
        lock.readLock().lock();
        try {
            return findRangeRecursive(root, pan, null);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Clears all card ranges from the tree.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            root = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Recursively searches for the best matching card range for the given PAN.
     * 
     * @param node      the current node in the search
     * @param pan       the Primary Account Number to search for
     * @param bestMatch the best matching range found so far
     * @return the best matching card range, or null if no match is found
     */
    private CardRange findRangeRecursive(Node node, long pan, CardRange bestMatch) {
        if (node == null) {
            return bestMatch;
        }

        if (node.range.getStartRange() <= pan && pan <= node.range.getEndRange()) {
            // Conflict resolution: prefer smaller (more specific) ranges
            if (bestMatch == null ||
                    (node.range.getEndRange() - node.range.getStartRange()) < (bestMatch.getEndRange()
                            - bestMatch.getStartRange())) {
                bestMatch = node.range;
            }
        }

        if (node.left != null && node.left.maxEnd >= pan) {
            bestMatch = findRangeRecursive(node.left, pan, bestMatch);
        }

        return findRangeRecursive(node.right, pan, bestMatch);
    }
}
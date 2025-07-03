package com.example.threeds.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.threeds.exception.CardRangeNotFoundException;
import com.example.threeds.model.CardRange;
import com.example.threeds.model.PResMessage;
import com.example.threeds.repository.CardRangeRepository;

/**
 * Service layer for card range operations in the 3DS Server.
 * 
 * @author Julian Camilo
 * @version 1.0
 * @since 2025
 */
@Service
public class CardRangeService {

    private final CardRangeRepository cardRangeRepository;

    /**
     * Constructs a new CardRangeService with the required repository.
     * 
     * @param cardRangeRepository the repository for card range operations
     */
    @Autowired
    public CardRangeService(CardRangeRepository cardRangeRepository) {
        this.cardRangeRepository = cardRangeRepository;
    }

    /**
     * Saves all card ranges from a PRes message to the repository.
     * 
     * @param presMessage the PRes message containing card ranges to store
     * @throws IllegalArgumentException if presMessage is null or contains invalid
     *                                  data
     */
    public void savePResMessage(PResMessage presMessage) {
        if (presMessage == null) {
            throw new IllegalArgumentException("PRes message cannot be null");
        }

        if (presMessage.getCardRangeData() == null) {
            throw new IllegalArgumentException("Card range data cannot be null");
        }

        cardRangeRepository.saveAll(presMessage.getCardRangeData());
    }

    /**
     * Finds the card range that contains the specified PAN.
     * 
     * @param pan the Primary Account Number to search for
     * @return the matching card range
     * @throws CardRangeNotFoundException if no matching card range is found
     * @throws IllegalArgumentException   if pan is negative
     */
    public CardRange findByPAN(long pan) throws CardRangeNotFoundException {
        if (pan < 0) {
            throw new IllegalArgumentException("PAN cannot be negative: " + pan);
        }

        CardRange cardRange = cardRangeRepository.findCardRange(pan);
        if (cardRange == null) {
            throw new CardRangeNotFoundException("No matching card range found for PAN: " + pan);
        }
        return cardRange;
    }

    /**
     * Clears all card ranges from the repository.
     * 
     */
    public void clear() {
        cardRangeRepository.clear();
    }
}

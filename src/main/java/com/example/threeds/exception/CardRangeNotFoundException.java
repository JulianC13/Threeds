package com.example.threeds.exception;

/**
 * Exception thrown when a card range matching the specified PAN cannot be found.
 * This exception is used to indicate that the requested Primary Account Number
 * does not fall within any of the stored card ranges in the 3DS Server.
 * 
 * @author Julian Camilo
 * @version 1.0
 * @since 2025
 */
public class CardRangeNotFoundException extends Exception {
    
    /**
     * Constructs a new CardRangeNotFoundException with the specified detail message.
     * 
     * @param message the detail message explaining why the card range was not found
     */
    public CardRangeNotFoundException(String message) {
        super(message);
    }
}
package com.example.threeds.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.threeds.exception.CardRangeNotFoundException;
import com.example.threeds.model.CardRange;
import com.example.threeds.model.PResMessage;
import com.example.threeds.service.CardRangeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;

import java.util.Collections;

/**
 * REST Controller for 3DS Server operations.
 * 
 * @author Julian Camilo
 * @version 1.0
 * @since 2025
 */
@RestController
@RequestMapping("/api/3ds")
public class ThreeDSController {

    private static final Logger logger = LoggerFactory.getLogger(ThreeDSController.class);

    private final CardRangeService cardRangeService;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a new ThreeDSController with required dependencies.
     * 
     * @param cardRangeService the service for card range operations
     * @param objectMapper     the Jackson ObjectMapper for JSON processing
     */
    @Autowired
    public ThreeDSController(CardRangeService cardRangeService, ObjectMapper objectMapper) {
        this.cardRangeService = cardRangeService;
        this.objectMapper = objectMapper;
    }

    /**
     * Stores a PRes message containing card ranges.
     * 
     * @param presMessage the PRes message containing card range data
     * @return HTTP 200 OK if successful
     * @throws IllegalArgumentException if the PRes message contains invalid data
     */
    @PostMapping("/store")
    public ResponseEntity<Void> storePResMessage(@RequestBody @Valid PResMessage presMessage) {
        // Shuffle input to prevent unbalanced trees with ordered datasets
        // This is a key optimization that prevents StackOverflowError
        Collections.shuffle(presMessage.getCardRangeData());

        cardRangeService.savePResMessage(presMessage);
        return ResponseEntity.ok().build();
    }

    /**
     * Stores a PRes message with detailed performance timing.
     * 
     * @param json the JSON string representation of the PRes message
     * @return HTTP 200 OK if successful
     * @throws JsonMappingException    if JSON mapping fails
     * @throws JsonProcessingException if JSON processing fails
     */
    @PostMapping("/store-times")
    public ResponseEntity<?> storePResMessageWithTiming(@RequestBody String json)
            throws JsonMappingException, JsonProcessingException {
        long startTime = System.nanoTime();

        // deserialization time
        long deserializationStart = System.nanoTime();
        PResMessage presMessage = objectMapper.readValue(json, PResMessage.class);
        long deserializationEnd = System.nanoTime();

        // storage time
        long storageStart = System.nanoTime();
        Collections.shuffle(presMessage.getCardRangeData());
        cardRangeService.savePResMessage(presMessage);
        long storageEnd = System.nanoTime();

        long totalTime = System.nanoTime() - startTime;

        logger.info("POST /api/3ds/store timing (ms): total={}, deserialize={}, save={}",
                totalTime / 1_000_000,
                (deserializationEnd - deserializationStart) / 1_000_000,
                (storageEnd - storageStart) / 1_000_000);

        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves the card range matching the given PAN (Primary Account Number).
     * 
     * @param pan the Primary Account Number to search for
     * @return the matching CardRange with HTTP 200 OK
     * @throws CardRangeNotFoundException if no matching card range is found
     */
    @GetMapping("/method-url")
    public ResponseEntity<CardRange> findCardRangeByPAN(@RequestParam long pan) throws CardRangeNotFoundException {
        CardRange cardRange = cardRangeService.findByPAN(pan);
        return ResponseEntity.ok(cardRange);
    }
}
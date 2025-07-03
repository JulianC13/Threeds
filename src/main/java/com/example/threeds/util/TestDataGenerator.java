package com.example.threeds.util;

import com.example.threeds.model.CardRange;
import com.example.threeds.model.PResMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Utility class for generating test data for the 3DS Server application.
 * 
 * @author Julian Camilo
 * @version 1.0
 * @since 2025
 */
public class TestDataGenerator {

    /**
     * Generates a PResMessage with the specified number of card ranges.
     * Creates realistic card range data with non-overlapping BIN ranges,
     * proper 3DS method URLs, and valid protocol versions for testing purposes.
     *
     * @param rangeCount the number of card ranges to generate (e.g., 700000)
     * @return PResMessage containing realistic card range data with the specified
     *         number of ranges
     * @throws IllegalArgumentException if rangeCount is negative or zero
     */
    public static PResMessage generatePResMessage(int rangeCount) {
        if (rangeCount <= 0) {
            throw new IllegalArgumentException("Range count must be positive: " + rangeCount);
        }
        Random random = new Random();
        List<CardRange> ranges = new ArrayList<>();
        long startRange = 4000020000000000L;

        for (int i = 0; i < rangeCount; i++) {
            long rangeSize = 1_000_000L + random.nextInt(9_000_000);
            long endRange = startRange + rangeSize - 1;
            String url = "https://secure4.arcot.com/content-server/api/tds2/txn/browser/v1/tds-method/" + i;

            CardRange range = new CardRange(
                    startRange,
                    endRange,
                    "A",
                    "2.1.0",
                    url,
                    "2.1.0",
                    Arrays.asList("01", "02"));
            ranges.add(range);
            startRange = endRange + 1;
        }

        return new PResMessage(
                String.valueOf(random.nextInt(10_000_000)),
                "PRes",
                UUID.randomUUID().toString(),
                ranges);
    }
}
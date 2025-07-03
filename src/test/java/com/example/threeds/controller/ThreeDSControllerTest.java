package com.example.threeds.controller;

import com.example.threeds.model.CardRange;
import com.example.threeds.model.PResMessage;
import com.example.threeds.service.CardRangeService;
import com.example.threeds.util.TestDataGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the ThreeDSController REST API.
 * Validates storing and retrieving card ranges, error handling, and performance
 * with large datasets.
 *
 * @author Julian Camilo
 * @version 1.0
 * @since 2025
 */
@SpringBootTest
@AutoConfigureMockMvc
class ThreeDSControllerTest {
        @Autowired
        private MockMvc mockMvc;
        @Autowired
        private CardRangeService service;
        @Autowired
        private ObjectMapper objectMapper;

        /**
         * Clears the in-memory IntervalTree between tests to ensure isolation.
         */
        @BeforeEach
        void setUp() {
                service.clear();
        }

        /**
         * Tests storing a PResMessage and retrieving the corresponding CardRange by
         * PAN.
         *
         * @throws Exception if the request fails
         */
        @Test
        void testStoreAndFindCardRange() throws Exception {
                CardRange range = new CardRange(
                                4000020000000000L,
                                4000020009999999L,
                                "A",
                                "2.1.0",
                                "https://example.com/3ds",
                                "2.1.0",
                                Arrays.asList("01", "02"));
                PResMessage message = new PResMessage("123", "PRes", "uuid", Arrays.asList(range));
                mockMvc.perform(post("/api/3ds/store")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(message)))
                                .andExpect(status().isOk());
                mockMvc.perform(get("/api/3ds/method-url")
                                .param("pan", "4000020005000000"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.startRange").value(range.getStartRange()))
                                .andExpect(jsonPath("$.endRange").value(range.getEndRange()))
                                .andExpect(jsonPath("$.threeDSMethodURL").value(range.getThreeDSMethodURL()));
        }

        /**
         * Tests that posting an invalid card range returns a Bad Request error.
         *
         * @throws Exception if the request fails
         */
        @Test
        void testInvalidCardRangeInsertedViaPost() throws Exception {
                String invalidJson = """
                                    {
                                        "serialNum": "123",
                                        "messageType": "PRes",
                                        "dsTransID": "uuid",
                                        "cardRangeData": [
                                            {
                                                "startRange": "4000020009999999",
                                                "endRange": "4000020000000000",
                                                "actionInd": "A",
                                                "acsEndProtocolVersion": "2.1.0",
                                                "threeDSMethodURL": "https://example.com/3ds",
                                                "acsStartProtocolVersion": "2.1.0",
                                                "acsInfoInd": ["01", "02"]
                                            }
                                        ]
                                    }
                                """;
                mockMvc.perform(post("/api/3ds/store")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message")
                                                .value(org.hamcrest.Matchers.containsString(
                                                                "endRange (4000020000000000) must be greater than or equal to startRange (4000020009999999)")));
        }

        /**
         * Tests that the CardRange constructor throws an exception for invalid ranges.
         */
        @Test
        void testConstructorThrowsExceptionWhenEndRangeIsLessThanStartRange() {
                assertThrows(IllegalArgumentException.class, () -> new CardRange(
                                4000020009999999L,
                                4000020000000000L,
                                "A",
                                "2.1.0",
                                "https://example.com/3ds",
                                "2.1.0",
                                Arrays.asList("01", "02")));
        }

        /**
         * Tests that searching for a non-existent PAN returns a Not Found error.
         *
         * @throws Exception if the request fails
         */
        @Test
        void testFindCardRangeNotFound() throws Exception {
                CardRange range = new CardRange(
                                4000020000000000L,
                                4000020009999999L,
                                "A",
                                "2.1.0",
                                "https://example.com/3ds",
                                "2.1.0",
                                Arrays.asList("01", "02"));
                PResMessage message = new PResMessage("123", "PRes", "uuid", Arrays.asList(range));
                service.savePResMessage(message);
                mockMvc.perform(get("/api/3ds/method-url")
                                .param("pan", "9999999999999999"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message")
                                                .value("No matching card range found for PAN: 9999999999999999"))
                                .andExpect(jsonPath("$.status").value(404));
                mockMvc.perform(get("/api/3ds/method-url")
                                .param("pan", "4000020000000000"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.startRange").exists());
        }

        /**
         * Tests storing and retrieving a large dataset from a file, measuring
         * performance.
         *
         * @throws Exception if the request fails
         */
        @Test
        void testLoadHugeJsonFromFileAndStore() throws Exception {
                long testStart = System.nanoTime();
                long startLoad = System.nanoTime();
                ClassPathResource resource = new ClassPathResource("700k-pres.json.data");
                Path filePath = resource.getFile().toPath();
                String hugeJson = Files.readString(filePath);
                long loadTime = System.nanoTime() - startLoad;
                long startPost = System.nanoTime();
                mockMvc.perform(post("/api/3ds/store")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(hugeJson))
                                .andExpect(status().isOk());
                long postTime = System.nanoTime() - startPost;
                long startManualSave = System.nanoTime();
                PResMessage deserialized = objectMapper.readValue(hugeJson, PResMessage.class);
                service.savePResMessage(deserialized);
                long manualSaveTime = System.nanoTime() - startManualSave;
                long startGet = System.nanoTime();
                long testPan = deserialized.getCardRangeData().get(0).getStartRange() + 50000;
                mockMvc.perform(get("/api/3ds/method-url")
                                .param("pan", String.valueOf(testPan)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.startRange").exists());
                long getTime = System.nanoTime() - startGet;
                long testTotalTime = System.nanoTime() - testStart;
                System.out.println("==== Performance Summary ====");
                System.out.println("Data load time: " + loadTime / 1_000_000 + " ms");
                System.out.println("POST time (deserialization + insertion): " + postTime / 1_000_000 + " ms");
                System.out.println("Manual deserialization + insertion time: " + manualSaveTime / 1_000_000 + " ms");
                System.out.println("GET lookup time: " + getTime / 1_000_000 + " ms");
                System.out.println("Total test duration: " + testTotalTime / 1_000_000 + " ms");
        }

        /**
         * Tests performance logging for very large datasets, including serialization,
         * POST, manual deserialization, and GET lookup.
         *
         * @throws Exception if the request fails
         */
        @Test
        void testLargeDatasetPerformanceLogs() throws Exception {
                long testStart = System.nanoTime();
                long startLoad = System.nanoTime();
                PResMessage message = TestDataGenerator.generatePResMessage(700000);
                String json = objectMapper.writeValueAsString(message);
                long loadTime = System.nanoTime() - startLoad;
                long startPost = System.nanoTime();
                mockMvc.perform(post("/api/3ds/store-times")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isOk());
                long postTime = System.nanoTime() - startPost;
                long startManualSave = System.nanoTime();
                PResMessage deserialized = objectMapper.readValue(json, PResMessage.class);
                service.savePResMessage(deserialized);
                long manualSaveTime = System.nanoTime() - startManualSave;
                long startGet = System.nanoTime();
                long testPan = message.getCardRangeData().get(0).getStartRange() + 50000;
                mockMvc.perform(get("/api/3ds/method-url")
                                .param("pan", String.valueOf(testPan)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.startRange").exists());
                long getTime = System.nanoTime() - startGet;
                long testTotalTime = System.nanoTime() - testStart;
                System.out.println("==== Performance Summary ====");
                System.out.println("Data load time: " + loadTime / 1_000_000 + " ms");
                System.out.println("POST time (deserialization + insertion): " + postTime / 1_000_000 + " ms");
                System.out.println("Manual deserialization + insertion time: " + manualSaveTime / 1_000_000 + " ms");
                System.out.println("GET lookup time: " + getTime / 1_000_000 + " ms");
                System.out.println("Total test duration: " + testTotalTime / 1_000_000 + " ms");
        }

        /**
         * Tests storing and retrieving a large dataset, measuring insert and lookup
         * times.
         *
         * @throws Exception if the request fails
         */
        @Test
        void testLargeDatasetPerformance() throws Exception {
                PResMessage message = TestDataGenerator.generatePResMessage(100000);
                long startTime = System.nanoTime();
                mockMvc.perform(post("/api/3ds/store")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(message)))
                                .andExpect(status().isOk());
                long insertTime = System.nanoTime() - startTime;
                startTime = System.nanoTime();
                mockMvc.perform(get("/api/3ds/method-url")
                                .param("pan", "4000020500000000"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.startRange").exists());
                long lookupTime = System.nanoTime() - startTime;
                System.out.println("Insert time for 700k ranges: " + insertTime / 1_000_000 + "ms");
                System.out.println("Lookup time: " + lookupTime / 1_000_000 + "ms");
        }

        /**
         * Tests that an invalid PAN parameter (non-numeric) returns Bad Request.
         *
         * @throws Exception if the request fails
         */
        @Test
        void testInvalidPanParameter() throws Exception {
                mockMvc.perform(get("/api/3ds/method-url")
                                .param("pan", "invalid"))
                                .andExpect(status().isBadRequest());
        }

        /**
         * Tests that an empty PResMessage (no card ranges) is accepted.
         *
         * @throws Exception if the request fails
         */
        @Test
        void testEmptyPResMessage() throws Exception {
                PResMessage message = new PResMessage("123", "PRes", "uuid", Collections.emptyList());
                mockMvc.perform(post("/api/3ds/store")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(message)))
                                .andExpect(status().isOk());
        }

        /**
         * Tests that posting an invalid PResMessage (missing required fields) returns
         * Bad Request.
         *
         * @throws Exception if the request fails
         */
        @Test
        void testInvalidPResMessage() throws Exception {
                String invalidJson = "{\"serialNum\":\"123\"}";
                mockMvc.perform(post("/api/3ds/store")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson))
                                .andExpect(status().isBadRequest());
        }

        /**
         * Tests concurrent POST and GET requests to the store and lookup endpoints.
         * Ensures thread safety and correctness under concurrent access.
         *
         * @throws Exception if the request fails
         */
        @Test
        void testConcurrentStoreAndFind() throws Exception {
                ExecutorService executor = Executors.newFixedThreadPool(10);
                PResMessage message = TestDataGenerator.generatePResMessage(100000);
                String json = objectMapper.writeValueAsString(message);
                List<Future<Void>> futures = new ArrayList<>();
                for (int i = 0; i < 5; i++) {
                        mockMvc.perform(post("/api/3ds/store")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json))
                                        .andExpect(status().isOk());
                }
                for (int i = 0; i < 5; i++) {
                        futures.add(executor.submit(() -> {
                                mockMvc.perform(get("/api/3ds/method-url")
                                                .param("pan", "4000020500000000"))
                                                .andExpect(status().isOk())
                                                .andExpect(jsonPath("$.startRange").exists());
                                return null;
                        }));
                }
                for (Future<Void> future : futures) {
                        future.get(10, TimeUnit.SECONDS);
                }
                executor.shutdown();
                assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }

        /**
         * Tests high concurrency with many simultaneous GET and POST requests.
         * Verifies that the system remains stable and correct under heavy load.
         *
         * @throws Exception if the request fails
         */
        @Test
        void testHighConcurrencyReadWrite() throws Exception {
                PResMessage message = TestDataGenerator.generatePResMessage(10000);
                String json = objectMapper.writeValueAsString(message);
                mockMvc.perform(post("/api/3ds/store")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isOk());
                ExecutorService executor = Executors.newFixedThreadPool(20);
                List<Future<Void>> futures = new ArrayList<>();
                int readThreads = 150;
                int writeThreads = 50;
                long startTime = System.nanoTime();
                for (int i = 0; i < readThreads; i++) {
                        futures.add(executor.submit(() -> {
                                mockMvc.perform(get("/api/3ds/method-url")
                                                .param("pan", "4000020500000000"))
                                                .andExpect(status().isOk())
                                                .andExpect(jsonPath("$.startRange").exists());
                                return null;
                        }));
                }
                for (int i = 0; i < writeThreads; i++) {
                        futures.add(executor.submit(() -> {
                                PResMessage smallMessage = TestDataGenerator.generatePResMessage(100);
                                mockMvc.perform(post("/api/3ds/store")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(smallMessage)))
                                                .andExpect(status().isOk());
                                return null;
                        }));
                }
                for (Future<Void> future : futures) {
                        future.get(30, TimeUnit.SECONDS);
                }
                long duration = System.nanoTime() - startTime;
                System.out.println(
                                "Concurrent read/write test time (" + readThreads + " reads, " + readThreads
                                                + " writes): " + duration / 1_000_000 + "ms");
                mockMvc.perform(get("/api/3ds/method-url")
                                .param("pan", "4000020500000000"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.startRange").exists());
                executor.shutdown();
                assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }

        /**
         * Tests that PANs on the boundary of a card range are included in the result.
         *
         * @throws Exception if the request fails
         */
        @Test
        void testPanOnBoundaryIsIncluded() throws Exception {
                CardRange range = new CardRange(
                                4000020000000000L,
                                4000020009999999L,
                                "A",
                                "2.1.0",
                                "https://example.com/3ds",
                                "2.1.0",
                                Arrays.asList("01", "02"));
                PResMessage message = new PResMessage("123", "PRes", "uuid", List.of(range));
                service.savePResMessage(message);
                mockMvc.perform(get("/api/3ds/method-url")
                                .param("pan", "4000020000000000"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.startRange").value(range.getStartRange()));
                mockMvc.perform(get("/api/3ds/method-url")
                                .param("pan", "4000020009999999"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.startRange").value(range.getStartRange()));
        }

        /**
         * Tests that when multiple overlapping ranges exist, the smallest (most
         * specific) is chosen.
         *
         * @throws Exception if the request fails
         */
        @Test
        void testConflictResolutionChoosesSmallestRange() throws Exception {
                CardRange wider = new CardRange(4000020000000000L, 4000020009999999L, "A", "2.1.0",
                                "https://example.com/wide", "2.1.0", List.of("01"));
                CardRange narrower = new CardRange(4000020002000000L, 4000020002009999L, "A", "2.1.0",
                                "https://example.com/narrow", "2.1.0", List.of("01"));
                PResMessage message = new PResMessage("123", "PRes", "uuid", List.of(wider, narrower));
                service.savePResMessage(message);
                long testPan = 4000020002000500L;
                mockMvc.perform(get("/api/3ds/method-url")
                                .param("pan", String.valueOf(testPan)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.threeDSMethodURL").value("https://example.com/narrow"));
        }

        /**
         * Tests that duplicate card ranges are handled gracefully and do not cause
         * errors.
         *
         * @throws Exception if the request fails
         */
        @Test
        void testDuplicateRangesAreHandledGracefully() throws Exception {
                CardRange range = new CardRange(4000020000000000L, 4000020009999999L, "A", "2.1.0",
                                "https://example.com", "2.1.0", List.of("01"));
                PResMessage message = new PResMessage("123", "PRes", "uuid", List.of(range, range));
                mockMvc.perform(post("/api/3ds/store")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(message)))
                                .andExpect(status().isOk());
                mockMvc.perform(get("/api/3ds/method-url")
                                .param("pan", "4000020005000000"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.threeDSMethodURL").value("https://example.com"));
        }

        /**
         * Tests that the response excludes fields that are null in the CardRange
         * object.
         *
         * @throws Exception if the request fails
         */
        @Test
        void testResponseExcludesNullFields() throws Exception {
                CardRange range = new CardRange(4000020000000000L, 4000020009999999L, null, null, "https://example.com",
                                null, null);
                PResMessage message = new PResMessage("123", "PRes", "uuid", List.of(range));
                service.savePResMessage(message);
                mockMvc.perform(get("/api/3ds/method-url")
                                .param("pan", "4000020005000000"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.actionInd").doesNotExist())
                                .andExpect(jsonPath("$.acsEndProtocolVersion").doesNotExist());
        }
}
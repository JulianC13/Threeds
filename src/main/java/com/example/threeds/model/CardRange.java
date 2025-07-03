package com.example.threeds.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a card range with associated 3DS method URL and metadata.
 *
 * JSON Structure:
 * {
 * "startRange": "4000020000000000",
 * "endRange": "4000020009999999",
 * "actionInd": "A",
 * "acsEndProtocolVersion": "2.1.0",
 * "threeDSMethodURL": "https://example.com/3ds",
 * "acsStartProtocolVersion": "2.1.0",
 * "acsInfoInd": ["01", "02"]
 * }
 * 
 * @author Julian Camilo
 * @version 1.0
 * @since 2025
 */

@Data
@NoArgsConstructor
@JsonIgnoreProperties(value = { "actionInd", "acsEndProtocolVersion", "acsStartProtocolVersion",
        "acsInfoInd" }, allowGetters = true, ignoreUnknown = true)
public class CardRange {

    /**
     * The starting BIN (Bank Identification Number) of this card range.
     * Must be less than or equal to endRange.
     */
    @JsonProperty("startRange")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @NotNull
    private long startRange;

    /**
     * The ending BIN (Bank Identification Number) of this card range.
     * Must be greater than or equal to startRange.
     */
    @JsonProperty("endRange")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @NotNull
    private long endRange;

    private String actionInd;

    private String acsEndProtocolVersion;

    /**
     * The 3DS method URL associated with this card range.
     * This is the primary data used by the 3DS server for authentication.
     */
    @JsonProperty("threeDSMethodURL")
    private String threeDSMethodURL;

    private String acsStartProtocolVersion;

    private List<String> acsInfoInd;

    /**
     * Constructs a new CardRange with validation.
     * This constructor is used by Jackson for JSON
     * deserialization. The validation ensures data integrity without
     * significant performance impact.
     * 
     * @param startRange              the starting BIN of the card range
     * @param endRange                the ending BIN of the card range
     * @param actionInd               the action indicator
     * @param acsEndProtocolVersion   the ACS end protocol version
     * @param threeDSMethodURL        the 3DS method URL
     * @param acsStartProtocolVersion the ACS start protocol version
     * @param acsInfoInd              the list of ACS information indicators
     * @throws IllegalArgumentException if endRange is less than startRange
     */
    @JsonCreator
    public CardRange(
            @JsonProperty("startRange") long startRange,
            @JsonProperty("endRange") long endRange,
            @JsonProperty("actionInd") String actionInd,
            @JsonProperty("acsEndProtocolVersion") String acsEndProtocolVersion,
            @JsonProperty("threeDSMethodURL") String threeDSMethodURL,
            @JsonProperty("acsStartProtocolVersion") String acsStartProtocolVersion,
            @JsonProperty("acsInfoInd") List<String> acsInfoInd) {

        // Validate that endRange is greater than or equal to startRange
        if (endRange < startRange) {
            throw new IllegalArgumentException(
                    String.format("Invalid card range: endRange (%d) must be greater than or equal to startRange (%d)",
                            endRange, startRange));
        }

        this.startRange = startRange;
        this.endRange = endRange;
        this.actionInd = actionInd;
        this.acsEndProtocolVersion = acsEndProtocolVersion;
        this.threeDSMethodURL = threeDSMethodURL;
        this.acsStartProtocolVersion = acsStartProtocolVersion;
        this.acsInfoInd = acsInfoInd;
    }

    @Override
    public String toString() {
        return "CardRange{" +
                "startRange=" + startRange +
                ", endRange=" + endRange +
                ", threeDSMethodURL='" + threeDSMethodURL + '\'' +
                '}';
    }
}

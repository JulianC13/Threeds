package com.example.threeds.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(value = { "serialNum", "messageType", "dsTransID" }, allowGetters = true, ignoreUnknown = true)
public class PResMessage {

    @JsonProperty("serialNum")
    private String serialNum;

    @JsonProperty("messageType")
    private String messageType;

    @JsonProperty("dsTransID")
    private String dsTransID;

    @JsonProperty("cardRangeData")
    @NotNull
    private List<@Valid CardRange> cardRangeData;

    @Override
    public String toString() {
        return "PResMessage{cardRangeDataSize=" + (cardRangeData != null ? cardRangeData.size() : 0) + "}";
    }
}

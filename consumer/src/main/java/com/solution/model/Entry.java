package com.solution.model;

public class Entry {
    private final String messageId;
    private Integer consumerId;
    private Integer handledData;

    public Entry(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageId() {
        return messageId;
    }

    public Integer getHandledData() {
        return handledData;
    }

    public void setHandledData(Integer handledData) {
        this.handledData = handledData;
    }

    public Integer getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(Integer consumerId) {
        this.consumerId = consumerId;
    }
}
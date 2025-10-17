package com.n9.shared.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.n9.shared.MessageProtocol;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageEnvelope {

    @JsonProperty(MessageProtocol.Keys.TYPE)
    private String type;

    @JsonProperty(MessageProtocol.Keys.CORRELATION_ID)
    private String correlationId;

    @JsonProperty(MessageProtocol.Keys.SESSION_ID)
    private String sessionId;

    @JsonProperty(MessageProtocol.Keys.PAYLOAD)
    private Object payload; // Sử dụng Object thay vì JsonNode

    @JsonProperty(MessageProtocol.Keys.ERROR)
    private ErrorInfo error;

    // Constructors, Getters, Setters...
    public MessageEnvelope() {}

    public MessageEnvelope(String type, String correlationId, Object payload) {
        this.type = type;
        this.correlationId = correlationId;
        this.payload = payload;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }
    public ErrorInfo getError() { return error; }
    public void setError(ErrorInfo error) { this.error = error; }
}
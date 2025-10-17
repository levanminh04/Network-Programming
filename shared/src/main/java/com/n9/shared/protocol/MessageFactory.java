package com.n9.shared.protocol;

import com.n9.shared.util.IdUtils; // Giả sử bạn có class này
import com.n9.shared.MessageProtocol;

public class MessageFactory {

    public static MessageEnvelope createRequest(String type, Object payload) {
        return new MessageEnvelope(type, IdUtils.generateCorrelationId(), payload);
    }

    public static MessageEnvelope createResponse(MessageEnvelope request, String responseType, Object payload) {
        MessageEnvelope response = new MessageEnvelope(responseType, request.getCorrelationId(), payload);
        response.setSessionId(request.getSessionId());
        return response;
    }

    public static MessageEnvelope createErrorResponse(MessageEnvelope request, String code, String message) {
        ErrorInfo error = new ErrorInfo(code, message);
        MessageEnvelope response = new MessageEnvelope(MessageProtocol.Type.SYSTEM_ERROR, request.getCorrelationId(), null);
        response.setError(error);
        return response;
    }

    public static MessageEnvelope createNotification(String type, Object payload) {
        return new MessageEnvelope(type, null, payload);
    }
}
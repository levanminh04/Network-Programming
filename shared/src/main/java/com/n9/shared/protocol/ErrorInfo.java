package com.n9.shared.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorInfo {
    private String code;
    private String message;

    // Constructor rỗng cho Jackson
    public ErrorInfo() {}

    public ErrorInfo(String code, String message) {
        this.code = code;
        this.message = message;
    }

    // Getters and Setters...
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
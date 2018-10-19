package com.bytegen.common.http.exception;

import java.io.IOException;

/**
 * User: xiang
 * Date: 2018/10/19
 * Desc:
 */
public class HttpRequestException extends RuntimeException {

    private String serviceName;
    private String displayMsg;
    private String debugMsg;

    private Object data;

    public HttpRequestException(String serviceName) {
        this(serviceName, null, null, null);
    }

    public HttpRequestException(String serviceName, String debugMsg) {
        this(serviceName, null, debugMsg, null);
    }

    public HttpRequestException(String serviceName, String displayMsg, String debugMsg) {
        this(serviceName, displayMsg, debugMsg, null);
    }

    public HttpRequestException(String serviceName, String displayMsg, String debugMsg, Object data) {
        super(debugMsg);
        this.serviceName = serviceName;
        this.displayMsg = displayMsg;
        this.debugMsg = debugMsg;

        this.data = data;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getDisplayMsg() {
        return displayMsg;
    }

    public String getDebugMsg() {
        return debugMsg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}

package com.bytegen.common.http.exception;

import java.io.IOException;

/**
 * User: xiang
 * Date: 2018/10/19
 * Desc:
 */
public class Not2xxException extends IOException {

    public Not2xxException() {
        super();
    }

    public Not2xxException(String message) {
        super(message);
    }

    public Not2xxException(String message, Throwable cause) {
        super(message, cause);
    }
}

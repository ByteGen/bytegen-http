package com.bytegen.common.http;

import java.util.Locale;

/**
 * User: xiang
 * Date: 2018/10/19
 * Desc:
 */
public interface HttpParam {

    public static final Locale DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE;

    public static final String TRACE_ID = "trace_id";
    public static final String APP_ID = "app_id";
    public static final String APP_SECRET = "app_secret";

    public static final String SIGN = "sign";
    public static final String TIMESTAMP = "timestamp";
    public static final String NONCE = "nonce";

    public static final String JSON_KEY = "json_body";

}

package org.jexpress.mockservice.app;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.summerboot.jexpress.nio.server.SessionContext;

import java.util.Properties;
import java.util.concurrent.TimeUnit;


public class ResponseSettings {
    public static final String KEY_RESPONSE_STATUS_CODE = "Response_Status_Code";
    public static final String KEY_RESPONSE_DELAY_SECOND = "Response_Delay_Second";
    public static final String KEY_RUN_RESPONSE_AS_JS = "Run_Response_File_As_JavaScript";
    public static final String KEY_JS_AS_SWITCH = "JavaScript_Response_As_Switch";

    public static final String RSPONSE_HEADER_FILE_CONTENT = """
            ####################
            # Response Ctrl    #
            ####################
            Response_Status_Code=200  
            Response_Delay_Second=0
            Run_Response_File_As_JavaScript=false
            JavaScript_Response_As_Switch=true
            
            ####################
            # Response Headers #
            ####################
            #header1=value1
            """;

    private final HttpResponseStatus responseStatus;
    private final int responseDelaySecond;
    private final boolean runResponseFileAsJS;
    private final boolean jsResponseAsSwitch;

    public ResponseSettings(Properties properties, SessionContext context) {
        if (properties == null) {
            properties = new Properties();
        }
        String v = properties.getProperty(KEY_RESPONSE_STATUS_CODE);
        if (v != null) {
            int code = Integer.parseInt(v);
            responseStatus = HttpResponseStatus.valueOf(code);
            properties.remove(KEY_RESPONSE_STATUS_CODE);
        } else {
            responseStatus = HttpResponseStatus.OK;
        }

        v = properties.getProperty(KEY_RESPONSE_DELAY_SECOND);
        if (v != null) {
            responseDelaySecond = Integer.parseInt(v);
            properties.remove(KEY_RESPONSE_DELAY_SECOND);
        } else {
            responseDelaySecond = 0;
        }

        v = properties.getProperty(KEY_RUN_RESPONSE_AS_JS);
        if (v != null) {
            runResponseFileAsJS = Boolean.parseBoolean(v);
            properties.remove(KEY_RUN_RESPONSE_AS_JS);
        } else {
            runResponseFileAsJS = false;
        }

        v = properties.getProperty(KEY_JS_AS_SWITCH);
        if (v != null) {
            jsResponseAsSwitch = Boolean.parseBoolean(v);
            properties.remove(KEY_JS_AS_SWITCH);
        } else {
            jsResponseAsSwitch = true;
        }

        for (Object key : properties.keySet()) {
            Object value = properties.get(key);
            context.responseHeader(key.toString(), value);
        }
        context.status(responseStatus);
    }

    public void apply() {
        if (getResponseDelaySecond() > 0) {
            try {
                TimeUnit.SECONDS.sleep(getResponseDelaySecond());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public HttpResponseStatus getResponseStatus() {
        return responseStatus;
    }

    public int getResponseDelaySecond() {
        return responseDelaySecond;
    }

    public boolean isRunResponseFileAsJS() {
        return runResponseFileAsJS;
    }

    public boolean isJsResponseAsSwitch() {
        return jsResponseAsSwitch;
    }
}

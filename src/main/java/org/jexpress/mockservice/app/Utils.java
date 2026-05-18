package org.jexpress.mockservice.app;

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import org.apache.commons.lang3.StringUtils;
import org.graalvm.polyglot.Context;
import org.summerboot.jexpress.boot.BootConstant;
import org.summerboot.jexpress.nio.server.NioHttpUtil;
import org.summerboot.jexpress.nio.server.SessionContext;
import org.summerboot.jexpress.security.JwtUtil;
import org.summerboot.jexpress.security.SecurityUtil;
import org.summerboot.jexpress.security.auth.AuthConfig;
import org.summerboot.jexpress.security.auth.Caller;
import org.summerboot.jexpress.security.auth.RoleMapping;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.summerboot.jexpress.util.FormatterUtil.EMPTY_STR_ARRAY;

/**
 * @author 魏泽北
 */
public class Utils {


    public static String escape4Filename(String s) {
        return SecurityUtil.escape4Filename(s);
    }

    public static Properties loadProperties(String fileName, boolean createIfNotExist) throws IOException {
        File fileEntry = new File(escape4Filename(fileName)).getAbsoluteFile();
        if (!fileEntry.exists()) {
            if (createIfNotExist) {
                fileEntry.getParentFile().mkdirs();
                //fileEntry.createNewFile();
                try (FileWriter writer = new FileWriter(fileEntry);) {
                    writer.write(ResponseSettings.RSPONSE_HEADER_FILE_CONTENT);
                }
            }
            return null;
        }
        Properties props = new Properties();
        try (InputStream is = new FileInputStream(fileEntry); InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);) {
            props.load(isr);
        }
        return props;
    }

    public static String loadFileContent(String fileName, boolean createIfNotExist, String defaultContent) throws IOException {
        File fileEntry = new File(escape4Filename(fileName)).getAbsoluteFile();
        if (!fileEntry.exists()) {
            if (createIfNotExist) {
                fileEntry.getParentFile().mkdirs();
                if (StringUtils.isBlank(defaultContent)) {
                    fileEntry.createNewFile();
                } else {
                    try (FileWriter writer = new FileWriter(fileEntry);) {
                        writer.write(defaultContent);
                    }
                }
            }
            return null;
        }
        return Files.readString(Paths.get(fileEntry.getAbsolutePath()));
    }


    public static String generateJWT(int ttlMinutes, String userId, String role, String... audience) {
        Collection<String> groupNames = null;
        if (role != null) {
            RoleMapping rm = AuthConfig.cfg.getRole(role);
            if (rm != null) {
                groupNames = rm.getGroups();
            }
        }
        if (groupNames == null && audience != null) {
            groupNames = List.of(audience);
        }
        if (groupNames == null) {
            groupNames = Set.of("group1", "group2");
        }

        JwtBuilder builder = Jwts.builder()
                .id(userId + System.currentTimeMillis())
                .issuer("MockService")
                .subject(userId);
        builder.audience().add(groupNames);
        return JwtUtil.createJWT(AuthConfig.cfg.getJwtSigningKey(), builder, Duration.ofMinutes(ttlMinutes));
    }

    private static final String FUNCTION_NAME = "requestRouter";

    public static final String JS_FILE_CONTENT = """
            /**
             * Router method invoked by MockService to determine which response file to load next.
             * This method will not be invoked if Run_Response_File_As_JavaScript=false in .properties, 
             *
             * @param {remoteAddress} - String
             * @param {uid} - String (from request header: Authorization = Bearer ${jwt})
             * @param {requestHeader} - Map<String, String>
             * @param {queryParam} - Map<String, List<String>>
             * @param {requestBody} - String
             * @returns a string as a route suffix, which will be appended to the end of the response file name, and the contents of that response file will be sent back as the response.
             * 
             * example:
             * var headerValue = requestHeader["Content-Type"];
             * var queryParamValue = queryParam.key1[0];
             * var jsonObject = JSON.parse(requestBody);
             * return headerValue.includes('json') ? 'json' : 'xml';
            */
            function %s(remoteAddress, uid, requestHeader, queryParam, requestBody) {
                return uid;
            }
            """.formatted(FUNCTION_NAME);

    public static String javascriptRuleEngine(String jsCode, List<Map.Entry<String, String>> headers, Map<String, List<String>> queryParam, String requestBody, final SessionContext context) throws ScriptException, NoSuchMethodException {
        if (StringUtils.isBlank(jsCode)) {
            return null;
        }

        Map<String, String> requestHeader = new HashMap();
        for (Map.Entry<String, String> entry : headers) {
            requestHeader.put(entry.getKey(), entry.getValue());
        }
        String key = NioHttpUtil.HTTP_HEADER_AUTH_TOKEN;
        if (requestHeader.containsKey(key)) {
            requestHeader.put(key, "***");
        }

        String[] jsLines = StringUtils.isBlank(jsCode) ? EMPTY_STR_ARRAY : jsCode.split("\\r?\\n");
        StringBuilder sb = new StringBuilder();
        for (String jsLine : jsLines) {
            if (StringUtils.isBlank(jsLine)) {
                continue;
            }
            String trimed = jsLine.trim();
            if (trimed.startsWith("//") || trimed.startsWith("#")) {
                continue;
            }
            sb.append(jsLine).append(BootConstant.BR);
        }
        if (StringUtils.isBlank(sb.toString())) {
            return null;
        }
        String jsFunctionCode = sb.toString();
        context.memo("jsCode" + BootConstant.BR, jsFunctionCode);

        String remoteAddress = null;
        String uid = null;
        if (context != null) {
            InetSocketAddress saddr = (InetSocketAddress) context.remoteIP();
            InetAddress addr = saddr.getAddress();
            remoteAddress = addr.getCanonicalHostName();
            Caller caller = context.caller();
            if (caller != null) {
                uid = caller.getUid();
            }
        }

        ScriptEngine graalEngine = GraalJSScriptEngine.create(null, Context.newBuilder("js").allowAllAccess(true));
        graalEngine.eval(jsFunctionCode);
        Invocable invocable = (Invocable) graalEngine;
        Object result = invocable.invokeFunction(FUNCTION_NAME, remoteAddress, uid, requestHeader, queryParam, requestBody);
        return result == null ? null : result.toString();
    }
}

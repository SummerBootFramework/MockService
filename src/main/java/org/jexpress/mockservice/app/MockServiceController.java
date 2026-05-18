package org.jexpress.mockservice.app;

import com.google.inject.Singleton;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.summerboot.jexpress.boot.annotation.Controller;
import org.summerboot.jexpress.nio.server.SessionContext;
import org.summerboot.jexpress.nio.server.domain.Err;
import org.summerboot.jexpress.nio.server.domain.ServiceRequest;
import org.summerboot.jexpress.security.SecurityUtil;

import javax.script.ScriptException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author 魏泽北
 */
@Singleton
@Controller
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class MockServiceController {

    // curl -v -k https://localhost:8211/service1/action1 -H "Accept: application/json" -H "Authorization: Bearer abcdefg"
    // curl -v -k https://localhost:8211/service1/action1 -H "Accept: application/json" -H "Authorization: Bearer abcdefg" -X POST -d '{"name":"John Done","title":"Boss"}'
    @GET
    @POST
    @PUT
    @PATCH
    @DELETE
    @Path("")
    public String mockService(final String body, ServiceRequest serviceRequest, @Parameter(hidden = true) final SessionContext context) throws IOException, ScriptException, NoSuchMethodException {
        // 1. get request URI
        Map<String, List<String>> queryParam = serviceRequest.getQueryParams();
        String action = serviceRequest.getHttpRequestPath();
        // 2. check whitelist
        Set<String> whiteList = WhitelistConfig.cfg.getWhteList();
        if (whiteList != null && !whiteList.contains(action)) {
            boolean match = false;
            for (String whiteListPath : whiteList) {
                if (action.startsWith(whiteListPath) || SecurityUtil.matches(action, whiteListPath)) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                Err e = new Err(400, "", "URI " + action + " is not in whitelist", null, null);
                context.error(e).status(HttpResponseStatus.FORBIDDEN);
                return null;
            }
        }
        // 3. load root properties
        String filePath = "mock_response" + action + "_" + context.method();
        int level = 1;
        return runMock(body, queryParam, context, filePath, level);
    }

    protected String runMock(final String body, Map<String, List<String>> queryParam, final SessionContext context, final String filePath, int level) throws IOException, ScriptException, NoSuchMethodException {
        String fileName = filePath + ".properties";
        context.memo(level + ".properties.file", fileName);
        Properties properties1 = Utils.loadProperties(fileName, true);
        ResponseSettings responseSettings1 = new ResponseSettings(properties1, context);
        responseSettings1.apply();
        boolean runResponseFileAsJS = responseSettings1.isRunResponseFileAsJS();
        if (!runResponseFileAsJS) {
            // 1. load root txt file
            fileName = filePath + ".txt";
            context.memo(level + ".txt.file", fileName);
            String defaultFileContent = null;
            return Utils.loadFileContent(fileName, true, defaultFileContent);
        }

        // 3. load root js file
        fileName = filePath + ".js";
        context.memo(level + ".js.file", fileName);
        String jsCode = Utils.loadFileContent(fileName, true, Utils.JS_FILE_CONTENT);
        String jsResponse = Utils.javascriptRuleEngine(jsCode, context.requestHeaders().entries(), queryParam, body, context);
        if (!responseSettings1.isJsResponseAsSwitch()) {
            return jsResponse;
        }
        String subFilePath = filePath + "_case_" + jsResponse;
        return runMock(body, queryParam, context, subFilePath, ++level);
    }

}

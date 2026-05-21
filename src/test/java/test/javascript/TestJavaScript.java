/*
 * Copyright 2005-2026 Du Law Office - jExpress, The Summer Boot Framework Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://apache.org
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package test.javascript;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import org.graalvm.polyglot.Context;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 *
 * @author DuXiao
 */
public class TestJavaScript {

    public TestJavaScript() {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    public static final String requestBody = """
            {"result":true, "count":42}
            """;
    public static final String jsCode = """                        
            function function1(arg1, arg2) { 
                print('arg1='+arg1+', arg2='+arg2);
                return "from JS1: " + 'arg1='+arg1+', arg2='+arg2; 
            }
            
            var MyApp = {
              myVar1: 0,
              myVar2: 'abc',
            
             function2: function(arg1, arg2) { 
                print('arg1='+arg1+', arg2='+arg2);
                MyApp.myVar1++;
                return "from JS2: " + 'arg1='+arg1+', arg2='+arg2 + ", " + MyApp.myVar1 + "=" + MyApp.myVar2; 
             },
            
             function3: function(requestBodyString, header) {
                var json = JSON.parse(requestBodyString);
                print('result=' + json.result + ', count=' + json.count);
            
                print('header=' + header.keys().length);
            
                MyApp.myVar1++;                 
                return "from JS3: " + "result="+json.result+", count="+json.count + ", " + MyApp.myVar1 + "=" + MyApp.myVar2 + ", header.key1="+header["Content-Type"] + ", header.ke2="+header["Content-Length"]; 
             }
            }
            """;

    @Test
    public void testJavaScript() throws ScriptException, NoSuchMethodException, JsonProcessingException {
        //ScriptEngine graalEngine = new ScriptEngineManager().getEngineByName("Graal.js");
        ScriptEngine graalEngine = GraalJSScriptEngine.create(null,
                Context.newBuilder("js")
                        .allowAllAccess(true)
        );
        graalEngine.eval(jsCode);
        Invocable invocable = (Invocable) graalEngine;

        Object result1 = invocable.invokeFunction("function1", "v1", "v2");
        System.out.println("result1=" + result1);
        assertEquals(result1, "from JS1: arg1=v1, arg2=v2");

        Object thiz = graalEngine.get("MyApp");

        Object result2 = invocable.invokeMethod(thiz, "function2", "v3", "v4");
        System.out.println("result2=" + result2);
        assertEquals(result2, "from JS2: arg1=v3, arg2=v4, 1=abc");

        Object result3 = invocable.invokeMethod(thiz, "function2", "v5", "v6");
        System.out.println("result3=" + result3);
        assertEquals(result3, "from JS2: arg1=v5, arg2=v6, 2=abc");

        // test request body and header
        List<Map.Entry<String, String>> listOfEntry = new ArrayList();
        Map.Entry<String, String> e = new AbstractMap.SimpleEntry("Content-Type", "application/json;charset=UTF-8");
        listOfEntry.add(e);
        e = new AbstractMap.SimpleEntry("Content-Length", "27");
        listOfEntry.add(e);
        Map<String, String> map = new HashMap();
        for (Map.Entry<String, String> entry : listOfEntry) {
            map.put(entry.getKey(), entry.getValue());
        }

        Object result4 = invocable.invokeMethod(thiz, "function3", requestBody, map);
        System.out.println("result4=" + result4);
        assertEquals(result4, "from JS3: result=true, count=42, 3=abc, header.key1=application/json;charset=UTF-8, header.ke2=27");
    }
}

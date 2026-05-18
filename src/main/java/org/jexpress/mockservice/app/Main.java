package org.jexpress.mockservice.app;

import org.summerboot.jexpress.boot.SummerApplication;
import org.summerboot.jexpress.boot.annotation.Version;

/**
 * @author 魏泽北
 */
@Version(value = "MockService1.1.0")
public class Main {

    public static void main(String[] args) {
        SummerApplication.run();
        String usage = """
                curl -v -k https://localhost:8211/mockservice/jwt/1440 -X POST -H "application/x-www-form-urlencoded; charset=UTF-8" -X POST -d "userId=myId&role=myRole&group=myGroup"
                """;
        System.out.println(usage);
    }
}

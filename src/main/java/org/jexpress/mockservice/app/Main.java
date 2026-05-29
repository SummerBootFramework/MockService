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

package org.jexpress.mockservice.app;

import org.summerboot.jexpress.annotation.Version;
import org.summerboot.jexpress.boot.SummerApplication;

/**
 * @author 魏泽北
 */
@Version(value = "MockService1.2.0")
public class Main {

    public static void main(String[] args) {
        SummerApplication.run();
        String usage = """
                curl -v -k https://localhost:8211/mockservice/jwt/1440 -X POST -H "application/x-www-form-urlencoded; charset=UTF-8" -X POST -d "userId=myId&role=myRole&group=myGroup"
                """;
        System.out.println(usage);
    }
}

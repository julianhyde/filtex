/*
 * Licensed to Julian Hyde under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hydromatic.filtex;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static net.hydromatic.filtex.Ft.ft;
import static net.hydromatic.filtex.Matchers.isAst;
import static net.hydromatic.filtex.Matchers.isLiteral;

/**
 * Tests the parser.
 */
public class ParserTest {
  @Test void testString() {
    ft(TypeFamily.STRING, "\"abc\"")
        .assertParse(isAst("abc"));
  }

  @Test void testNumber() {
    ft(TypeFamily.NUMBER, "20")
        .assertParse(isLiteral(BigDecimal.valueOf(20), "20"));
    ft(TypeFamily.NUMBER, "20,30")
        .assertParse(isAst("{20,30}"));
    ft(TypeFamily.NUMBER, "[0,20]")
        .assertParse(isAst("[0,20]"));
    ft(TypeFamily.NUMBER, "[0,20],>30")
        .assertParse(isAst("{[0,20],(30,inf)}"));
  }
}

// End ParserTest.java

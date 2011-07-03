/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.isis.viewer.wicket.ui.util;

import org.apache.wicket.behavior.IBehavior;

public class EvenOrOddCssClassAppenderFactory {

    private enum EvenOrOdd {
        EVEN, ODD;
        private EvenOrOddCssClassAppenderFactory.EvenOrOdd next() {
            return this == EVEN ? ODD : EVEN;
        }

        private String className() {
            return this.name().toLowerCase();
        }
    }

    private EvenOrOddCssClassAppenderFactory.EvenOrOdd eo = EvenOrOdd.EVEN;

    public IBehavior nextClass() {
        final String className = eo.className();
        eo = eo.next();
        return new CssClassAppender(className);
    }
}
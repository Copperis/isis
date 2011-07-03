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

package org.apache.isis.core.metamodel.facets.object.immutable;

import org.apache.isis.applib.events.UsabilityEvent;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.facetapi.Facet;
import org.apache.isis.core.metamodel.facetapi.FacetHolder;
import org.apache.isis.core.metamodel.facets.SingleWhenValueFacetAbstract;
import org.apache.isis.core.metamodel.facets.When;
import org.apache.isis.core.metamodel.interactions.UsabilityContext;

public abstract class ImmutableFacetAbstract extends SingleWhenValueFacetAbstract implements ImmutableFacet {

    public static Class<? extends Facet> type() {
        return ImmutableFacet.class;
    }

    public ImmutableFacetAbstract(final When value, final FacetHolder holder) {
        super(type(), holder, value);
    }

    @Override
    public String disables(final UsabilityContext<? extends UsabilityEvent> ic) {
        final ObjectAdapter target = ic.getTarget();
        return disabledReason(target);
    }

    public String disabledReason(final ObjectAdapter targetAdapter) {
        if (value() == When.ALWAYS) {
            return "Always hidden";
        } else if (value() == When.NEVER) {
            return null;
        }

        // remaining tests depend on target in question.
        if (targetAdapter == null) {
            return null;
        }

        if (value() == When.UNTIL_PERSISTED) {
            return targetAdapter.isTransient() ? "Immutable until persisted" : null;
        } else if (value() == When.ONCE_PERSISTED) {
            return targetAdapter.isPersistent() ? "Immutable once persisted" : null;
        }
        return null;
    }

}

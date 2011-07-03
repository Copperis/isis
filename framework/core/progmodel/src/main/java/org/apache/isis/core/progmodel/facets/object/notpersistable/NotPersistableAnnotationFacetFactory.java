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

package org.apache.isis.core.progmodel.facets.object.notpersistable;

import org.apache.isis.applib.annotation.NotPersistable;
import org.apache.isis.core.metamodel.facetapi.FacetHolder;
import org.apache.isis.core.metamodel.facetapi.FacetUtil;
import org.apache.isis.core.metamodel.facetapi.FeatureType;
import org.apache.isis.core.metamodel.facets.AnnotationBasedFacetFactoryAbstract;
import org.apache.isis.core.metamodel.facets.object.notpersistable.InitiatedBy;
import org.apache.isis.core.metamodel.facets.object.notpersistable.NotPersistableFacet;

public class NotPersistableAnnotationFacetFactory extends AnnotationBasedFacetFactoryAbstract {

    public NotPersistableAnnotationFacetFactory() {
        super(FeatureType.OBJECTS_ONLY);
    }

    @Override
    public void process(final ProcessClassContext processClassContaxt) {
        final NotPersistable annotation = getAnnotation(processClassContaxt.getCls(), NotPersistable.class);
        FacetUtil.addFacet(create(annotation, processClassContaxt.getFacetHolder()));
    }

    @Override
    public void process(final ProcessMethodContext processMethodContext) {
        final NotPersistable annotation = getAnnotation(processMethodContext.getMethod(), NotPersistable.class);
        FacetUtil.addFacet(create(annotation, processMethodContext.getFacetHolder()));
    }

    private NotPersistableFacet create(final NotPersistable annotation, final FacetHolder holder) {
        return annotation != null ? new NotPersistableFacetAnnotation(InitiatedBy.decodeBy(annotation.value()), holder)
            : null;
    }

}

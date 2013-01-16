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

package org.apache.isis.viewer.scimpi.dispatcher.view.display;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.isis.applib.annotation.Where;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociation;
import org.apache.isis.core.runtime.system.context.IsisContext;
import org.apache.isis.viewer.scimpi.ForbiddenException;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TagProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

/**
 * Element to include another file that will display an object.
 */
public class IncludeObject extends AbstractElementProcessor {

    // REVIEW: should provide this rendering context, rather than hardcoding.
    // the net effect currently is that class members annotated with 
    // @Hidden(where=Where.ANYWHERE) or @Disabled(where=Where.ANYWHERE) will indeed
    // be hidden/disabled, but will be visible/enabled (perhaps incorrectly) 
    // for any other value for Where
    private final Where where = Where.ANYWHERE;

    @Override
    public void process(final TagProcessor tagProcessor) {
        final String path = tagProcessor.getOptionalProperty("file");
        String id = tagProcessor.getOptionalProperty(OBJECT);
        final String fieldName = tagProcessor.getOptionalProperty(FIELD);
        ObjectAdapter object = tagProcessor.getContext().getMappedObjectOrResult(id);
        if (fieldName != null) {
            final ObjectAssociation field = object.getSpecification().getAssociation(fieldName);
            if (field.isVisible(IsisContext.getAuthenticationSession(), object, where).isVetoed()) {
                throw new ForbiddenException(field, ForbiddenException.VISIBLE);
            }
            object = field.get(object);
            id = tagProcessor.getContext().mapObject(object, Scope.REQUEST);
        }

        if (object != null) {
            IsisContext.getPersistenceSession().resolveImmediately(object);
            tagProcessor.getContext().addVariable("_object", id, Scope.REQUEST);
            importFile(tagProcessor, path);
        }
        tagProcessor.closeEmpty();
    }

    private static void importFile(final TagProcessor tagProcessor, final String path) {
        // TODO load in file via HtmlFileParser
        final File file = new File(path);
        BufferedReader reader = null;
        try {
            if (file.exists()) {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                String line;
                while ((line = reader.readLine()) != null) {
                    tagProcessor.appendHtml(line);
                }
            } else {
                tagProcessor.appendHtml("<P classs=\"error\">File " + path + " not found to import</P>");
            }
        } catch (final FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "include-object";
    }

}

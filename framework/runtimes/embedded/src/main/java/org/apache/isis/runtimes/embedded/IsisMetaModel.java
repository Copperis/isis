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

package org.apache.isis.runtimes.embedded;

import static org.apache.isis.core.commons.ensure.Ensure.ensureThatArg;
import static org.apache.isis.core.commons.ensure.Ensure.ensureThatState;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.isis.core.commons.components.ApplicationScopedComponent;
import org.apache.isis.core.commons.config.IsisConfiguration;
import org.apache.isis.core.commons.config.IsisConfigurationDefault;
import org.apache.isis.core.commons.exceptions.IsisException;
import org.apache.isis.core.metamodel.facetdecorator.FacetDecorator;
import org.apache.isis.core.metamodel.layout.MemberLayoutArranger;
import org.apache.isis.core.metamodel.progmodel.ProgrammingModel;
import org.apache.isis.core.metamodel.services.ServicesInjector;
import org.apache.isis.core.metamodel.services.container.DomainObjectContainerDefault;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.SpecificationLoader;
import org.apache.isis.core.metamodel.specloader.ObjectReflectorDefault;
import org.apache.isis.core.metamodel.specloader.classsubstitutor.ClassSubstitutor;
import org.apache.isis.core.metamodel.specloader.collectiontyperegistry.CollectionTypeRegistry;
import org.apache.isis.core.metamodel.specloader.collectiontyperegistry.CollectionTypeRegistryDefault;
import org.apache.isis.core.metamodel.specloader.traverser.SpecificationTraverser;
import org.apache.isis.core.metamodel.specloader.traverser.SpecificationTraverserDefault;
import org.apache.isis.core.metamodel.specloader.validator.MetaModelValidator;
import org.apache.isis.core.progmodel.layout.dflt.MemberLayoutArrangerDefault;
import org.apache.isis.core.progmodel.metamodelvalidator.dflt.MetaModelValidatorDefault;
import org.apache.isis.progmodel.wrapper.applib.WrapperFactory;
import org.apache.isis.progmodel.wrapper.metamodel.DomainObjectContainerWrapperFactory;
import org.apache.isis.progmodel.wrapper.metamodel.internal.WrapperFactoryDefault;
import org.apache.isis.progmodels.dflt.ProgrammingModelFacetsJava5;
import org.apache.isis.runtimes.dflt.bytecode.identity.classsubstitutor.ClassSubstitutorIdentity;
import org.apache.isis.runtimes.embedded.internal.RuntimeContextForEmbeddedMetaModel;

/**
 * Facade for the entire Isis metamodel and supporting components.
 */
public class IsisMetaModel implements ApplicationScopedComponent {

    private static enum State {
        NOT_INITIALIZED, INITIALIZED, SHUTDOWN;
    }

    private final List<Class<?>> serviceTypes = new ArrayList<Class<?>>();
    private State state = State.NOT_INITIALIZED;

    private ObjectReflectorDefault reflector;
    private RuntimeContextForEmbeddedMetaModel runtimeContext;

    private IsisConfiguration configuration;
    private ClassSubstitutor classSubstitutor;
    private CollectionTypeRegistry collectionTypeRegistry;
    private ProgrammingModel programmingModel;
    private SpecificationTraverser specificationTraverser;
    private MemberLayoutArranger memberLayoutArranger;
    private Set<FacetDecorator> facetDecorators;
    private MetaModelValidator metaModelValidator;

    private WrapperFactory viewer;
    private final EmbeddedContext context;
    private List<Object> services;

    public IsisMetaModel(final EmbeddedContext context, final Class<?>... serviceTypes) {

        this.serviceTypes.addAll(Arrays.asList(serviceTypes));
        setConfiguration(new IsisConfigurationDefault());
        setClassSubstitutor(new ClassSubstitutorIdentity());
        setCollectionTypeRegistry(new CollectionTypeRegistryDefault());
        setSpecificationTraverser(new SpecificationTraverserDefault());
        setMemberLayoutArranger(new MemberLayoutArrangerDefault());
        setFacetDecorators(new TreeSet<FacetDecorator>());
        setProgrammingModelFacets(new ProgrammingModelFacetsJava5());

        setMetaModelValidator(new MetaModelValidatorDefault());

        this.context = context;
    }

    /**
     * The list of classes representing services, as specified in the {@link #IsisMetaModel(EmbeddedContext, Class...)
     * constructor}.
     * 
     * <p>
     * To obtain the instantiated services, use the {@link ServicesInjector#getRegisteredServices()} (available from
     * {@link #getServicesInjector()}).
     */
    public List<Class<?>> getServiceTypes() {
        return Collections.unmodifiableList(serviceTypes);
    }

    // ///////////////////////////////////////////////////////
    // init, shutdown
    // ///////////////////////////////////////////////////////

    @Override
    public void init() {
        ensureNotInitialized();
        reflector =
            new ObjectReflectorDefault(configuration, classSubstitutor, collectionTypeRegistry, specificationTraverser,
                memberLayoutArranger, programmingModel, facetDecorators, metaModelValidator);

        services = createServices(serviceTypes);
        runtimeContext = new RuntimeContextForEmbeddedMetaModel(context, services);
        final DomainObjectContainerDefault container = new DomainObjectContainerWrapperFactory();

        runtimeContext.injectInto(container);
        runtimeContext.setContainer(container);
        runtimeContext.injectInto(reflector);
        reflector.injectInto(runtimeContext);

        reflector.init();
        runtimeContext.init();

        for (final Class<?> serviceType : serviceTypes) {
            final ObjectSpecification serviceSpec = reflector.loadSpecification(serviceType);
            serviceSpec.markAsService();
        }
        state = State.INITIALIZED;

        viewer = new WrapperFactoryDefault();
    }

    @Override
    public void shutdown() {
        ensureInitialized();
        state = State.SHUTDOWN;
    }

    private List<Object> createServices(final List<Class<?>> serviceTypes) {
        final List<Object> services = new ArrayList<Object>();
        for (final Class<?> serviceType : serviceTypes) {
            try {
                services.add(serviceType.newInstance());
            } catch (final InstantiationException e) {
                throw new IsisException("Unable to instantiate service", e);
            } catch (final IllegalAccessException e) {
                throw new IsisException("Unable to instantiate service", e);
            }
        }
        return services;
    }

    // ///////////////////////////////////////////////////////
    // SpecificationLoader
    // ///////////////////////////////////////////////////////

    /**
     * Available once {@link #init() initialized}.
     */
    public SpecificationLoader getSpecificationLoader() {
        return reflector;
    }

    // ///////////////////////////////////////////////////////
    // Viewer
    // ///////////////////////////////////////////////////////

    /**
     * Available once {@link #init() initialized}.
     */
    public WrapperFactory getViewer() {
        ensureInitialized();
        return viewer;
    }

    // ///////////////////////////////////////////////////////
    // ServicesInjector
    // ///////////////////////////////////////////////////////

    /**
     * The {@link ServicesInjector}; can use to obtain the set of registered services.
     * 
     * <p>
     * Available once {@link #init() initialized}.
     */
    public ServicesInjector getServicesInjector() {
        ensureInitialized();
        return runtimeContext.getServicesInjector();
    }

    // ///////////////////////////////////////////////////////
    // Override defaults
    // ///////////////////////////////////////////////////////

    /**
     * The {@link IsisConfiguration} in force, either defaulted or specified
     * {@link #setConfiguration(IsisConfiguration) explicitly.}
     */
    public IsisConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Optionally specify the {@link IsisConfiguration}.
     * 
     * <p>
     * Call prior to {@link #init()}.
     */
    public void setConfiguration(final IsisConfiguration configuration) {
        ensureNotInitialized();
        ensureThatArg(configuration, is(notNullValue()));
        this.configuration = configuration;
    }

    /**
     * The {@link ClassSubstitutor} in force, either defaulted or specified
     * {@link #setClassSubstitutor(ClassSubstitutor) explicitly}.
     */
    public ClassSubstitutor getClassSubstitutor() {
        return classSubstitutor;
    }

    /**
     * Optionally specify the {@link ClassSubstitutor}.
     * 
     * <p>
     * Call prior to {@link #init()}.
     */
    public void setClassSubstitutor(final ClassSubstitutor classSubstitutor) {
        ensureNotInitialized();
        ensureThatArg(classSubstitutor, is(notNullValue()));
        this.classSubstitutor = classSubstitutor;
    }

    /**
     * The {@link CollectionTypeRegistry} in force, either defaulted or specified
     * {@link #setCollectionTypeRegistry(CollectionTypeRegistry) explicitly.}
     */
    public CollectionTypeRegistry getCollectionTypeRegistry() {
        return collectionTypeRegistry;
    }

    /**
     * Optionally specify the {@link CollectionTypeRegistry}.
     * 
     * <p>
     * Call prior to {@link #init()}.
     */
    public void setCollectionTypeRegistry(final CollectionTypeRegistry collectionTypeRegistry) {
        ensureNotInitialized();
        ensureThatArg(collectionTypeRegistry, is(notNullValue()));
        this.collectionTypeRegistry = collectionTypeRegistry;
    }

    /**
     * The {@link SpecificationTraverser} in force, either defaulted or specified
     * {@link #setSpecificationTraverser(SpecificationTraverser) explicitly}.
     */
    public SpecificationTraverser getSpecificationTraverser() {
        return specificationTraverser;
    }

    /**
     * Optionally specify the {@link SpecificationTraverser}.
     */
    public void setSpecificationTraverser(final SpecificationTraverser specificationTraverser) {
        this.specificationTraverser = specificationTraverser;
    }

    /**
     * Optionally specify the {@link MemberLayoutArranger}.
     */
    public void setMemberLayoutArranger(final MemberLayoutArranger memberLayoutArranger) {
        this.memberLayoutArranger = memberLayoutArranger;
    }

    /**
     * The {@link ProgrammingModel} in force, either defaulted or specified
     * {@link #setProgrammingModelFacets(ProgrammingModel) explicitly}.
     */
    public ProgrammingModel getProgrammingModelFacets() {
        return programmingModel;
    }

    /**
     * Optionally specify the {@link ProgrammingModel}.
     * 
     * <p>
     * Call prior to {@link #init()}.
     */
    public void setProgrammingModelFacets(final ProgrammingModel programmingModel) {
        ensureNotInitialized();
        ensureThatArg(programmingModel, is(notNullValue()));
        this.programmingModel = programmingModel;
    }

    /**
     * The {@link FacetDecorator}s in force, either defaulted or specified {@link #setFacetDecorators(Set) explicitly}.
     */
    public Set<FacetDecorator> getFacetDecorators() {
        return Collections.unmodifiableSet(facetDecorators);
    }

    /**
     * Optionally specify the {@link FacetDecorator}s.
     * 
     * <p>
     * Call prior to {@link #init()}.
     */
    public void setFacetDecorators(final Set<FacetDecorator> facetDecorators) {
        ensureNotInitialized();
        ensureThatArg(facetDecorators, is(notNullValue()));
        this.facetDecorators = facetDecorators;
    }

    /**
     * The {@link MetaModelValidator} in force, either defaulted or specified
     * {@link #setMetaModelValidator(MetaModelValidator) explicitly}.
     */
    public MetaModelValidator getMetaModelValidator() {
        return metaModelValidator;
    }

    /**
     * Optionally specify the {@link MetaModelValidator}.
     */
    public void setMetaModelValidator(final MetaModelValidator metaModelValidator) {
        this.metaModelValidator = metaModelValidator;
    }

    // ///////////////////////////////////////////////////////
    // State management
    // ///////////////////////////////////////////////////////

    private State ensureNotInitialized() {
        return ensureThatState(state, is(State.NOT_INITIALIZED));
    }

    private State ensureInitialized() {
        return ensureThatState(state, is(State.INITIALIZED));
    }

}

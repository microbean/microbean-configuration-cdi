/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2017-2019 microBean™.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.microbean.configuration.cdi;

import java.lang.annotation.Annotation;

import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.lang.reflect.Member;
import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.Dependent;

import javax.enterprise.event.Observes;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces; // for javadoc only

import javax.enterprise.inject.literal.SingletonLiteral;

import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.ProducerFactory; // for javadoc only

import javax.inject.Singleton; // for javadoc only

import org.microbean.configuration.api.ConfigurationException;
import org.microbean.configuration.api.Configurations;

import org.microbean.configuration.cdi.annotation.Configuration;
import org.microbean.configuration.cdi.annotation.ConfigurationCoordinate;
import org.microbean.configuration.cdi.annotation.ConfigurationCoordinates;
import org.microbean.configuration.cdi.annotation.ConfigurationValue;

/**
 * An {@link Extension} that adapts the configuration ecosystem
 * exposed by the {@link Configurations} class to CDI constructs.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see Configurations
 */
public class ConfigurationsExtension implements Extension {


  /*
   * Static fields.
   */


  /**
   * An {@linkplain Collections#unmodifiableMap(Map) immutable} {@link
   * Map} of values for primitive {@link Type}s that have not been
   * initialized (e.g. {@code 0} for {@code int.class}, {@code false}
   * for {@code boolean.class} and so on).
   *
   * <p>This field is never {@code null}.</p>
   *
   * @see #produceConfigurationValue(InjectionPoint, Configurations)
   */
  private static final Map<Type, Object> uninitializedValues = Collections.unmodifiableMap(new HashMap<Type, Object>() {
      private static final long serialVersionUID = 1L;
      {
        put(boolean.class, false);
        put(byte.class, (byte)0);
        put(char.class, (char)0);
        put(double.class, 0D);
        put(float.class, 0F);
        put(int.class, 0);
        put(long.class, 0L);
        put(short.class, (short)0);
        put(void.class, null);
      }
    });


  /*
   * Instance fields.
   */


  /**
   * The {@link Configurations} to which most work is delegated.
   *
   * <p>This field may be {@code null}.</p>
   */
  private Configurations configurations;

  /**
   * A {@link Logger} for use by this {@link ConfigurationsExtension}.
   *
   * <p>This field is never {@code null}.</p>
   *
   * @see #createLogger()
   */
  protected final Logger logger;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link ConfigurationsExtension}.
   *
   * @exception IllegalStateException if {@code #createLogger()}
   * returns {@code null}
   *
   * @see #createLogger()
   */
  public ConfigurationsExtension() {
    super();
    this.logger = this.createLogger();
    if (this.logger == null) {
      throw new IllegalStateException("createLogger() == null");
    }
  }


  /*
   * Instance methods.
   */


  /**
   * Returns a {@link Logger} for use by {@link
   * ConfigurationsExtension} implementations.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * <p>Overrides of this method must not return {@code null}.</p>
   *
   * @return a non-{@code null} {@link Logger}
   */
  protected Logger createLogger() {
    return Logger.getLogger(this.getClass().getName());
  }

  /**
   * {@linkplain Observes Observes} the {@link BeforeBeanDiscovery}
   * event and creates a {@link Configurations} instance that will
   * eventually be added as a bean itself.
   *
   * @param event the {@link BeforeBeanDiscovery} event being
   * observed; if {@code null}, then no action will be taken
   *
   * @exception ConfigurationException if no {@link Configurations}
   * implementation is available
   */
  private final void addConfigurations(@Observes final BeforeBeanDiscovery event) {
    final String cn = this.getClass().getName();
    final String mn = "addConfigurations";
    if (this.logger.isLoggable(Level.FINER)) {
      this.logger.entering(cn, mn, event);
    }
    if (event != null) {
      this.configurations = Configurations.newInstance();
      assert this.configurations != null;
    }
    if (this.logger.isLoggable(Level.FINER)) {
      this.logger.exiting(cn, mn);
    }
  }

  /**
   * {@linkplain Observes Observes} the {@link ProcessInjectionPoint}
   * event and ensures that an application's {@linkplain
   * Configurations#getConfigurationCoordinates() configuration
   * coordinates} are reflected as additional {@linkplain
   * ConfigurationCoordinates qualifier annotations} at each {@link
   * ConfigurationValue}-annotated injection point.
   *
   * @param event the {@link ProcessInjectionPoint} event being
   * observed; if {@code null}, then no action will be taken
   *
   * @param beanManager the {@link BeanManager} for the current CDI
   * container; if {@code null} then no action will be taken
   *
   * @see ConfigurationCoordinates
   *
   * @see Configurations#getConfigurationCoordinates()
   *
   * @see ConfigurationValue
   */
  private final void installConfigurationCoordinateQualifiers(@Observes final ProcessInjectionPoint<?, ?> event, final BeanManager beanManager) {
    final String cn = this.getClass().getName();
    final String mn = "installConfigurationCoordinateQualifiers";
    if (this.logger.isLoggable(Level.FINER)) {
      this.logger.entering(cn, mn, new Object[] { event, beanManager });
    }
    if (event != null && beanManager != null) {
      final InjectionPoint injectionPoint = event.getInjectionPoint();
      if (injectionPoint != null) {
        final Set<Annotation> qualifiers = injectionPoint.getQualifiers();
        if (qualifiers != null && !qualifiers.isEmpty()) {
          ConfigurationValue configurationValue = null;
          ConfigurationCoordinates configurationCoordinates = null;
          for (final Annotation qualifier : qualifiers) {
            assert qualifier != null;
            if (qualifier instanceof ConfigurationValue) {
              configurationValue = (ConfigurationValue)qualifier;
            } else if (qualifier instanceof ConfigurationCoordinates) {
              configurationCoordinates = (ConfigurationCoordinates)qualifier;
            }
          }
          if (configurationValue != null) {
            final Set<Annotation> newQualifiers = new HashSet<>(qualifiers);
            if (configurationCoordinates != null) {
              newQualifiers.remove(configurationCoordinates);
            }
            final ConfigurationCoordinates.Literal literal = new ConfigurationCoordinates.Literal(configurationCoordinates);
            final Map<String, String> coordinatesMap = this.configurations.getConfigurationCoordinates();
            if (coordinatesMap != null && !coordinatesMap.isEmpty()) {
              final Set<Entry<String, String>> entries = coordinatesMap.entrySet();
              assert entries != null;
              assert !entries.isEmpty();
              for (final Entry<String, String> entry : entries) {
                assert entry != null;
                final String name = entry.getKey();
                assert name != null;
                if (!literal.containsKey(name)) {
                  final String value = entry.getValue();
                  assert value != null;
                  literal.add(new ConfigurationCoordinate.Literal(name, value));
                }
              }
            }
            newQualifiers.add(literal);
            event.configureInjectionPoint().qualifiers(newQualifiers);
          }
        }
      }
    }
    if (this.logger.isLoggable(Level.FINER)) {
      this.logger.exiting(cn, mn);
    }
  }

  /**
   * {@linkplain Observes Observes} the {@link AfterBeanDiscovery}
   * event and installs a dynamic <a
   * href="http://docs.jboss.org/cdi/spec/2.0.Beta1/cdi-spec.html#producer_method">producer
   * method</a> for each {@link Type} found in the return value from
   * the {@link Configurations#getConversionTypes()} method.
   *
   * <p>The net result is that all injection points that (a) are
   * annotated with {@link ConfigurationValue} and (b) have a
   * {@linkplain InjectionPoint#getType() type} that the configuration
   * system can convert to will be handled properly.</p>
   *
   * @param event the {@link AfterBeanDiscovery} event being observed;
   * if {@code null}, no action will be taken
   *
   * @param beanManager the {@link BeanManager} for the current CDI
   * container; if {@code null}, no action will be taken
   *
   * @see BeanManager#createBean(BeanAttributes, Class, ProducerFactory)
   *
   * @see BeanManager#getProducerFactory(AnnotatedMethod, Bean)
   *
   * @see #produceConfigurationValue(InjectionPoint, Configurations)
   */
  private final void installConfigurationValueProducerMethods(@Observes final AfterBeanDiscovery event, final BeanManager beanManager) {
    final String cn = this.getClass().getName();
    final String mn = "installConfigurationValueProducerMethods";
    if (this.logger.isLoggable(Level.FINER)) {
      this.logger.entering(cn, mn, new Object[] { event, beanManager });
    }
    
    if (event != null && beanManager != null) {

      // Add this.configurations as a Singleton-scoped bean.
      event.addBean()
        .addTransitiveTypeClosure(this.configurations.getClass())
        .createWith(cc -> this.configurations)
        .scope(Singleton.class);

      // For each conversion type, add a producer that makes objects
      // of that type.  Note that the qualifiers are nonbinding.
      final Set<Type> types = this.configurations.getConversionTypes();
      if (types != null && !types.isEmpty()) {
        for (final Type type : types) {
          assert type != null;
          event.addBean()
            .addTransitiveTypeClosure(type)
            .addQualifiers(new ConfigurationCoordinates.Literal(),
                           ConfigurationValue.Literal.of(""))
            .scope(Dependent.class)
            .produceWith(cdi ->
                         produceConfigurationValue(cdi.select(InjectionPoint.class).get(),
                                                   cdi.select(Configurations.class).get()));
        }
      }
      
    }

    if (this.logger.isLoggable(Level.FINER)) {
      this.logger.exiting(cn, mn);
    }
  }


  /*
   * Static methods.
   */


  /**
   * A template of sorts for {@link ProducerFactory} implementations
   * created and installed by the {@link
   * #installConfigurationValueProducerMethods(AfterBeanDiscovery,
   * BeanManager)} method.
   *
   * <p>This method may return {@code null}.</p>
   *
   * <p>Every producer method that is effectively implemented by this
   * method will therefore first {@linkplain
   * #getMetadata(InjectionPoint) determine the configuration
   * coordinates at the site of injection} as well as the name of the
   * configuration value that should be injected.  {@link
   * Configurations#getValue(Map, String, Type)} is then used to
   * retrieve the value which is returned as an {@link Object} by this
   * method, but which will be returned as an object of the proper
   * type by the "real" producer method.</p>
   *
   * @param injectionPoint the {@link InjectionPoint} describing the
   * site of injection; must not be {@code null}
   *
   * @param configurations the {@link Configurations} object that will
   * do the actual value retrieval; must not be {@code null}
   *
   * @return the configuration value, converted appropriately
   *
   * @exception NullPointerException if either parameter value is
   * {@code null}
   *
   * @see
   * #installConfigurationValueProducerMethods(AfterBeanDiscovery,
   * BeanManager)
   *
   * @see Configurations#getValue(Map, String, Type)
   */
  private static final Object produceConfigurationValue(final InjectionPoint injectionPoint, final Configurations configurations) {
    final String cn = ConfigurationsExtension.class.getName();
    final Logger logger = Logger.getLogger(cn);
    assert logger != null;
    final String mn = "produceConfigurationValue";
    if (logger.isLoggable(Level.FINER)) {
      logger.entering(cn, mn, new Object[] { injectionPoint, configurations });
    }
    Objects.requireNonNull(injectionPoint);
    Objects.requireNonNull(configurations);
    final ConfigurationValueMetadata metadata = getMetadata(injectionPoint);
    assert metadata != null : "metadata == null";
    final Map<String, String> coordinates = metadata.getConfigurationCoordinates();
    final Collection<String> names = metadata.getNames();
    assert names != null;
    final String defaultValue = metadata.getDefaultValue();
    final Type injectionPointType = injectionPoint.getType();
    Object returnValue = configurations.getValue(coordinates, names, injectionPointType, defaultValue);
    if (returnValue == null && defaultValue == null && injectionPointType instanceof Class && ((Class<?>)injectionPointType).isPrimitive()) {
      returnValue = uninitializedValues.get(injectionPointType);
    }
    if (logger.isLoggable(Level.FINER)) {
      logger.exiting(cn, mn, returnValue);
    }
    return returnValue;
  }


  /*
   * Static methods.
   */


  /**
   * Returns a {@link ConfigurationValueMetadata} object representing
   * all the configuration-value-related metadata available on the
   * supplied {@link InjectionPoint}.
   *
   * @param injectionPoint the {@link InjectionPoint} to consider; may
   * be {@code null} in which case {@code null} will be returned
   *
   * @return a {@link ConfigurationValueMetadata} object, or {@code null}
   */
  private static final ConfigurationValueMetadata getMetadata(final InjectionPoint injectionPoint) {
    final String cn = ConfigurationsExtension.class.getName();
    final Logger logger = Logger.getLogger(cn);
    assert logger != null;
    final String mn = "getMetadata";
    if (logger.isLoggable(Level.FINER)) {
      logger.entering(cn, mn, injectionPoint);
    }
    ConfigurationValueMetadata returnValue = null;
    if (injectionPoint != null) {
      final Set<Annotation> qualifiers = injectionPoint.getQualifiers();
      if (qualifiers != null && !qualifiers.isEmpty()) {
        Map<String, String> configurationCoordinates = null;
        List<String> names = null;
        String defaultValue = null;
        for (final Annotation qualifier : qualifiers) {
          if (qualifier instanceof ConfigurationValue) {
            if (names == null) {
              final ConfigurationValue configurationValue = (ConfigurationValue)qualifier;
              defaultValue = configurationValue.defaultValue();
              if (defaultValue == null || defaultValue.equals(ConfigurationValue.NULL)) {
                defaultValue = null;
              } else {
                defaultValue = configurationValue.defaultValue().trim();
                assert defaultValue != null;
              }
              Annotated annotated = injectionPoint.getAnnotated();
              assert annotated != null;
              names = new ArrayList<>();
              names.addAll(Arrays.asList(configurationValue.value()));
              String prefix = null;
              if (names.isEmpty()) {
                // Try to get it from the annotated element
                if (annotated instanceof AnnotatedField) {
                  final Member field = ((AnnotatedField)annotated).getJavaMember();
                  assert field != null;
                  names.add(field.getName());
                } else if (annotated instanceof AnnotatedParameter) {
                  final AnnotatedParameter<?> annotatedParameter = (AnnotatedParameter<?>)annotated;

                  final AnnotatedMember<?> annotatedMember = annotatedParameter.getDeclaringCallable();
                  assert annotatedMember != null;

                  final Member member = annotatedMember.getJavaMember();
                  assert member != null;
                  assert member instanceof Executable;

                  final int parameterIndex = annotatedParameter.getPosition();
                  assert parameterIndex >= 0;

                  final Parameter[] parameters = ((Executable)member).getParameters();
                  assert parameters != null;
                  assert parameters.length >= parameterIndex;

                  final Parameter parameter = parameters[parameterIndex];
                  assert parameter != null;

                  if (parameter.isNamePresent()) {
                    names.add(parameter.getName());
                  } else {
                    throw new IllegalStateException("The parameter at index " +
                                                    parameterIndex +
                                                    " in " +
                                                    member +
                                                    " did not have a name available via reflection. " +
                                                    "Make sure you compiled its enclosing class, " +
                                                    member.getDeclaringClass().getName() +
                                                    ", with the -parameters option supplied to javac, " +
                                                    " or make use of the value() element of the " +
                                                    ConfigurationValue.class.getName() +
                                                    " annotation.");
                  }
                } else {
                  assert names.isEmpty();
                }
              }
              if (!names.isEmpty()) {
                // See if the InjectionPoint is "inside" a "context" with
                // a @Configuration annotation; that will define our
                // prefix if so
                assert annotated != null;
                Configuration configuration = null;
                while (configuration == null) {
                  configuration = annotated.getAnnotation(Configuration.class);
                  if (configuration == null) {
                    if (annotated instanceof AnnotatedParameter) {
                      annotated = ((AnnotatedParameter)annotated).getDeclaringCallable();
                    } else if (annotated instanceof AnnotatedMember) {
                      annotated = ((AnnotatedMember)annotated).getDeclaringType();
                    } else if (annotated instanceof AnnotatedType) {
                      break;
                    } else {
                      assert false : "Unexpected annotated: " + annotated;
                    }
                  } else {
                    prefix = configuration.value().trim(); // TODO: trim?
                    assert prefix != null;
                    final ListIterator<String> iterator = names.listIterator();
                    assert iterator != null;
                    while (iterator.hasNext()) {
                      final String name = iterator.next();
                      if (name == null || name.isEmpty() || name.equals(ConfigurationValue.NULL)) {
                        iterator.remove();
                      } else if (!prefix.isEmpty()) {
                        iterator.set(new StringBuilder(prefix).append('.').append(name).toString());
                      }
                    }
                  }
                }
              }
            }
          } else if (qualifier instanceof ConfigurationCoordinates) {
            if (configurationCoordinates == null) {
              final ConfigurationCoordinates configurationCoordinatesAnnotation = (ConfigurationCoordinates)qualifier;
              final ConfigurationCoordinate[] coordinateArray = configurationCoordinatesAnnotation.value();
              assert coordinateArray != null;
              if (coordinateArray.length > 0) {
                configurationCoordinates = new HashMap<>();
                for (final ConfigurationCoordinate coordinate : coordinateArray) {
                  assert coordinate != null;
                  final String coordinateName = coordinate.name();
                  assert coordinateName != null;
                  final String coordinateValue = coordinate.value();
                  assert coordinateValue != null;
                  configurationCoordinates.put(coordinateName, coordinateValue);
                }
              }
            }
          }
        }
        returnValue = new ConfigurationValueMetadata(configurationCoordinates, names, defaultValue);
      }
    }
    if (logger.isLoggable(Level.FINER)) {
      logger.exiting(cn, mn, returnValue);
    }
    return returnValue;
  }


  /*
   * Inner and nested classes.
   */


  /**
   * A value object containing metadata semantically associated with a
   * {@link ConfigurationValue} injection point.
   *
   * @author <a href="https://about.me/lairdnelson"
   * target="_parent">Laird Nelson</a>
   *
   * @see ConfigurationValue
   */
  private static final class ConfigurationValueMetadata {


    /*
     * Instance fields.
     */


    /**
     * A {@link Map} of <em>configuration coordinates</em> in effect.
     *
     * <p>This field is never {@code null}.</p>
     *
     * @see #getConfigurationCoordinates()
     */
    private final Map<String, String> configurationCoordinates;

    /**
     * The name of the {@link ConfigurationValue}.
     *
     * <p>This field is never {@code null}.</p>
     *
     * @see #getName()
     */
    private final Collection<String> names;

    /**
     * The default value to use for the {@link ConfigurationValue}.
     *
     * <p>This field may be {@code null}.</p>
     *
     * @see #getDefaultValue()
     */
    private final String defaultValue;


    /*
     * Constructors.
     */


    /**
     * Creates a new {@link ConfigurationValueMetadata}.
     *
     * @param configurationCoordinates a {@link Map} of
     * <em>configuration coordinates</em> in effect; may be {@code
     * null}
     *
     * @param names the names of the {@link ConfigurationValue}; must
     * not be {@code null}
     *
     * @param defaultValue the default value for the {@link
     * ConfigurationValue}; may be {@code null}
     *
     * @see #getConfigurationCoordinates()
     *
     * @see #getNames()
     *
     * @see #getDefaultValue()
     *
     * @exception NullPointerException if {@code names} is {@code null}
     */
    private ConfigurationValueMetadata(final Map<String, String> configurationCoordinates,
                                       final Collection<String> names,
                                       final String defaultValue) {
      super();
      Objects.requireNonNull(names);
      if (configurationCoordinates == null || configurationCoordinates.isEmpty()) {
        this.configurationCoordinates = Collections.emptyMap();
      } else {
        this.configurationCoordinates = Collections.unmodifiableMap(configurationCoordinates);
      }
      if (names.isEmpty()) {
        this.names = Collections.emptySet();
      } else {
        this.names = Collections.unmodifiableCollection(new ArrayList<>(names));
      }
      if (defaultValue == null || defaultValue.equals(ConfigurationValue.NULL)) {
        this.defaultValue = null;
      } else {
        this.defaultValue = defaultValue;
      }
    }

    /**
     * Returns a {@link Map} of <em>configuration coordinates</em>
     * associated with this {@link ConfigurationValueMetadata}.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * @return a non-{@code null} {@link Map} of configuration
     * coordinates
     */
    public final Map<String, String> getConfigurationCoordinates() {
      return this.configurationCoordinates;
    }

    /**
     * Returns the names of the {@link ConfigurationValue} for which
     * this {@link ConfigurationValueMetadata} serves as metadata.
     *
     * <p>This method may return {@code null}.</p>
     *
     * @return the names of the {@link ConfigurationValue}, or {@code
     * null}
     */
    public final Collection<String> getNames() {
      return this.names;
    }

    /**
     * Returns the default value of the {@link ConfigurationValue} for
     * which this {@link ConfigurationValueMetadata} serves as
     * metadata.
     *
     * <p>This method may return {@code null}.</p>
     *
     * @return the default value of the {@link ConfigurationValue}, or
     * {@code null}
     */
    public final String getDefaultValue() {
      return this.defaultValue;
    }

    /**
     * Returns a hashcode for this {@link ConfigurationValueMetadata}.
     *
     * @return a hashcode
     *
     * @see #equals(Object)
     */
    @Override
    public final int hashCode() {
      int hashCode = 17;

      final Object configurationCoordinates = this.getConfigurationCoordinates();
      int c = configurationCoordinates == null ? 0 : configurationCoordinates.hashCode();
      hashCode = 37 * hashCode + c;

      final Object names = this.getNames();
      c = names == null ? 0 : names.hashCode();
      hashCode = 37 * hashCode + c;

      final Object defaultValue = this.getDefaultValue();
      c = defaultValue == null ? 0 : defaultValue.hashCode();
      hashCode = 37 * hashCode + c;

      return hashCode;
    }

    /**
     * Returns {@code true} if the supplied {@link Object} is an
     * instance of {@link ConfigurationValueMetadata} and has values
     * equal to the values of this {@link
     * ConfigurationValueMetadata}'s properties.
     *
     * @param other the {@link Object} to test; may be {@code null}
     *
     * @return {@code true} if the supplied {@link Object} is equal to
     * this {@link ConfigurationValueMetadata}; {@code false}
     * otherwise
     *
     * @see #hashCode()
     */
    @Override
    public final boolean equals(final Object other) {
      if (other == this) {
        return true;
      } else if (other instanceof ConfigurationValueMetadata) {
        final ConfigurationValueMetadata her = (ConfigurationValueMetadata)other;

        final Object configurationCoordinates = this.getConfigurationCoordinates();
        if (configurationCoordinates == null) {
          if (her.getConfigurationCoordinates() != null) {
            return false;
          }
        } else if (!configurationCoordinates.equals(her.getConfigurationCoordinates())) {
          return false;
        }

        final Object names = this.getNames();
        if (names == null) {
          if (her.getNames() != null) {
            return false;
          }
        } else if (!names.equals(her.getNames())) {
          return false;
        }

        final Object defaultValue = this.getDefaultValue();
        if (defaultValue == null) {
          if (her.getDefaultValue() != null) {
            return false;
          }
        } else if (!defaultValue.equals(her.getDefaultValue())) {
          return false;
        }

        return true;
      } else {
        return false;
      }
    }

  }


  /**
   * A {@link BeanAttributes} implementation that delegates all of its
   * calls to another {@link BeanAttributes}.
   *
   * @author <a href="http://about.me/lairdnelson"
   * target="_parent">Laird Nelson</a>
   *
   * @see BeanAttributes
   */
  private static class DelegatingBeanAttributes<T> implements BeanAttributes<T> {


    /*
     * Instance fields.
     */


    /**
     * The {@link BeanAttributes} to which all calls will be
     * delegated.
     *
     * <p>This field is never {@code null}.</p>
     */
    private final BeanAttributes<?> delegate;


    /*
     * Constructors.
     */


    /**
     * Creates a new {@link DelegatingBeanAttributes}.
     *
     * @param delegate the {@link BeanAttributes} to which all calls
     * will be delegated; must not be {@code null}
     *
     * @exception NullPointerException if {@code delegate} is {@code
     * null}
     */
    private DelegatingBeanAttributes(final BeanAttributes<?> delegate) {
      super();
      Objects.requireNonNull(delegate);
      this.delegate = delegate;
    }


    /*
     * Instance methods.
     */


    /**
     * Returns the name for this {@link DelegatingBeanAttributes} if
     * it has one.
     *
     * <p>This method may return {@code null}.</p>
     *
     * @return the name for this {@link DelegatingBeanAttributes}, or
     * {@code null}
     *
     * @see BeanAttributes#getName()
     */
    @Override
    public String getName() {
      return this.delegate.getName();
    }

    /**
     * Returns a {@link Set} of {@link Annotation}s that are,
     * themselves, each {@linkplain BeanManager#isQualifier(Class)
     * considered to be a qualifier} by CDI.
     *
     * <p>This method may return {@code null}.</p>
     *
     * @return a {@link Set} of qualifier {@link Annotation}s, or
     * {@code null}
     *
     * @see BeanAttributes#getQualifiers()
     *
     * @see BeanManager#isQualifier(Class)
     */
    @Override
    public Set<Annotation> getQualifiers() {
      return this.delegate.getQualifiers();
    }

    /**
     * Returns the {@link Class} representing the scope of the bean
     * described by this {@link DelegatingBeanAttributes}.
     *
     * <p>This method will never return {@code null}.</p>
     *
     * <p>Overrides of this method must not return {@code null}.</p>
     *
     * @return the {@link Class} representing the scope of the bean
     * described by this {@link DelegatingBeanAttributes}; never
     * {@code null}
     *
     * @see BeanAttributes#getScope()
     */
    @Override
    public Class<? extends Annotation> getScope() {
      return this.delegate.getScope();
    }

    /**
     * Returns a {@link Set} of {@link Annotation}s that are,
     * themselves, each {@linkplain BeanManager#isStereotype(Class)
     * considered to be a stereotype} by CDI.
     *
     * <p>This method may return {@code null}.</p>
     *
     * @return a {@link Set} of stereotype {@link Annotation}s, or
     * {@code null}
     *
     * @see BeanAttributes#getStereotypes()
     *
     * @see BeanManager#isStereotype(Class)
     */
    @Override
    public Set<Class<? extends Annotation>>	getStereotypes() {
      return this.delegate.getStereotypes();
    }

    /**
     * Returns a {@link Set} of {@link Type}s whose elements represent
     * the bean types of the bean described by this {@link
     * DelegatingBeanAttributes}.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * <p>Overrides of this method must not return {@code null}.</p>
     *
     * @return a non-{@code null} {@link Set} of bean types
     *
     * @see BeanAttributes#getTypes()
     */
    @Override
    public Set<Type> getTypes() {
      return this.delegate.getTypes();
    }

    /**
     * Returns {@code true} if the bean described by this {@link
     * DelegatingBeanAttributes} is an alternative; {@code false}
     * otherwise.
     *
     * @return {@code true} if the bean described by this {@link
     * DelegatingBeanAttributes} is an alternative; {@code false}
     * otherwise
     *
     * @see BeanAttributes#isAlternative()
     */
    @Override
    public boolean isAlternative() {
      return this.delegate.isAlternative();
    }

    /**
     * Returns a {@link String} representation of this {@link DelegatingBeanAttributes}.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * <p>Overrides of this method must not return {@code null}.</p>
     *
     * @return a non-{@code null} {@link String} representation of
     * this {@link DelegatingBeanAttributes}
     */
    @Override
    public String toString() {
      return this.delegate.toString();
    }

  }

}

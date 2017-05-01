/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2017 MicroBean.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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
   * Constructors.
   */
  
  
  /**
   * Creates a new {@link ConfigurationsExtension}.
   */
  public ConfigurationsExtension() {
    super();
  }


  /*
   * Instance methods.
   */
  

  /**
   * {@linkplain Observes Observes} the {@link BeforeBeanDiscovery}
   * event and ensures that {@link Configurations} is added as an
   * {@link AnnotatedType} in {@linkplain Singleton singleton scope}.
   *
   * @param event the {@link BeforeBeanDiscovery} event being
   * observed; if {@code null}, then no action will be taken
   *
   * @exception ConfigurationException if no {@link Configurations}
   * implementation is available
   */
  private final void addConfigurations(@Observes final BeforeBeanDiscovery event) {
    if (event != null) {
      final Configurations configurations = Configurations.newInstance();
      assert configurations != null;
      event.addAnnotatedType(configurations.getClass(), "configurations")
        .add(SingletonLiteral.INSTANCE);
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
            final Instance<Object> i = beanManager.createInstance();
            assert i != null;
            final Instance<Configurations> configurationsInstance = i.select(Configurations.class);
            assert configurationsInstance != null;
            if (configurationsInstance.isResolvable()) {
              final Configurations configurations = configurationsInstance.get();
              assert configurations != null;

              final Set<Annotation> newQualifiers = new HashSet<>(qualifiers);
              if (configurationCoordinates != null) {
                newQualifiers.remove(configurationCoordinates);
              }
              final ConfigurationCoordinates.Literal literal = new ConfigurationCoordinates.Literal(configurationCoordinates);
              final Map<String, String> coordinatesMap = configurations.getConfigurationCoordinates();
              if (coordinatesMap != null && !coordinatesMap.isEmpty()) {
                final Set<Entry<String, String>> entries = coordinatesMap.entrySet();
                assert entries != null;
                for (final Entry<String, String> entry : entries) {
                  final String name = entry.getKey();
                  assert name != null;
                  final String value = entry.getValue();
                  assert value != null;
                  if (!literal.containsKey(name)) {
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
    if (event != null && beanManager != null) {
      final Instance<Object> i = beanManager.createInstance();
      assert i != null;
      final Instance<Configurations> configurationsInstance = i.select(Configurations.class);
      assert configurationsInstance != null;
      if (configurationsInstance.isResolvable()) {
        final Configurations configurations = configurationsInstance.get();
        assert configurations != null;
        final Set<Type> types = configurations.getConversionTypes();
        if (types != null && !types.isEmpty()) {
          final AnnotatedType<ConfigurationsExtension> thisType = beanManager.createAnnotatedType(ConfigurationsExtension.class);
          final AnnotatedMethod<? super ConfigurationsExtension> producerMethod = thisType.getMethods().stream()
            .filter(m -> m.getJavaMember().getName().equals("produceConfigurationValue"))
            .findFirst()
            .get();
          final BeanAttributes<?> producerAttributes = beanManager.createBeanAttributes(producerMethod);
          for (final Type type : types) {
            assert type != null;
            final Bean<?> bean =
              beanManager.createBean(new DelegatingBeanAttributes<Object>(producerAttributes) {
                  @Override
                  public final Set<Type> getTypes() {
                    final Set<Type> types = new HashSet<>();
                    types.add(Object.class);
                    types.add(type);
                    return types;
                  }
                },
                ConfigurationsExtension.class,
                beanManager.getProducerFactory(producerMethod, null /* null OK; producer method is static */));
            event.addBean(bean);
          }
        }
      }
    }
  }

  /**
   * A template of sorts for {@link ProducerFactory} implementations
   * created and installed by the {@link
   * #installConfigurationValueProducerMethods(AfterBeanDiscovery,
   * BeanManager)} method.
   *
   * <p>This method may return {@code null}.</p>
   *
   * <p>This method, though annotated with {@link
   * ConfigurationCoordinates}, {@link ConfigurationValue} and {@link
   * Dependent}, is <em>not</em> itself a <a
   * href="http://docs.jboss.org/cdi/spec/2.0.Beta1/cdi-spec.html#producer_method">producer
   * method</a>, since it is not annotated with {@link Produces}.  It
   * is instead a method that will be {@linkplain
   * BeanManager#getProducerFactory(AnnotatedMethod, Bean)
   * introspected} and used as the effective "body" of dynamically
   * created producer methods installed by the {@link
   * #installConfigurationValueProducerMethods(AfterBeanDiscovery,
   * BeanManager)} method.</p>
   *
   * <p>Every producer method that incorporates this method will first
   * {@linkplain #getMetadata(InjectionPoint)
   * determine the configuration coordinates at the site of injection}
   * as well as the name of the configuration value that should be
   * injected.  {@link Configurations#getValue(Map, String, Type)} is
   * then used to retrieve the value which is returned as an {@link
   * Object} by this method, but which will be returned as an object
   * of the proper type by the "real" producer method.</p>
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
  @ConfigurationCoordinates
  @ConfigurationValue
  @Dependent
  private static final Object produceConfigurationValue(final InjectionPoint injectionPoint, final Configurations configurations) {
    Objects.requireNonNull(injectionPoint);
    Objects.requireNonNull(configurations);
    final ConfigurationValueMetadata metadata = getMetadata(injectionPoint);
    assert metadata != null : "metadata == null";
    final Map<String, String> coordinates = metadata.getConfigurationCoordinates();
    final String name = metadata.getName();
    assert name != null;
    final String defaultValue = metadata.getDefaultValue();
    final Type injectionPointType = injectionPoint.getType();
    Object returnValue = configurations.getValue(coordinates, name, injectionPointType, defaultValue);
    if (returnValue == null && defaultValue == null && injectionPointType instanceof Class && ((Class<?>)injectionPointType).isPrimitive()) {
      returnValue = uninitializedValues.get(injectionPointType);
    }
    return returnValue;
  }


  /*
   * Static methods.
   */


  private static final ConfigurationValueMetadata getMetadata(final InjectionPoint injectionPoint) {
    ConfigurationValueMetadata returnValue = null;
    if (injectionPoint != null) {
      final Set<Annotation> qualifiers = injectionPoint.getQualifiers();
      if (qualifiers != null && !qualifiers.isEmpty()) {
        Map<String, String> configurationCoordinates = null;
        String name = null;
        String defaultValue = null;
        for (final Annotation qualifier : qualifiers) {
          if (qualifier instanceof ConfigurationValue) {
            if (name == null) {
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
              name = configurationValue.value().trim();
              assert name != null;
              if (name.isEmpty()) {
                // Try to get it from the annotated element
                if (annotated instanceof AnnotatedField) {
                  final Member field = ((AnnotatedField)annotated).getJavaMember();
                  assert field != null;
                  name = field.getName();
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
                    name = parameter.getName();
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
                  name = null;
                }
              }
              if (name != null) {
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
                    final String prefix = configuration.value().trim(); // TODO: trim?
                    assert prefix != null;
                    if (!prefix.isEmpty()) {
                      name = new StringBuilder(prefix).append(".").append(name).toString();
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
        returnValue = new ConfigurationValueMetadata(configurationCoordinates, name, defaultValue);
      }
    }
    return returnValue;
  }
  

  /*
   * Inner and nested classes.
   */


  private static final class ConfigurationValueMetadata {

    private final Map<String, String> configurationCoordinates;

    private final String name;

    private final String defaultValue;
    
    private ConfigurationValueMetadata(final Map<String, String> configurationCoordinates,
                                                     final String name,
                                                     final String defaultValue) {
      super();
      Objects.requireNonNull(name);
      if (configurationCoordinates == null || configurationCoordinates.isEmpty()) {
        this.configurationCoordinates = Collections.emptyMap();
      } else {
        this.configurationCoordinates = Collections.unmodifiableMap(configurationCoordinates);
      }
      this.name = name;
      if (defaultValue == null || defaultValue.equals(ConfigurationValue.NULL)) {
        this.defaultValue = null;
      } else {
        this.defaultValue = defaultValue;
      }
    }

    public final Map<String, String> getConfigurationCoordinates() {
      return this.configurationCoordinates;
    }

    public final String getName() {
      return this.name;
    }

    public final String getDefaultValue() {
      return this.defaultValue;
    }

    @Override
    public final int hashCode() {
      int hashCode = 17;

      final Object configurationCoordinates = this.getConfigurationCoordinates();
      int c = configurationCoordinates == null ? 0 : configurationCoordinates.hashCode();
      hashCode = 37 * hashCode + c;

      final Object name = this.getName();
      c = name == null ? 0 : name.hashCode();
      hashCode = 37 * hashCode + c;

      final Object defaultValue = this.getDefaultValue();
      c = defaultValue == null ? 0 : defaultValue.hashCode();
      hashCode = 37 * hashCode + c;

      return hashCode;
    }

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

        final Object name = this.getName();
        if (name == null) {
          if (her.getName() != null) {
            return false;
          }
        } else if (!name.equals(her.getName())) {
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

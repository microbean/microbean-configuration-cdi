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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;

import javax.enterprise.event.Observes;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

import javax.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;

import org.microbean.configuration.Configurations;

import org.microbean.configuration.cdi.annotation.ConfigurationValue;

import org.microbean.configuration.spi.Converter;

public class ConfigurationsExtension implements Extension {

  public ConfigurationsExtension() {
    super();
  }
  
  private final void addConfigurations(@Observes final BeforeBeanDiscovery event) {
    if (event != null) {
      final AnnotatedTypeConfigurator<Configurations> configurator = event.addAnnotatedType(Configurations.class, "configurations");
      assert configurator != null;
      configurator.add(ApplicationScoped.Literal.INSTANCE);
    }
  }

  private final void installConfigurationValueProducerMethods(@Observes final AfterBeanDiscovery event, final BeanManager beanManager) {
    if (event != null && beanManager != null) {
      final Instance<Object> i = beanManager.createInstance();
      assert i != null;
      final Instance<Configurations> configurationsInstance = i.select(Configurations.class);
      assert configurationsInstance != null;
      if (configurationsInstance.isResolvable()) {
        final Configurations configurations = configurationsInstance.get();
        assert configurations != null;
        final Map<Type, Converter<?>> convertersMap = configurations.getConverters();
        assert convertersMap != null;
        if (!convertersMap.isEmpty()) {
          final Collection<Entry<Type, Converter<?>>> entrySet = convertersMap.entrySet();
          assert entrySet != null;
          assert !entrySet.isEmpty();

          final AnnotatedType<ConfigurationsExtension> thisType = beanManager.createAnnotatedType(ConfigurationsExtension.class);
          final AnnotatedMethod<? super ConfigurationsExtension> producerMethod = thisType.getMethods().stream()
            .filter(m -> m.getJavaMember().getName().equals("produceConfigurationValue"))
            .findFirst()
            .get();
          final BeanAttributes<?> producerAttributes = beanManager.createBeanAttributes(producerMethod);
                                                        
          for (final Entry<Type, Converter<?>> entry : entrySet) {
            assert entry != null;
            final Type type = entry.getKey();
            assert type != null;
            final Converter<?> converter = entry.getValue();
            assert converter != null;
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

  @ConfigurationValue
  @Dependent
  private static final Object produceConfigurationValue(final InjectionPoint injectionPoint, final Configurations configurations) {
    Objects.requireNonNull(injectionPoint);
    Objects.requireNonNull(configurations);
    final String name = getConfigurationPropertyName(injectionPoint);
    assert name != null;
    final Object returnValue = configurations.getValue(Collections.emptyMap(), name, injectionPoint.getType());
    return returnValue;
  }

  private static final String getConfigurationPropertyName(final InjectionPoint injectionPoint) {
    String returnValue = null;
    if (injectionPoint != null) {
      final Set<Annotation> qualifiers = injectionPoint.getQualifiers();
      if (qualifiers != null && !qualifiers.isEmpty()) {
        final Optional<Annotation> configurationValueAnnotation = qualifiers.stream()
          .filter(annotation -> annotation instanceof ConfigurationValue)
          .findFirst();
        if (configurationValueAnnotation != null && configurationValueAnnotation.isPresent()) {
          final ConfigurationValue configurationValue = ConfigurationValue.class.cast(configurationValueAnnotation.get());
          assert configurationValue != null;
          returnValue = configurationValue.value();
          assert returnValue != null;
          if (returnValue.isEmpty()) {
            // Try to get it from the annotated element
            final Object annotated = injectionPoint.getAnnotated();
            assert annotated != null;
            if (annotated instanceof AnnotatedField) {
              final Member field = ((AnnotatedField)annotated).getJavaMember();
              assert field != null;
              returnValue = field.getName();
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
                returnValue = parameter.getName();
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
              returnValue = null;
            }
          }
        }
      }
    }
    return returnValue;
  }

  private static class DelegatingBeanAttributes<T> implements BeanAttributes<T> {

    private final BeanAttributes<?> delegate;
    
    private DelegatingBeanAttributes(final BeanAttributes<?> delegate) {
      super();
      Objects.requireNonNull(delegate);
      this.delegate = delegate;
    }

    @Override
    public String getName() {
      return this.delegate.getName();
    }
    
    @Override
    public Set<Annotation> getQualifiers() {
      return this.delegate.getQualifiers();
    }
    
    @Override
    public Class<? extends Annotation> getScope() {
      return this.delegate.getScope();
    }
    
    @Override
    public Set<Class<? extends Annotation>>	getStereotypes() {
      return this.delegate.getStereotypes();
    }
    
    @Override
    public Set<Type> getTypes() {
      return this.delegate.getTypes();
    }
    
    @Override
    public boolean isAlternative() {
      return this.delegate.isAlternative();
    }

    @Override
    public String toString() {
      return this.delegate.toString();
    }
    
  }

}

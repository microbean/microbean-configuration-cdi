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
import java.lang.reflect.Member;
import java.lang.reflect.Parameter;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;

import javax.enterprise.inject.Produces;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessSyntheticBean;

import javax.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;

import javax.inject.Inject;

import org.microbean.configuration.Configurations;

import org.microbean.configuration.cdi.annotation.ConfigurationValue;

@ApplicationScoped
final class ConfigurationValueProducer {

  private ConfigurationValueProducer() {
    super();
  }

  @Produces
  @Dependent
  @ConfigurationValue
  private static final String produceStringConfigurationValue(final InjectionPoint injectionPoint, final Configurations configurations) {
    Objects.requireNonNull(injectionPoint);
    Objects.requireNonNull(configurations);
    final String name = getConfigurationPropertyName(injectionPoint);
    assert name != null;
    final String returnValue = configurations.getValue(name);
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
  
}

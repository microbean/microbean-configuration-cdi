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
package org.microbean.configuration.cdi.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.util.LinkedHashMap;

import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.Nonbinding;

import javax.inject.Qualifier;

@Documented
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
public @interface ConfigurationCoordinates {

  @Nonbinding
  ConfigurationCoordinate[] value() default {};

  public static final class Literal extends AnnotationLiteral<ConfigurationCoordinates> implements ConfigurationCoordinates {

    private static final long serialVersionUID = 1L;

    private final LinkedHashMap<String, ConfigurationCoordinate> coordinates;

    public Literal() {
      this((LinkedHashMap<String, ConfigurationCoordinate>)null);
    }

    public Literal(final ConfigurationCoordinates coordinates) {
      super();
      this.coordinates = new LinkedHashMap<>();
      if (coordinates != null) {
        final ConfigurationCoordinate[] cs = coordinates.value();
        if (cs != null && cs.length > 0) {
          for (final ConfigurationCoordinate c : cs) {
            if (c != null) {
              this.add(c);
            }
          }
        }
      }
    }
    
    public Literal(final LinkedHashMap<String, ConfigurationCoordinate> coordinates) {
      super();
      if (coordinates == null) {
        this.coordinates = new LinkedHashMap<>();
      } else {
        this.coordinates = coordinates;
      }
    }

    @Nonbinding
    @Override
    public ConfigurationCoordinate[] value() {
      return this.coordinates.values().toArray(new ConfigurationCoordinate[this.coordinates.size()]);
    }

    public ConfigurationCoordinate add(final ConfigurationCoordinate coordinate) {
      ConfigurationCoordinate returnValue = null;
      if (coordinate != null) {
        final String name = coordinate.name();
        assert name != null;
        returnValue = this.coordinates.put(name, coordinate);
      }
      return returnValue;
    }

    public boolean containsKey(final String name) {
      if (name == null) {
        return false;
      } else {
        return this.coordinates.containsKey(name);
      }
    }
    
  }
  
}

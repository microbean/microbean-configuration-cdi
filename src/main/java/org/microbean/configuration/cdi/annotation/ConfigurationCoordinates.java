/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2017–2019 microBean™.
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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.util.LinkedHashMap;

import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.Nonbinding;

import javax.inject.Qualifier;

/**
 * A {@link Qualifier} that houses individual {@link
 * ConfigurationCoordinate} annotations.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see ConfigurationCoordinate
 */
@Documented
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
public @interface ConfigurationCoordinates {

  /**
   * An array of individual {@link ConfigurationCoordinate} instances.
   *
   * @return an array of individual {@link ConfigurationCoordinate} instances
   */  
  @Nonbinding
  ConfigurationCoordinate[] value() default {};


  /*
   * Nested classes.
   */


  /**
   * An {@link AnnotationLiteral} that implements {@link
   * ConfigurationCoordinates}.
   *
   * @author <a href="https://about.me/lairdnelson"
   * target="_parent">Laird Nelson</a>
   *
   * @see ConfigurationCoordinates
   */
  public static final class Literal extends AnnotationLiteral<ConfigurationCoordinates> implements ConfigurationCoordinates {

    
    /*
     * Static fields.
     */


    /*
     * The version of this class for {@linkplain Serializable
     * serialization} purposes.
     *
     * @see Serializable
     */
    private static final long serialVersionUID = 1L;


    /*
     * Instance fields.
     */

    
    /**
     * A {@link LinkedHashMap} of configuration coordinates.
     *
     * <p>This field will never be {@code null}.</p>
     *
     * @see #add(ConfigurationCoordinate)
     */
    private final LinkedHashMap<String, ConfigurationCoordinate> coordinates;


    /*
     * Constructors.
     */


    /**
     * Creates a new {@link Literal}.
     */
    public Literal() {
      this((LinkedHashMap<String, ConfigurationCoordinate>)null);
    }

    /**
     * Creates a new {@link Literal}.
     *
     * @param coordinates the {@link ConfigurationCoordinates} to
     * start with; may be {@code null}.  No reference is kept to this
     * parameter.
     */
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

    /**
     * Creates a new {@link Literal}.
     *
     * @param coordinates the {@link LinkedHashMap} of {@link
     * ConfigurationCoordinate}s to start with; may be {@code null}.
     * No reference is kept to this parameter.
     */
    public Literal(final LinkedHashMap<String, ConfigurationCoordinate> coordinates) {
      super();
      if (coordinates == null || coordinates.isEmpty()) {
        this.coordinates = new LinkedHashMap<>();
      } else {
        this.coordinates = new LinkedHashMap<>(coordinates);
      }
    }


    /*
     * Instance methods.
     */


    /**
     * Returns an array of {@link ConfigurationCoordinate} instances
     * that this {@link Literal} aggregates.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * @return a non-{@code null} array of {@link
     * ConfigurationCoordinate} instances
     *
     * @see ConfigurationCoordinate
     */
    @Nonbinding
    @Override
    public ConfigurationCoordinate[] value() {
      return this.coordinates.values().toArray(new ConfigurationCoordinate[this.coordinates.size()]);
    }

    /**
     * Adds a {@link ConfigurationCoordinate} to this {@link
     * Literal}'s collection of such coordinates.
     *
     * <p>This method may return {@code null}.</p>
     *
     * @param coordinate the {@link ConfigurationCoordinate} to add;
     * may be {@code null} in which case no action will be taken and
     * {@code null} will be returned
     *
     * @return any prior {@link ConfigurationCoordinate} with the same
     * {@linkplain ConfigurationCoordinate#name() name}, or {@code
     * null}
     */
    public ConfigurationCoordinate add(final ConfigurationCoordinate coordinate) {
      ConfigurationCoordinate returnValue = null;
      if (coordinate != null) {
        final String name = coordinate.name();
        assert name != null;
        returnValue = this.coordinates.put(name, coordinate);
      }
      return returnValue;
    }

    /**
     * Returns {@code true} if this {@link Literal} contains a {@link
     * ConfigurationCoordinate} with the supplied {@code name}.
     *
     * @param name the name to look for; may be {@code null} in which
     * case {@code false} will be returned
     *
     * @return {@code true} if this {@link Literal} contains a {@link
     * ConfigurationCoordinate} with the supplied {@code name}; {@code
     * false} otherwise
     *
     * @see ConfigurationCoordinate#name()
     */
    public boolean containsKey(final String name) {
      if (name == null) {
        return false;
      } else {
        return this.coordinates.containsKey(name);
      }
    }
    
  }
  
}

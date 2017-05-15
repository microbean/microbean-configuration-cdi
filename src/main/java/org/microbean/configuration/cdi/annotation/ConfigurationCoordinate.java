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

import javax.enterprise.util.AnnotationLiteral;

import javax.inject.Qualifier;

/**
 * A <a
 * href="https://docs.oracle.com/javase/tutorial/java/annotations/repeating.html"
 * target="_parent">repeatable annotation</a> representing an
 * individual configuration coordinate in a universe of such
 * coordinates.
 *
 * <p>Note that this annotation is deliberately <strong>not</strong> a
 * {@link Qualifier}.</p>
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see ConfigurationCoordinates
 */
@Documented
@Repeatable(ConfigurationCoordinates.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
public @interface ConfigurationCoordinate {

  /**
   * The name of the configuration coordinate.
   *
   * @return the name of the configuration coordinate
   */
  String name();

  /**
   * The value for the configuration coordinate.
   *
   * @return the value for the configuration coordinate
   */
  String value() default "";


  /*
   * Nested classes.
   */


  /**
   * An {@link AnnotationLiteral} that implements {@link ConfigurationCoordinate}.
   *
   * @author <a href="https://about.me/lairdnelson"
   * target="_parent">Laird Nelson</a>
   *
   * @see ConfigurationCoordinate
   */
  public static final class Literal extends AnnotationLiteral<ConfigurationCoordinate> implements ConfigurationCoordinate {


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
     * The name of the configuration coordinate this {@link Literal}
     * represents.
     *
     * <p>This field will never be {@code null}.</p>
     *
     * @see #name()
     */
    private final String name;

    /**
     * The value of the configuration coordinate this {@link Literal}
     * represents.
     *
     * <p>This field will never be {@code null}.</p>
     *
     * @see #value()
     */
    private final String value;


    /*
     * Constructors.
     */


    /**
     * Creates a new {@link Literal}.
     *
     * @param name the name of the configuration coordinate this
     * {@link Literal} represents; may be {@code null} in which case
     * the {@linkplain String#isEmpty() empty <code>String</code>}
     * will be used instead
     *
     * @param value the value of the configuration coordinate this
     * {@link Literal} represents; may be {@code null} in which case
     * the {@linkplain String#isEmpty() empty <code>String</code>}
     * will be used instead
     *
     * @see #name()
     *
     * @see #value()
     */
    public Literal(final String name, final String value) {
      super();
      this.name = name == null ? "" : name;
      this.value = value == null ? "" : value;
    }


    /*
     * Instance methods.
     */


    /**
     * Returns the name of the configuration coordinate this {@link
     * Literal} represents.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * @return the name of the configuration coordinate this {@link
     * Literal} represents; never {@code null}
     *
     * @see ConfigurationCoordinate#name()
     */
    @Override
    public final String name() {
      return this.name;
    }

    /**
     * Returns the value of the configuration coordinate this {@link
     * Literal} represents.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * @return the value of the configuration coordinate this {@link
     * Literal} represents; never {@code null}
     *
     * @see ConfigurationCoordinate#value()
     */
    @Override
    public final String value() {
      return this.value;
    }

  }
  
}

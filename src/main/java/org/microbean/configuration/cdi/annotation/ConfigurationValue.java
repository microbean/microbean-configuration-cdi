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

import java.io.Serializable; // for javadoc only

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.util.ArrayList;
import java.util.Collection;

import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.Nonbinding;

import javax.inject.Qualifier;

/**
 * A {@link Qualifier} annotation indicating that the annotated
 * element is related to a particular configuration value in some way.
 *
 * <p>In the case of method parameters and instance fields, this
 * usually means that the annotated element would like to receive a
 * particular configuration value from the microBean Configuration
 * framework.</p>
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see ConfigurationCoordinate
 *
 * @see Configuration
 */
@Documented
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
public @interface ConfigurationValue {


  /*
   * Static fields.
   */


  /**
   * A {@link String} that stands in for {@code null}&mdash;annotation
   * elements cannot be set to {@code null} so if the {@link
   * #defaultValue()} element in this class returns the value of this
   * field that value is to be interpreted as if it were actually
   * {@code null}.
   *
   * @see #value()
   *
   * @see #defaultValue()
   */
  public static final String NULL = "\0";


  /*
   * Elements.
   */


  /**
   * The names of the configuration value in question.
   *
   * <p>Each name in the returned array represents a name of a
   * configuration property whose value will be sought in order.</p>
   *
   * <p>If this element is not specified, or if it is an empty array,
   * the sole effective configuration value name will be the name of
   * the annotated element upon which this {@link ConfigurationValue}
   * appears.</p>
   *
   * <p>This element will never return {@code null}.</p>
   *
   * @return the name of the configuration value in question; never
   * {@code null} or {@link ConfigurationValue#NULL NULL}
   */
  @Nonbinding
  String[] value() default {};

  /**
   * The {@link String} representation of the default value to use in
   * case no suitable configuration value may be found.
   *
   * <p>This element may return {@link #NULL NULL}.</p>
   *
   * @return the {@link String} representation of the default value to
   * use in case no suitable configuration value may be found, or
   * {@link #NULL NULL}
   *
   * @see #NULL
   */
  @Nonbinding
  String defaultValue() default NULL;


  /*
   * Inner and nested classes.
   */
  

  /**
   * An {@link AnnotationLiteral} representing a runtime instance of
   * the {@link ConfigurationValue} annotation.
   *
   * @author <a href="https://about.me/lairdnelson"
   * target="_parent">Laird Nelson</a>
   */
  public static final class Literal extends AnnotationLiteral<ConfigurationValue> implements ConfigurationValue {


    /*
     * Static fields.
     */
    

    /**
     * A {@link ConfigurationValue} whose {@link
     * ConfigurationValue#value()} returns the empty string.
     *
     * @deprecated This {@link ConfigurationValue} is essentially
     * useless and is slated for removal.
     */
    @Deprecated
    public static final ConfigurationValue INSTANCE = of("");

    /**
     * The version of this class for {@linkplain Serializable
     * serialization purposes}.
     *
     * @see Serializable
     */
    private static final long serialVersionUID = 1L;


    /*
     * Instance fields.
     */


    private final String[] names;

    /**
     * The {@link String} representation of the default value to use
     * for the configuration value in question if no suitable
     * configuration value could be found.
     *
     * <p>This field will never be {@code null} but may be {@link
     * ConfigurationValue#NULL NULL}.</p>
     *
     * @see #defaultValue()
     *
     * @see ConfigurationValue#NULL
     */
    private final String defaultValue;


    /*
     * Constructors.
     */


    /**
     * Creates a new {@link ConfigurationValue.Literal}.
     *
     * @param name the name of the configuration value; may be {@code
     * null} or {@link ConfigurationValue#NULL NULL} in which case an
     * empty array will be used instead
     *
     * @param defaultValue the {@link String} representation of the
     * default value to use when a suitable configuration value cannot
     * be found; may be {@code null} in which case {@link
     * ConfigurationValue#NULL NULL} will be used instead
     *
     * @see #value()
     *
     * @see #defaultValue()
     *
     * @see #Literal(String[], String)
     */
    private Literal(final String name, final String defaultValue) {
      this(name == null ? new String[0] : NULL.equals(name) ? new String[0] : new String[] { name }, defaultValue);
    }

    /**
     * Creates a new {@link ConfigurationValue.Literal}.
     *
     * @param names the names of the configuration value; may be
     * {@code null} in which case an empty array will be used instead
     *
     * @param defaultValue the {@link String} representation of the
     * default value to use when a suitable configuration value cannot
     * be found; may be {@code null} in which case {@link
     * ConfigurationValue#NULL NULL} will be used instead
     *
     * @see #value()
     *
     * @see #defaultValue()
     */
    private Literal(final String[] names, final String defaultValue) {
      super();
      if (names == null || names.length <= 0) {
        this.names = new String[0];
      } else {
        final Collection<String> nameCollection = new ArrayList<>();
        for (final String name : names) {
          if (name != null && !name.isEmpty() && !name.equals(NULL)) {
            nameCollection.add(name);
          }
        }
        this.names = nameCollection.toArray(new String[nameCollection.size()]);
      }
      this.defaultValue = defaultValue == null ? NULL : defaultValue;
    }


    /*
     * Instance methods.
     */

    
    /**
     * Returns the {@link String} representation of the default value to use
     * for the configuration value in question if no suitable
     * configuration value could be found.
     *
     * <p>This method will never return {@code null} but may return
     * {@link ConfigurationValue#NULL NULL}.</p>
     *
     * @return the {@link String} representation of the default value to use
     * for the configuration value in question if no suitable
     * configuration value could be found; never {@code null}
     * 
     * @see ConfigurationValue#defaultValue()
     *
     * @see ConfigurationValue#NULL
     */
    @Override
    public final String defaultValue() {
      return this.defaultValue;
    }

    /**
     * Returns the names of the configuration value in question.
     *
     * <p>This field will never return {@code null}.</p>
     *
     * @return the names of the configuration value in question; never
     * {@code null}
     *
     * @see ConfigurationValue#value()
     */
    @Override
    public final String[] value() {
      final String[] returnValue;
      if (this.names.length <= 0) {
        returnValue = this.names;
      } else {
        final String[] array = new String[this.names.length];
        System.arraycopy(this.names, 0, array, 0, this.names.length);
        returnValue = array;
      }
      return returnValue;
    }


    /*
     * Static methods.
     */


    /**
     * Returns a new {@link Literal} representing a {@link
     * ConfigurationValue} instance whose {@link
     * ConfigurationValue#value()} method will return the supplied
     * {@code name}.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * @param name the name of the configuration value in question;
     * may be {@code null} or {@link ConfigurationValue#NULL NULL} in
     * which case {@code ""} will be used instead
     *
     * @return a non-{@code null} {@link Literal}
     *
     * @see ConfigurationValue#value()
     */
    public static final Literal of(final String name) {
      return new Literal(name, NULL);
    }

    /**
     * Returns a new {@link Literal} representing a {@link
     * ConfigurationValue} instance whose {@link
     * ConfigurationValue#value()} method will return the supplied
     * {@code names}.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * @param names the names of the configuration value in question;
     * may be {@code null} in which case an empty array will be used
     * instead
     *
     * @return a non-{@code null} {@link Literal}
     *
     * @see ConfigurationValue#value()
     */
    public static final Literal of(final String[] names) {
      return new Literal(names, NULL);
    }
    
    /**
     * Returns a new {@link Literal} representing a {@link
     * ConfigurationValue} instance whose {@link
     * ConfigurationValue#value()} method will return an array
     * containing only the supplied {@code name} and whose {@link
     * ConfigurationValue#defaultValue()} method will return the
     * supplied {@code defaultValue}.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * @param name the name of the configuration value in question;
     * may be {@code null} or {@link ConfigurationValue#NULL NULL} in
     * which case {@code ""} will be used instead
     *
     * @param defaultValue the default value for the configuration
     * value in question; may be {@code null} in which case {@link
     * ConfigurationValue#NULL NULL} will be used instead
     *
     * @return a non-{@code null} {@link Literal}
     *
     * @see ConfigurationValue#value()
     *
     * @see ConfigurationValue#defaultValue()
     */
    public static final Literal of(final String name, final String defaultValue) {
      return new Literal(name, defaultValue);
    }

    /**
     * Returns a new {@link Literal} representing a {@link
     * ConfigurationValue} instance whose {@link
     * ConfigurationValue#value()} method will return an array
     * containing only the supplied {@code names} and whose {@link
     * ConfigurationValue#defaultValue()} method will return the
     * supplied {@code defaultValue}.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * @param names the names of the configuration value in question;
     * may be {@code null} or in which case an empty array will be
     * used instead
     *
     * @param defaultValue the default value for the configuration
     * value in question; may be {@code null} in which case {@link
     * ConfigurationValue#NULL NULL} will be used instead
     *
     * @return a non-{@code null} {@link Literal}
     *
     * @see ConfigurationValue#value()
     *
     * @see ConfigurationValue#defaultValue()
     */
    public static final Literal of(final String[] names, final String defaultValue) {
      return new Literal(names, defaultValue);
    }
    
  }
  
}

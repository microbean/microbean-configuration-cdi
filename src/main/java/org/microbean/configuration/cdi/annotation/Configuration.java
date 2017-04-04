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

import java.io.Serializable; // for javadoc only

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.Nonbinding;

import javax.inject.Qualifier; // for javadoc only

/**
 * An annotation&mdash;not a {@link Qualifier}&mdash;identifying a
 * subspace within a larger configuration value space located in
 * <em>overall</em> configuration space by configuration coordinates.
 *
 * <p>Less abstractly, this annotation identifies a configuration
 * <em>prefix</em> "underneath" which actual configuration values may
 * be found.</p>
 *
 * <p>As an example, a class may be annotated with:</p>
 *
 * <blockquote><pre>@Configuration("java")</pre></blockquote>
 *
 * <p>...and one of its method parameters may be annotated with:</p>
 *
 * <blockquote><pre>@ConfigurationValue("home")</pre></blockquote>
 *
 * <p>...and so the ultimate configuration value that would be sought
 * would be {@code java.home} (the underlying configuration system may
 * store values under this name in a variety of configuration value
 * subspaces).  Note that this does not, by itself, specify inside of
 * which configuration value subspace&mdash;identified by
 * configuration coordinates&mdash;the {@code java} configuration
 * value subspace resides.  In other words, this annotation is
 * <em>not</em> to be confused with {@link
 * ConfigurationCoordinate}.</p>
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see ConfigurationCoordinate
 *
 * @see ConfigurationValue
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
public @interface Configuration {


  /*
   * Elements.
   */
  

  /**
   * The name of the configuration value subspace this {@link Configuration}
   * annotation represents.
   *
   * @return the name of the configuration value subspace this {@link
   * Configuration} annotation represents; never {@code null}
   */
  @Nonbinding
  String value() default "";


  /*
   * Inner and nested classes.
   */

  
  /**
   * An {@link AnnotationLiteral} representing a runtime instance of
   * the {@link Configuration} annotation.
   *
   * @author <a href="https://about.me/lairdnelson"
   * target="_parent">Laird Nelson</a>
   *
   * @see Configuration
   */
  public static final class Literal extends AnnotationLiteral<Configuration> implements Configuration {


    /*
     * Static fields.
     */


    /**
     * A {@link Configuration} whose {@link Configuration#value()}
     * element will return {@code ""}.
     *
     * <p>This field is never {@code null}.</p>
     *
     * @deprecated This field is essentially useless and is slated for
     * removal.
     */
    @Deprecated
    public static final Configuration INSTANCE = of("");

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


    /**
     * The name of the configuration value subspace represented by
     * this {@link Literal}.
     *
     * <p>This field is never {@code null}.</p>
     *
     * @see #value()
     */
    private final String name;


    /*
     * Constructors.
     */
    

    /**
     * Creates a new {@link Literal}.
     *
     * @param name the name of the configuration value subspace
     * represented by this {@link Literal}; may be {@code null} in
     * which case {@code ""} will be used instead
     *
     * @see #value()
     */
    private Literal(final String name) {
      super();
      this.name = name == null ? "" : name;
    }


    /*
     * Instance methods.
     */


    /**
     * Returns the name of the configuration value subspace this
     * {@link Literal} represents.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * @return the non-{@code null} name of the configuration value
     * subspace this {@link Literal} represents
     *
     * @see Configuration#value()
     */
    public final String value() {
      return this.name;
    }

    
    /*
     * Static methods.
     */


    /**
     * Returns a new {@link Literal} representing the configuration
     * value subspace identified by the supplied {@code name}.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * @param name the name of the configuration value subspace the
     * new {@link Literal} will represent; may be {@code null} in
     * which case {@code ""} will be used instead
     *
     * @return a new, non-{@code null} {@link Literal}
     *
     * @see #value()
     */
    public static final Literal of(final String name) {
      return new Literal(name == null ? "" : name);
    }

  }
  
}

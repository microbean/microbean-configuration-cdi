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

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.event.Observes;

import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

import javax.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;

import org.microbean.configuration.Configurations;

public class ConfigurationsExtension implements Extension {

  public ConfigurationsExtension() {
    super();
  }
  
  private final void addConfigurations(@Observes final BeforeBeanDiscovery event) {

    // Be careful to not make this method static: https://issues.jboss.org/browse/WELD-2331
    
    if (event != null) {
      final AnnotatedTypeConfigurator<Configurations> configurator = event.addAnnotatedType(Configurations.class, "configurations");
      assert configurator != null;
      configurator.add(ApplicationScoped.Literal.INSTANCE);
    }
  }

}

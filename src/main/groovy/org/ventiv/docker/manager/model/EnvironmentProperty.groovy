/*
 * Copyright (c) 2014 - 2017 Ventiv Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.ventiv.docker.manager.model

import com.fasterxml.jackson.annotation.JsonIgnore
import org.ventiv.docker.manager.utils.CachingGroovyShell

/**
 * Created by jcrygier on 7/27/17.
 */
class EnvironmentProperty {

    String name;
    String value;
    @JsonIgnore CachingGroovyShell cachingGroovyShell;

    String comments;
    boolean secure = false;
    Collection<String> propertySets = []
    Collection<String> tiers = []
    Collection<String> environments = []
    Collection<String> applications = []

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        EnvironmentProperty that = (EnvironmentProperty) o

        if (name != that.name) return false
        if (value != that.value) return false

        return true
    }

    int hashCode() {
        int result
        result = (name != null ? name.hashCode() : 0)
        result = 31 * result + (value != null ? value.hashCode() : 0)
        return result
    }
}

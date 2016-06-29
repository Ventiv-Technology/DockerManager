/*
 * Copyright (c) 2014 - 2016 Ventiv Technology
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
package org.ventiv.docker.manager.utils

import java.security.AccessController
import java.security.PrivilegedAction
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by jcrygier on 6/29/16.
 */
class CachingGroovyShell {

    public static final AtomicInteger counter = new AtomicInteger();
    private final Script groovyScript;

    public CachingGroovyShell(String groovyCode) {
        GroovyCodeSource gcs = AccessController.doPrivileged(new PrivilegedAction<GroovyCodeSource>() {
            public GroovyCodeSource run() {
                return new GroovyCodeSource(groovyCode, "CachingGroovyShell${counter.incrementAndGet()}.groovy", GroovyShell.DEFAULT_CODE_BASE);
            }
        });

        groovyScript = new GroovyShell().parse(gcs);
    }

    public Object eval(Map<String, Object> binding) {
        synchronized (groovyScript) {
            groovyScript.setBinding(new Binding(binding));
            return groovyScript.run();
        }
    }

}

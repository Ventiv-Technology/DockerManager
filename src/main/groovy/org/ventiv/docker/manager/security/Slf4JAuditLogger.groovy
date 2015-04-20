/*
 * Copyright (c) 2014 - 2015 Ventiv Technology
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
package org.ventiv.docker.manager.security

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.security.acls.domain.AuditLogger
import org.springframework.security.acls.model.AccessControlEntry
import org.springframework.security.acls.model.AuditableAccessControlEntry
import org.springframework.util.Assert

/**
 * Created by jcrygier on 4/17/15.
 */
@Slf4j
@CompileStatic
class Slf4JAuditLogger implements AuditLogger {

    @Override
    void logIfNeeded(boolean granted, AccessControlEntry ace) {
        Assert.notNull(ace, "AccessControlEntry required");

        if (ace instanceof AuditableAccessControlEntry) {
            AuditableAccessControlEntry auditableAce = (AuditableAccessControlEntry) ace;

            if (granted && auditableAce.isAuditSuccess()) {
                log.debug("GRANTED due to ACE: " + ace);
            } else if (!granted && auditableAce.isAuditFailure()) {
                log.debug("DENIED due to ACE: " + ace);
            }
        }
    }

}

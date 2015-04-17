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

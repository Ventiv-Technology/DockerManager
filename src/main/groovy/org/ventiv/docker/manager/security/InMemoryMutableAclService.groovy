package org.ventiv.docker.manager.security

import groovy.transform.CompileStatic
import org.springframework.security.acls.domain.AclAuthorizationStrategy
import org.springframework.security.acls.domain.AclImpl
import org.springframework.security.acls.domain.AuditLogger
import org.springframework.security.acls.model.Acl
import org.springframework.security.acls.model.AlreadyExistsException
import org.springframework.security.acls.model.ChildrenExistException
import org.springframework.security.acls.model.MutableAcl
import org.springframework.security.acls.model.MutableAclService
import org.springframework.security.acls.model.NotFoundException
import org.springframework.security.acls.model.ObjectIdentity
import org.springframework.security.acls.model.Sid
import org.springframework.stereotype.Service

/**
 * In Memory implementation of MutableAclService that does just the basics
 */
@Service
@CompileStatic
class InMemoryMutableAclService implements MutableAclService {

    AuditLogger auditLogger = new Slf4JAuditLogger();
    AclAuthorizationStrategy aclAuthorizationStrategy = new AclAuthorizationStrategy() {
        @Override
        void securityCheck(Acl acl, int changeType) {}      // Do Nothing, we're authorizing everyone
    }

    Map<ObjectIdentity, MutableAcl> aclStore = [:]

    @Override
    MutableAcl createAcl(ObjectIdentity objectIdentity) throws AlreadyExistsException {
        if (aclStore.containsKey(objectIdentity)) {
            throw new AlreadyExistsException("Object identity '" + objectIdentity + "' already exists");
        }

        MutableAcl acl = new AclImpl(objectIdentity, objectIdentity.getIdentifier(), aclAuthorizationStrategy, auditLogger)
        aclStore.put(objectIdentity, acl);

        return acl;
    }

    @Override
    void deleteAcl(ObjectIdentity objectIdentity, boolean deleteChildren) throws ChildrenExistException {
        aclStore.remove(objectIdentity)
    }

    @Override
    MutableAcl updateAcl(MutableAcl acl) throws NotFoundException {
        aclStore.put(acl.getObjectIdentity(), acl);
    }

    @Override
    List<ObjectIdentity> findChildren(ObjectIdentity parentIdentity) {
        return null     // TODO: Implement
    }

    @Override
    Acl readAclById(ObjectIdentity object) throws NotFoundException {
        if (!aclStore.containsKey(object))
            throw new NotFoundException("ObjectIdentity $object is not found in memory");

        return aclStore[object]
    }

    @Override
    Acl readAclById(ObjectIdentity object, List<Sid> sids) throws NotFoundException {
        return readAclById(object);
    }

    @Override
    Map<ObjectIdentity, Acl> readAclsById(List<ObjectIdentity> objects) throws NotFoundException {
        return aclStore.findAll { k, v -> return objects.contains(k) }
    }

    @Override
    Map<ObjectIdentity, Acl> readAclsById(List<ObjectIdentity> objects, List<Sid> sids) throws NotFoundException {
        return readAclsById(objects);
    }

    public void clearAll() {
        aclStore.clear();
    }

}

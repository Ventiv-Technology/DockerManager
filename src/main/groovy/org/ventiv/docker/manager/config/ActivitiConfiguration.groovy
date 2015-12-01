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
package org.ventiv.docker.manager.config

import groovy.transform.CompileStatic
import org.activiti.engine.IdentityService
import org.activiti.engine.RuntimeService
import org.activiti.engine.identity.Group
import org.activiti.engine.identity.User
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Configuration
import org.springframework.security.acls.domain.PrincipalSid
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.ldap.userdetails.InetOrgPerson
import org.ventiv.docker.manager.process.AuthorizationEventListener

import javax.annotation.PostConstruct
import javax.annotation.Resource

/**
 * Created by jcrygier on 4/22/15.
 */
@CompileStatic
@Configuration
class ActivitiConfiguration implements ApplicationListener<AuthenticationSuccessEvent> {

    @Resource IdentityService identityService;
    @Resource RuntimeService runtimeService;
    @Resource AuthorizationEventListener authorizationEventListener;

    /* Attempt at getting explorer integrated
    @Bean
    public ExplorerApplicationServlet explorer() {
        return new ExplorerApplicationServlet();
    }

    @Bean
    public I18nManager i18nManager() {
        return new I18nManager([locale: Locale.US, messageSource: messageSource]);
    }

    @Bean
    public MainWindow mainWindow() {
        return new MainWindow()
    }

    @Bean
    public NavigationFragmentChangeListener navigationFragmentChangeListener() {
        return new NavigationFragmentChangeListener();
    }

    @Bean
    public NavigatorManager navigatorManager() {
        return new NavigatorManager();
    }

    @Bean
    public ExplorerApp explorerApp() {
        DefaultLoginHandler loginHandler = new DefaultLoginHandler() {
            public LoggedInUser authenticate(HttpServletRequest request, HttpServletResponse response) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                User user = identityService.createUserQuery().userId(new PrincipalSid(authentication).getPrincipal()).singleResult();

                return new LoggedInUserImpl(user, authentication.getCredentials().toString());
            }
        };

        def explorer = new ExplorerApp() {
            public void init() {

            }
        };

        explorer.setLoginHandler(loginHandler);
        explorer.setMainWindow(mainWindow());

        return explorer;
    }
    */

    @PostConstruct
    void addListeners() {
        runtimeService.addEventListener(authorizationEventListener);
    }

    @Override
    void onApplicationEvent(AuthenticationSuccessEvent event) {
        User activitiUser = addUserToActiviti(event.getAuthentication());
        event.getAuthentication().getAuthorities().each { def grantedAuthority ->
            addUserToGroup((User) activitiUser, ((GrantedAuthority)grantedAuthority).getAuthority().replaceAll("ROLE_", ""));
        }
    }

    private User addUserToActiviti(Authentication authentication) {
        String userId = new PrincipalSid(authentication).getPrincipal();
        User user = identityService.createUserQuery().userId(userId).singleResult();

        if (user == null)
            user = identityService.newUser(userId);

        // If we're an LDAP InetOrgPerson, grab some additional details
        if (authentication.getPrincipal() instanceof InetOrgPerson) {
            InetOrgPerson person = (InetOrgPerson) authentication.getPrincipal();
            user.setEmail(person.getMail());
            if(person.getDisplayName()) {
                user.setFirstName(person.getDisplayName().split(" ")[0])
                user.setLastName(person.getDisplayName().split(" ")[1])
            }
        }

        user.setPassword(authentication.getCredentials().toString());
        identityService.saveUser(user);

        return user;
    }

    private Group addUserToGroup(User user, String groupId) {
        Group group = identityService.createGroupQuery().groupId(groupId).singleResult();

        if (group == null) {
            group = identityService.newGroup(groupId);
            identityService.saveGroup(group);
        }

        try {
            identityService.createMembership(user.getId(), groupId);
        } catch (Exception ignored) {}

        return group;
    }

}

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

import org.activiti.engine.IdentityService
import org.activiti.engine.RuntimeService
import org.activiti.engine.identity.User
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Configuration
import org.springframework.security.acls.domain.PrincipalSid
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import org.ventiv.docker.manager.process.AuthorizationEventListener

import javax.annotation.PostConstruct
import javax.annotation.Resource

/**
 * Created by jcrygier on 4/22/15.
 */
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
        // Add the user to activiti
        String userId = new PrincipalSid(event.getAuthentication()).getPrincipal();
        User user = identityService.createUserQuery().userId(userId).singleResult();

        if (user == null)
            user = identityService.newUser(new PrincipalSid(event.getAuthentication()).getPrincipal());

        user.setPassword(event.getAuthentication().getCredentials().toString());
        identityService.saveUser(user);
    }
}

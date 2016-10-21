/*
 * Redux Auth Wrapper configurations
 */

import { UserAuthWrapper } from 'redux-auth-wrapper';
import { routerActions } from 'react-router-redux';

export const UserIsAuthenticated = UserAuthWrapper({
  authSelector: (state) => state.getIn(['global', 'user']).toJS(),
  redirectAction: routerActions.replace,
  wrapperDisplayName: 'UserIsAuthenticated',
  predicate: (authData) => {
    console.log('Auth Data', authData);
    return authData && authData.user_name;
  },
});

/*
 * Root App Level Reducer
 *
 * The reducer takes care of our data. Using actions, we can change our
 * application state.
 * To add a new action, add it to the switch statement in the reducer function
 *
 * Example:
 * case YOUR_ACTION_CONSTANT:
 *   return state.set('yourStateVariable', true);
 */

import {
  SET_USER_INFORMATION, SET_HOST_INFORMATION,
} from './constants';
import { fromJS } from 'immutable';

// The initial state of the App
const initialState = fromJS({
  user: {},
});

function rootReducer(state = initialState, action) {
  console.log('Reducing (App):', action);     // TODO: Figure out why commenting this out causes race conditions in store

  switch (action.type) {
    case SET_USER_INFORMATION:
      return state.set('user', fromJS(action.userInfo));
    case SET_HOST_INFORMATION:
      return state.set('hosts', fromJS(action.hostInfo));
    default:
      return state;
  }
}

export default rootReducer;

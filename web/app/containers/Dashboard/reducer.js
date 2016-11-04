/*
 *
 * Dashboard reducer
 *
 */

import { fromJS } from 'immutable';
import {
  DEFAULT_ACTION, UPDATE_LAYOUT,
} from './constants';

const initialState = fromJS({
  configuration: [
    { i: 'asdf', x: 0, y: 0, w: 4, h: 2, dashletName: 'ConnectedHosts' },
  ],
});

function dashboardReducer(state = initialState, action) {
  switch (action.type) {
    case DEFAULT_ACTION:
      return state;
    case UPDATE_LAYOUT:
      return state.mergeDeepIn(['configuration'], action.layout);
    default:
      return state;
  }
}

export default dashboardReducer;

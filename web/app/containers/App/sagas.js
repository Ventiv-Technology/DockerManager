/**
 * Global sagas that should always be running
 *
 * @flow
 */
import { takeLatest } from 'redux-saga';
import { select, put } from 'redux-saga/effects';
import { push } from 'react-router-redux';
import { AUTH_SUCCESS } from 'redux-token-auth/dist/constants';
import { selectLocationState } from './selectors';
import { setUser } from './actions';

function* doOnAuthSuccess(action) {
  // First, peel user information out of our token, and put it in the store
  const userInformation = JSON.parse(atob(action.token.access_token.split('.')[1]));
  yield put(setUser(userInformation));

  // Now, we're officially logged in...feel free to redirect back
  const location = yield select(selectLocationState());
  if (location.locationBeforeTransitions.query && location.locationBeforeTransitions.query.redirect)
    yield put(push(location.locationBeforeTransitions.query.redirect));
}

function* listenForAuthSuccess() {
  yield takeLatest(AUTH_SUCCESS, doOnAuthSuccess);
}

// Bootstrap sagas
export default [
  listenForAuthSuccess,
];

/**
 * Global sagas that should always be running
 *
 * @flow
 */
import { takeLatest } from 'redux-saga';
import { select, put, call } from 'redux-saga/effects';
import { push } from 'react-router-redux';
import { AUTH_SUCCESS } from 'redux-token-auth/dist/constants';
import { selectLocationState } from './selectors';
import { setUser, setHostInfo } from './actions';
import Api, { setToken } from '../../utils/Api';

function* doOnAuthSuccess(action) : Generator<*, *, *> {
  // First, peel user information out of our token, and put it in the store
  const userInformation = JSON.parse(atob(action.token.access_token.split('.')[1]));
  yield put(setUser(userInformation));
  yield call(setToken, action.token.access_token);

  // Now, we're officially logged in...feel free to redirect back
  const location = yield select(selectLocationState());
  if (location && location.locationBeforeTransitions.query && location.locationBeforeTransitions.query.redirect)
    yield put(push(location.locationBeforeTransitions.query.redirect));

  // Load State
  const hostInfo = yield call(Api.getHostInformation);
  yield put(setHostInfo(hostInfo));
}

function* listenForAuthSuccess() : Generator<*, *, *> {
  yield takeLatest(AUTH_SUCCESS, doOnAuthSuccess);
}

// Bootstrap sagas
export default [
  listenForAuthSuccess,
];

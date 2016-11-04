import { SET_USER_INFORMATION, SET_HOST_INFORMATION } from './constants';

/**
 * Changes the input field of the form
 *
 * @param  {userInfo} UserInformation The User Information
 *
 * @return {object}    An action object with a type of SET_USER_INFORMATION
 */
export function setUser(userInfo) {
  return {
    type: SET_USER_INFORMATION,
    userInfo,
  };
}

export function setHostInfo(hostInfo) {
  return {
    type: SET_HOST_INFORMATION,
    hostInfo,
  };
}

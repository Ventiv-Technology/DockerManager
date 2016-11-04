/*
 *
 * Dashboard actions
 *
 */

import {
  DEFAULT_ACTION, UPDATE_LAYOUT,
} from './constants';

import type { Layout } from 'react-grid-layout/build/utils';

export function defaultAction() {
  return {
    type: DEFAULT_ACTION,
  };
}

export function updateLayout(layout : Layout) {
  return {
    type: UPDATE_LAYOUT,
    layout,
  };
}

/**
*
* Connected Hosts Dashlet - Shows which hosts are connected to this instance of Docker Manager.
*
*/

import React from 'react';

import { FormattedMessage } from 'react-intl';
import messages from './messages';

import { Table } from 'react-bootstrap';

export function ConnectedHosts() {
  return (
    <Table striped condensed hover>
      <thead>
        <tr>
          <th><FormattedMessage {...messages.hostName} /></th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td><a href="/images/192.168.99.100">Boot2Docker Host</a></td>
        </tr>
      </tbody>
    </Table>
  );
}

// Dashlet API
ConnectedHosts.title = messages.title;
ConnectedHosts.layout = {
  minH: 2,
  minW: 3,
};

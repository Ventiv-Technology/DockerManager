/**
*
* Connected Hosts Dashlet - Shows which hosts are connected to this instance of Docker Manager.
*
* @flow
*/

import React from 'react';

import { connect } from 'react-redux';
import { FormattedMessage } from 'react-intl';
import messages from './messages';
import { selectHostsInfo } from '../../containers/App/selectors';

import { Table } from 'react-bootstrap';

type PropTypes = {
  hostDetails: Array<any>;
}

const ConnectedHosts = (props : PropTypes) => (
  <Table striped condensed hover>
    <thead>
      <tr>
        <th><FormattedMessage {...messages.hostName} /></th>
      </tr>
    </thead>
    <tbody>
      <tr>
        {props.hostDetails.map((host) => (
          <td key={host.id}><a href={`/images/${host.hostname}`}>{host.description}</a></td>
        ))}
      </tr>
    </tbody>
  </Table>
);

// Dashlet API
ConnectedHosts.title = messages.title;
ConnectedHosts.layout = {
  minH: 2,
  minW: 3,
};

const mapStateToProps = selectHostsInfo();

function mapDispatchToProps(dispatch) {
  return {
    dispatch,
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(ConnectedHosts);

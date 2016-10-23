/*
 *
 * Dashboard
 *
 */

import React from 'react';
import { connect } from 'react-redux';
import selectDashboard from './selectors';
import { FormattedMessage } from 'react-intl';
import { WidthProvider, Responsive } from 'react-grid-layout';
import messages from './messages';
import { Card, CardActions, CardHeader, CardText } from 'material-ui/Card';
import RaisedButton from 'material-ui/RaisedButton';

import { Table, TableBody, TableHeader, TableHeaderColumn, TableRow, TableRowColumn } from 'material-ui/Table';


// styles
import 'react-grid-layout/css/styles.css';
import 'react-resizable/css/styles.css';
import styles from './styles.css';

const ResponsiveReactGridLayout = WidthProvider(Responsive);

export const Dashboard = () => (
  <div className={styles.dashboard}>
    <ResponsiveReactGridLayout
      className="layout"
      breakpoints={{ lg: 1200, md: 996, sm: 768, xs: 480, xxs: 0 }}
      cols={{ lg: 12, md: 10, sm: 6, xs: 4, xxs: 4 }}
      rowHeight={100}
      isResizable
      isDraggable
    >
      <div data-grid={{ x: 0, y: 0, w: 4, h: 2, minW: 4, minH: 2 }} key={"1"}>
        <Card style={{ height: '100%', overflow: 'scroll' }}>
          <CardHeader
            title="Connected Hosts"
          />
          <CardText>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHeaderColumn>Host Name</TableHeaderColumn>
                </TableRow>
              </TableHeader>
              <TableBody stripedRows showRowHover>
                <TableRow>
                  <TableRowColumn>Boot2Docker Host</TableRowColumn>
                </TableRow>
              </TableBody>
            </Table>
          </CardText>
        </Card>
      </div>
      <div data-grid={{ x: 0, y: 0, w: 4, h: 1, minW: 4 }} key={"2"}>2</div>
      <div data-grid={{ x: 0, y: 0, w: 4, h: 1, minW: 4 }} key={"3"}><RaisedButton label="Testing" /></div>
    </ResponsiveReactGridLayout>
  </div>
);

const mapStateToProps = selectDashboard();

function mapDispatchToProps(dispatch) {
  return {
    dispatch,
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(Dashboard);

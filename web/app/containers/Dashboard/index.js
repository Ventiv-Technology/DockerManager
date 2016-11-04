/*
 *
 * Dashboard
 *
 * @flow
 */

import React from 'react';
import { connect } from 'react-redux';
import selectDashboard from './selectors';
import { FormattedMessage, intlShape } from 'react-intl';
import { WidthProvider, Responsive } from 'react-grid-layout';
import messages from './messages';
import { Card, CardActions, CardHeader, CardText } from 'material-ui/Card';
import RaisedButton from 'material-ui/RaisedButton';
import { updateLayout } from './actions';

// import { Table, TableBody, TableHeader, TableHeaderColumn, TableRow, TableRowColumn } from 'material-ui/Table';
import { Table } from 'react-bootstrap';

import * as Dashlets from '../../dashlets';


// styles
import 'react-grid-layout/css/styles.css';
import 'react-resizable/css/styles.css';
import styles from './styles.css';

const ResponsiveReactGridLayout = WidthProvider(Responsive);

type PropTypes = {
  updateLayout: (layout : any) => void;
  configuration: any;
};

export const Dashboard = (props : PropTypes, context : any) => {
  const formatMessage = context.intl.formatMessage;

  return (
    <div className={styles.dashboard}>
      <ResponsiveReactGridLayout
        className="layout"
        breakpoints={{ lg: 1200, md: 996, sm: 768, xs: 480, xxs: 0 }}
        cols={{ lg: 12, md: 10, sm: 6, xs: 4, xxs: 4 }}
        rowHeight={100}
        onLayoutChange={props.updateLayout}
        isResizable
        isDraggable
      >
        {props.configuration.map((dashlet) => {
          const Dashlet = Dashlets[dashlet.dashletName];

          return (
            <div data-grid={{ x: dashlet.x, y: dashlet.y, w: dashlet.w, h: dashlet.h, minW: Dashlet.layout.minW, minH: Dashlet.layout.minH }} key={dashlet.i}>
              <Card style={{ height: '100%', overflow: 'hidden' }}>
                <CardHeader title={formatMessage(Dashlet.title)} />
                <CardText>
                  <Dashlet />
                </CardText>
              </Card>
            </div>
          );
        })}
      </ResponsiveReactGridLayout>
    </div>
  );
};

Dashboard.contextTypes = {
  intl: intlShape,
};

const mapStateToProps = selectDashboard();

function mapDispatchToProps(dispatch) {
  return {
    dispatch,
    updateLayout: (layout) => dispatch(updateLayout(layout)),
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(Dashboard);

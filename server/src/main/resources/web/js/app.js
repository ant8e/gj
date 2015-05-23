/*
 *  Copyright © 2015 Antoine Comte
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

/**
 * @jsx React.DOM
 */


var API = (function () {
    var _activeGraphs = [];
    var _es = null;

    function renew() {
        if (_es != null) {
            _es.close();
        }
        if (_activeGraphs.length > 0) {
            var s = '';
            for (var i = 0; i < _activeGraphs.length; i++) {
                s = s + _activeGraphs[i] + '/';
            }
            _es = new EventSource("/values/" + s);
            _es.addEventListener("message", function (event) {
                var data = JSON.parse(event.data);
                Dispatcher.dispatch(Constants.events.MetricValue + data.metric, data);
            }, false);
        }
    }


    return {
        availableBucketsPromise: function () {
            return Promise.resolve($.ajax('api/buckets')).then(function (res) {
                return  _.sortBy(res, function (b) {
                    return b.name;
                });
            });
        },
        activeGraphs: function () {
            return _activeGraphs;
        },
        addActiveGraphs: function (bucket) {
            if (!_.contains(_activeGraphs, bucket.name)) {
                _activeGraphs.push(bucket.name);
                Dispatcher.dispatch(Constants.events.ActiveGraphChange);
                renew();
            }
        },
        removeActiveGraph: function (bucket) {
            var index = _activeGraphs.indexOf(bucket.name);
            if (index > -1) {
                _activeGraphs.splice(index, 1);
                Dispatcher.dispatch(Constants.events.ActiveGraphChange);
                renew();
            }
        }

    };
})();

var Constants = {
    events: {
        ActiveGraphChange: "ActiveGraphChange",
        MetricValue: "MetricValue."
    }};

var Dispatcher = (function () {

    var listeners = { };


    return {
        on: function (eventName, listener) {
            if (!listeners[eventName]) listeners[eventName] = [];
            listeners[eventName].push(listener);
        },
        dispatch: function (eventName, arg) {
            if (listeners[eventName]) {
                listeners[eventName].map(function (callback) {
                    callback(arg);
                });
            }
        }
    };

})
();

var MainView = React.createClass({
    render: function () {

        var view = undefined;
        switch (this.props.view) {
            case 'dashboard' :
                view = <DashBoard />;
                break;
            case 'settings' :
                view = <Settings />;
                break;
        }
        return (
            <div className="container-fluid">
                <div className="row">
                {view}
                </div>
            </div>
            )
            ;
    }});

var NavBar = React.createClass({
    getInitialState: function () {
        return {selected: 'dashboard'};
    },

    render: function () {
        var currentView = this.props.currentView;
        return (
            <div className="navbar navbar-inverse navbar-fixed-top" role="navigation">
                <div className="container-fluid">
                    <div className="navbar-header">
                        <button type="button" className="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
                            <span className="sr-only">Toggle navigation</span>
                            <span className="icon-bar"></span>
                            <span className="icon-bar"></span>
                            <span className="icon-bar"></span>
                        </button>
                        <a className="navbar-brand" href="#">Graph Junkie</a>
                    </div>
                    <div className="navbar-collapse collapse">
                        <form className="navbar-form navbar-right">
                            <input type="text" className="form-control" placeholder="Search..."/>
                        </form>
                        <ul className="nav navbar-nav navbar-right">
                            <li className={currentView === 'dashboard' ? "active" : ''}>
                                <a href="#" onClick={this.props.onViewChange.bind(null, 'dashboard')}>Dashboard</a>
                            </li>
                            <li className={currentView === 'setting' ? "active" : ''}>
                                <a href="#" onClick={this.props.onViewChange.bind(null, 'settings')}>Settings</a>
                            </li>
                            <li>
                                <a href="#">Help</a>
                            </li>
                        </ul>
                    </div>
                </div>
            </div>

            );
    }});


var DashBoard = React.createClass({
    render: function () {
        return (
            <div>
                <BucketSelection />
                <GraphPanel />
            </div>
            );
    }});

var Settings = React.createClass({
    render: function () {
        return (
            <div>
            underconstruction
            </div>
            );
    }});

//
// BucketSelection
//
var BucketSelection = React.createClass({
    getInitialState: function () {
        return { buckets: []};
    },

    componentDidMount: function () {
        API.availableBucketsPromise().then(function (res) {
            this.setState({buckets: res});
        }.bind(this));
    },

    addBucket: function (b) {
        this.changeActive(b, true);
    },

    removeBucket: function (b) {
        this.changeActive(b, false);
    },

    changeActive: function (bucket, active) {
        var s = this.state.buckets.map(function (item) {
            if (item.name === bucket.name) {
                item.active = active;
            }
            return item;
        });
        this.setState({buckets: s});
        if (active) {
            API.addActiveGraphs(bucket);
        } else
            API.removeActiveGraph(bucket);
    },

    render: function () {
        var that = this;
        var items = this.state.buckets.map(function (bucket) {
            var active = bucket.active
            return (
                <li key={bucket.name} className="list-group-item">
                {bucket.name + ' '}
                    <i className={active ? "fa fa-bar-chart-o" : ""}></i>
                    <span className="pull-right">
                        <button type="button" onClick = {that.addBucket.bind(that, bucket)} className="btn btn-default btn-xs" disabled={active}  >
                            <span className="glyphicon glyphicon-plus"></span>
                        </button>
                        <button type="button" onClick = {that.removeBucket.bind(that, bucket)} className="btn btn-default btn-xs" disabled={!active}>
                            <span className="glyphicon glyphicon-minus"></span>
                        </button>
                    </span>
                </li>   );
        });
        return (
            <div className="col-sm-3 col-md-2 sidebar">
                <ul className="list-group" >
                { items}
                </ul>
            </div>

            );
    }});

var GraphPanel = React.createClass({
    getInitialState: function () {
        return { buckets: API.activeGraphs()};
    },
    componentDidMount: function () {
        Dispatcher.on(Constants.events.ActiveGraphChange, function () {
            this.onGraphChange();
        }.bind(this))
    },
    onGraphChange: function () {
        this.setState({ buckets: API.activeGraphs()});
    },
    render: function () {
        var graphs = this.state.buckets.map(function (bucketName) {
            return (<Graph key={bucketName} bucket={bucketName}/>);
        });
        return (
            <div className="col-sm-9 col-sm-offset-3 col-md-10 col-md-offset-2 main">
                <h3 className="page-header">Dashboard</h3>
                <div className="row placeholders">
                    {graphs}
                </div>
            </div>
            );
    }});

var Graph = React.createClass({
    render: function () {
        return (
            <div className="col-xs-9 col-sm-6 placeholder">
                <RickshawR metric={this.props.bucket}/>
            {this.props.bucket}
            </div>
            );
    }});


//rickshaw
// inspired by  https://gist.github.com/rpflorence/7cdaea0af8e334413502
//
var RickshawR = React.createClass({
    render: function () {
        // 1) render nothing, this way the DOM diff will never try to do
        //    anything to it again, and we get a node to mess with
        return React.DOM.div();
//        return (<div>"heelo"</div>)           ;
    },

    componentDidMount: function () {
        // 2) do DOM lib stuff
        this.node = this.getDOMNode();
        var ele = this.node;
        this.series = [
            {
                name : this.props.metric,
                color: 'steelblue',
                data: [ ]
            }
        ];
        this.graph = new Rickshaw.Graph({
            element: ele,
            renderer: 'line',
            series: this.series
        });

        var xAxis = new Rickshaw.Graph.Axis.Time({
            graph: this.graph,
            ticksTreatment: 'glow',
            timeFixture: new Rickshaw.Fixtures.Time()
        });

        var hoverDetail = new Rickshaw.Graph.HoverDetail( {
            graph: this.graph
        } );

        this.graph.render();

        xAxis.render();

        // 3) call method to reconnect React's rendering
        this.renderGraphContent(this.props);
        Dispatcher.on(Constants.events.MetricValue + this.props.metric, function (data) {
            console.log(data);
            if (this.series[0].data.length >100 ){
                this.series[0].data.shift();
            }
            this.series[0].data.push({x: data.ts/1000, y: data.value});
            this.graph.update();
        }.bind(this));
    },

    componentWillReceiveProps: function (newProps) {
        // 4) render reconnected tree when props change
        this.renderGraphContent(newProps);
    },

    renderGraphContent: function (props) {
        // 5) make a new rendering tree, we've now hidden the DOM
        //    manipulation that jQuery UI dialog did from React and
        //    continue the React render tree, some people call this
        //    a "portal"
      //  React.renderComponent(React.DOM.div({}, props.children), this.node);

        // 6) Call methods on the DOM lib via prop changes

    },

    componentWillUnmount: function () {
        Dispatcher.on(Constants.events.MetricValue + this.props.bucket, function (data) {
            console.log(data);
        })
        // clean up the mess
//        this.graph = undefined;
        $(this.node).html("<span />")
    },


});


var App = React.createClass({
    getInitialState: function () {
        return {currentView: 'dashboard'};
    },
    switchView: function (view) {
        this.setState({currentView: view})
    },
    render: function () {
        return (
            <div>
                <NavBar currentView = {this.state.currentView} onViewChange={this.switchView} />
                <MainView view = {this.state.currentView} />
            </div>
            );
    }});


React.renderComponent(<App />, document.getElementById('content'));
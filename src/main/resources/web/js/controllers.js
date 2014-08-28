'use strict';

/* Controllers */

var myAppControlers = angular.module('myApp.controllers', []);

myAppControlers.controller('MainCtrl', ['$scope', '$location', '$route', function ($scope, $location, $route) {
    $scope.activePath = null;
    $scope.$on('$routeChangeSuccess', function () {
        $scope.activePath = $location.path();
//        console.log($location.path());
    });
}]);


myAppControlers.controller('DashboardCtrl', ['$scope', 'Bucket', 'MetricSource', 'ActiveGraphs', function ($scope, Bucket, metricSource, ActiveGraphs) {

    $scope.values = [];

    $scope.chartSeries = [
        {"name": "Plot", "data": []}

    ];

    $scope.chartConfig = {
        options: {
            chart: {
                type: 'area',
                animation: Highcharts.svg,
                zoomType: 'x'
            },
            plotOptions: {
                area: {
                    fillColor: {
                        linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1},
                        stops: [
                            [0, Highcharts.getOptions().colors[0]],
                            [1, Highcharts.Color(Highcharts.getOptions().colors[0]).setOpacity(0).get('rgba')]
                        ]
                    },
                    marker: {
                        enabled: false
//                        radius: 2
                    },
                    lineWidth: 1,
                    states: {
                        hover: {
                            lineWidth: 1
                        }
                    },
                    threshold: null
                }
            }
        },
        series: $scope.chartSeries,
        title: {
            text: ''
        },
        xAxis: {
            type: 'datetime',
            tickPixelInterval: 150
        },
        credits: {
            enabled: true
        },
        loading: false};


    $scope.subscribe = function (b) {
        metricSource.subscribe($scope, b)
    };

    $scope.unsubscribe = function (b) {
        metricSource.unsubscribe(b)
    };

    $scope.buckets = Bucket.query();

    $scope.ActiveGraphs = ActiveGraphs;

    $scope.showedBuckets = ActiveGraphs.getBuckets();

    $scope.$on('ActiveGraphsChangedEvent', function () {
        $scope.showedBuckets = ActiveGraphs.getBuckets();
    });


}]);


myAppControlers.controller('buckets', ['$scope', 'Bucket', function ($scope, Bucket) {

} ]);

myAppControlers.controller('SettingsCtrl', ['$scope', function ($scope) {

} ]);

myAppControlers.controller('ChartCtrl', ['$scope', 'MetricSource', function ($scope, metricSource) {
    //  $scope.options = { bezierCurve: true, animation :false};
    $scope.options = { animation: false, showScale: false, showTooltips: false, pointDot: false, datasetStrokeWidth: 0.5 };
    $scope.labels = [''];
    $scope.series = [$scope.bucket.name];
    $scope.data = [
        [0]
    ];
    $scope.onClick = function (points, evt) {
        console.log(points, evt);
        $scope.labels.push('');
        $scope.data[0].push(10);
    };


    metricSource.subscribe($scope, $scope.bucket);

    function extracted(metric) {
        var item = JSON.parse(metric.data);
        if (item.metric == $scope.bucket.name) {
           $scope.labels.push('');
            $scope.data[0].push(item.value);
            $scope.$digest();
        }
    }

    $scope.$on('metricvalue', function (event, metric) {
        extracted(metric);
    });

} ]);
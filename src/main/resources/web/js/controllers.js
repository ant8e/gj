'use strict';

/* Controllers */

var myAppControlers = angular.module('myApp.controllers', []);

myAppControlers.controller('MainCtrl', ['$scope', '$location', '$route', function ($scope, $location, $route) {
    $scope.activePath = null;
    $scope.$on('$routeChangeSuccess', function () {
        $scope.activePath = $location.path();
        console.log($location.path());
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

    $scope.showedBuckets = ActiveGraphs.getBuckets();

    $scope.addBucket = function (b) {
        ActiveGraphs.addBucket(b);
    };

    $scope.removeBucket = function (i) {
        ActiveGraphs.removeBucket(i);
    };

}]);

myAppControlers.controller('buckets', ['$scope', 'Bucket', function ($scope, Bucket) {

} ]);

myAppControlers.controller('SettingsCtrl', ['$scope', function ($scope) {

} ]);
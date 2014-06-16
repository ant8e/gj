'use strict';

/* Controllers */

var myAppControlers = angular.module('myApp.controllers', []);

myAppControlers.controller('DashboardCtrl', ['$scope', 'Bucket', 'metricSource', function ($scope, Bucket, metricSource) {

    $scope.values = [];

    $scope.chartSeries = [
        {"name": "Plot", "data": []}

    ];

    $scope.chartConfig = {
        options: {
            chart: {
                type: 'spline',
                animation: Highcharts.svg,
            },
            plotOptions: {
                series: {
                    stacking: ''
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
    }
    $scope.unsubscribe = function (b) {
        metricSource.unsubscribe(b)
    }

    $scope.buckets = Bucket.query();
    $scope.showedBuckets = []

}]);

myAppControlers.controller('buckets', ['$scope', 'Bucket', function ($scope, Bucket) {

} ]);

myAppControlers.controller('SettingsCtrl', ['$scope', function ($scope) {

} ]);
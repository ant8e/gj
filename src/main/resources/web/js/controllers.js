'use strict';

/* Controllers */

var myAppControlers = angular.module('myApp.controllers', []);

myAppControlers.controller('MyCtrl1', ['$scope', 'Bucket', 'metricSource', function ($scope, Bucket, metricSource) {

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
            text: 'Hello'
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

    console.log($scope.buckets)

}]);

myAppControlers.controller('buckets', ['$scope', 'Bucket', function ($scope, Bucket) {

} ]);
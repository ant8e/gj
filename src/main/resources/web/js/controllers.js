'use strict';

/* Controllers */

var myAppControlers = angular.module('myApp.controllers', []);

myAppControlers.controller('MyCtrl1', ['$scope', 'Bucket', function ($scope, Bucket) {

    $scope.values = [];

    $scope.chartSeries = [
        {"name": "Plot", "data": [1, 2, 4, 7, 3]}

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
        loading: false,

        initFunction : function (chart) {
        var series = chart.series[0];
        var addMsg = function (msg) {
            var items = JSON.parse(msg.data),
            shift = series.data.length > 20
            series.addPoint([items.ts, items.value], true, shift);

        };
        var feed = new EventSource("/values/test.bucket.0");
        feed.addEventListener("message", addMsg, false);

    }

};


    $scope.addMsg = function (msg) {
        $scope.$apply(function () {
            var items = JSON.parse(msg.data);
            $scope.values.push(items);
            $scope.chartConfig.series[0].data.push(JSON.parse(msg.data).value)
            //  $scope.chart1.series[0].addPoint([items.ts, items.value], true, true);

        });
    };

    /** start listening on messages from selected room */
    /*  $scope.listen = function () {
     $scope.chatFeed = new EventSource("/values/test.bucket");
     $scope.chatFeed.addEventListener("message", $scope.addMsg, false);
     };
     */
    //$scope.listen();

    $scope.buckets = Bucket.query();

    console.log($scope.buckets)

}]);

myAppControlers.controller('buckets', ['$scope', 'Bucket', function ($scope, Bucket) {

} ]);
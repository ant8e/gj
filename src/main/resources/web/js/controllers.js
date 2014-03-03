'use strict';

/* Controllers */

angular.module('myApp.controllers', []).
    controller('MyCtrl1', function ($scope, $http) {

        $scope.values = [];

        $scope.chartSeries = [
            {"name": "Plot", "data": [1, 2, 4, 7, 3]}

        ];

        $scope.chartConfig = {
            options: {
                chart: {
                    type: 'spline'
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
            loading: false
        };


        $scope.addMsg = function (msg) {
            $scope.$apply(function () {
                console.log(msg)
                var items = JSON.parse(msg.data);
                $scope.values.push(items);
                $scope.chartConfig.series[0].data.push(JSON.parse(msg.data).value)
              //  $scope.chart1.series[0].addPoint([items.ts, items.value], true, true);

            });
        };

        /** start listening on messages from selected room */
        $scope.listen = function () {
            $scope.chatFeed = new EventSource("/bucket/test.bucket");
            $scope.chatFeed.addEventListener("message", $scope.addMsg, false);
        };

         $scope.listen();

    })
    .controller('MyCtrl2', [function () {

    }]);
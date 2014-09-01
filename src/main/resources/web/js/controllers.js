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

myAppControlers.controller('BucketButtonCtrl', ['$scope', 'ActiveGraphs', function ($scope, ActiveGraphs) {
    $scope.isActive = false;
    $scope.$on('ActiveGraphsChangedEvent', function () {
        var showedBuckets = ActiveGraphs.getBuckets();
        $scope.isActive = _.contains(showedBuckets, $scope.bucket)
    });
} ]);


myAppControlers.controller('ChartCtrl', ['$scope', 'MetricSource', function ($scope, metricSource) {

    $scope.options = {
        renderer: 'area', stroke: true,
        preserve: true
    };
    $scope.features = {
        palette: 'colorwheel',
        legend: {
            toggle: true,
            highlight: true
        },
        xAxis: {
            timeUnit: {
                ticksTreatment: 'glow',
                timeFixture: new Rickshaw.Fixtures.Time.Local()
            }
        },
        hover: {
            xFormatter: function (x) {
                return new Date(x * 1000).toString();
            }
        }
    };

    $scope.series = [
        {
            name: $scope.bucket.name,
            data: [
//                {x: 0, y: 230},
//                {x: 1, y: 1500},
//                {x: 2, y: 790},
//                {x: 3, y: 310},
//                {x: 4, y: 600}
            ]
        }
    ];


    metricSource.subscribe($scope, $scope.bucket);

    function extracted(metric) {
        var item = JSON.parse(metric.data);
        if (item.metric == $scope.bucket.name) {
            $scope.$apply(function (scope) {
                scope.series[0].data.push({y: item.value, x: item.ts / 1000});
            });

        }
    }

    $scope.$on('metricvalue', function (event, metric) {
        extracted(metric);
    });

} ])
;
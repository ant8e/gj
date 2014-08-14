'use strict';

/* Services */


var myAppServices = angular.module('myApp.services', ['ngResource']);

myAppServices.factory('Bucket', ['$resource',
    function ($resource) {
        return $resource('api/buckets', {}, {});
    }]);

myAppServices.factory('MetricSource', function () {
    var es = null;
    var buckets = {};
    return {

        renew: function ($scope) {
            if (es != null) {
                es.close();
            }
            var s = '';
            for (var b in buckets) {
                if (buckets.hasOwnProperty(b) && buckets[b])
                    s = s + b + '/';
            }

            es = new EventSource("/values/" + s);
            es.addEventListener("message", function (event) {
                $scope.$broadcast('metricvalue', event)
            }, false);
        },

        subscribe: function ($scope, b) {
            buckets[b.name] = true;
            this.renew($scope);
        },
        unsubscribe: function ($scope, b) {
            buckets[b.name] = false;
            this.renew($scope);

        }
    };
});

myAppServices.factory('ActiveGraphs', function () {
    var showedBuckets = [];
    return {
        getBuckets: function () {
            return showedBuckets;
        },
        addBucket: function (b) {

            if (b != null && b != "") {
                showedBuckets.push(b);
            }
        },
        removeBucket: function (i) {
            showedBuckets.splice(i, 1)
        }

    };
});
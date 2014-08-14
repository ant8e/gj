'use strict';

/* Services */


angular.module('myApp.services', ['ngResource'])
    .factory('Bucket', ['$resource',
        function ($resource) {
            return $resource('api/buckets', {}, {});
        }])
    .factory('MetricSource', function () {
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
    })
    .factory('ActiveGraphs', function () {
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
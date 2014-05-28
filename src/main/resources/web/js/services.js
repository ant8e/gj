'use strict';

/* Services */


// Demonstrate how to register services
// In this case it is a simple value service.
angular.module('myApp.services', ['ngResource'])
    .factory('Bucket', ['$resource',
        function ($resource) {
            return $resource('api/buckets', {}, {});
        }])
    .factory('metricSource', function () {
        var es = null;
        var buckets = {};
        return {

            renew: function ($scope) {
                if (es != null) {
                    es.close();
                }
                var s = '';
                for (var b in buckets) {
                    if (buckets.hasOwnProperty( b) && buckets[b])
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
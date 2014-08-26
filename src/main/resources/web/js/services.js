'use strict';

/* Services */


var myAppServices = angular.module('myApp.services', ['ng','ngResource']);

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

myAppServices.factory('ActiveGraphs', ['$rootScope', function ($rootScope) {
    var showedBuckets = [];

    var notifyChange = function (){
        $rootScope.$broadcast('ActiveGraphsChangedEvent');
    };
    return {
        getBuckets: function () {
            return showedBuckets;
        },
        hasBucket: function(bname){
            return _.contains(showedBuckets,bname);
        },
        addBucket: function (b) {

            if (b != null && b != "" && ! _.contains(showedBuckets, b)) {
                showedBuckets.push(b);
                notifyChange();
            }
        },
        removeBucketByIndex: function (i) {
            showedBuckets.splice(i, 1)
            notifyChange();
        },
        removeBucket: function (b) {
            showedBuckets = _.without(showedBuckets,b)
            notifyChange();
        }

    };
}]);
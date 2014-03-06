'use strict';

/* Services */


// Demonstrate how to register services
// In this case it is a simple value service.
angular.module('myApp.services', ['ngResource']).factory('Bucket', ['$resource',
    function ($resource) {
        return $resource('api/buckets', {}, {});
    }]);
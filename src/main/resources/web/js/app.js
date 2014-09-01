'use strict';


// Declare app level module which depends on filters, and services
angular.module('gj', [
    'ngRoute',
    'ngResource',
    'myApp.services',
    'myApp.directives',
    'myApp.controllers',
    'angular-rickshaw'
]).
    config(['$routeProvider', '$locationProvider', function ($routeProvider, $locationProvider) {
        $routeProvider.when('/dashboard', {templateUrl: 'partials/dashboard.html', controller: 'DashboardCtrl'});
        $routeProvider.when('/settings', {templateUrl: 'partials/settings.html', controller: 'SettingsCtrl'});
        $routeProvider.otherwise({redirectTo: '/dashboard'});
        $locationProvider.html5Mode(true);
    }]);

'use strict';

define(['jquery', 'angular', 'translations-en', 'ui-bootstrap-tpls', 'restangular', 'angular-translate', 'angular-ui-router', 'bootstrap'], function ($, angular, translations) {

    // Declare app level module which depends on filters, and services

    return angular.module('myApp', ['ui.bootstrap', 'restangular', 'pascalprecht.translate', 'ui.router'])
        .config(function (RestangularProvider, $translateProvider, $stateProvider, $urlRouterProvider) {
            // Configure RESTAngular
            RestangularProvider.setBaseUrl("/api");
            RestangularProvider.setResponseExtractor(function (response, operation, what) {
                if (response['_embedded'])
                    return response['_embedded'][what];
                else
                    return response;
            });

            // Configure Translations
            $translateProvider.translations('en', translations).preferredLanguage('en');

            // Configure UI-Router
            $urlRouterProvider.otherwise('/');
            $stateProvider
                .state('landing', {
                    url: '/',
                    templateUrl: '/app/partials/landing.html'
                })
                .state('hosts', {
                    url: '/hosts',
                    templateUrl: '/app/partials/hosts.html',
                    controller: 'HostsController'
                })
                .state('tiers', {
                    url: '/tiers',
                    templateUrl: '/app/partials/tiers.html'
                })
                .state('environments', {
                    url: '/environments',
                    templateUrl: '/app/partials/environments.html'
                })
                .state('applications', {
                    url: '/applications',
                    templateUrl: '/app/partials/applications.html'
                });
        })

        .controller('MainController', function($scope, $modal, Restangular) {

        })

        .controller('HostsController', function($scope, Restangular) {
            var hostsInterface = Restangular.all('hosts');
            $scope.hosts = hostsInterface.getList().$object;
        })
});
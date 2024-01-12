package uk.gov.justice.services.example.cakeshop.it.params;

import static uk.gov.justice.services.example.cakeshop.it.helpers.SystemPropertyFinder.findWildflyHttpPort;

public class CakeShopUris {
    private static final String HOST = "http://localhost:" + findWildflyHttpPort();

    public static final String RECIPES_RESOURCE_URI = HOST + "/cakeshop-command-api/command/api/rest/cakeshop/recipes/";
    public static final String ORDERS_RESOURCE_URI = HOST + "/cakeshop-command-api/command/api/rest/cakeshop/orders/";
    public static final String RECIPES_RESOURCE_QUERY_URI = HOST + "/cakeshop-query-api/query/api/rest/cakeshop/recipes/";
    public static final String ORDERS_RESOURCE_QUERY_URI = HOST + "/cakeshop-query-api/query/api/rest/cakeshop/orders/";
    public static final String CAKES_RESOURCE_QUERY_URI = HOST + "/cakeshop-query-api/query/api/rest/cakeshop/cakes/";
    public static final String OVEN_RESOURCE_CUSTOM_URI = HOST + "/cakeshop-custom-api/custom/api/rest/cakeshop/ovens/";
    public static final String INDEXES_RESOURCE_QUERY_URI = HOST + "/cakeshop-query-api/query/api/rest/cakeshop/index/";
    public static final String HEALTHCHECK_URI = HOST + "/cakeshop-service/internal/healthchecks/all";


    public static final String CAKES_RESOURCE_URI_FORMAT = RECIPES_RESOURCE_URI + "%s/cakes/%s";
}

/*******************************************************************************
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.air.queryservice.service;

import static com.ericsson.oss.air.queryservice.service.QueryService.CUSTOM_METRIC_PREFIX;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.ericsson.oss.air.queryservice.visitor.JpaQueryVisitor;

import io.micrometer.core.annotation.Timed;

/**
 * Component responsible for creating SQL expressions based on OData query options.
 */
@Component
public class QueryBuilderService {

    /**
     * Builds a SELECT query targeting a database table of a schema, based on OData query options.
     * @param schema database schema id
     * @param table database table name
     * @param uriInfo uri information extracted from the OData request uri, contains query options
     * @return the SQL SELECT statement
     * @throws ODataApplicationException in case of an invalid parameter or filter
     */
    @Timed(CUSTOM_METRIC_PREFIX + "build_listing_query_timer")
    public String buildQuery(final String schema, final String table, final UriInfo uriInfo) throws ODataApplicationException {
        nullCheck(uriInfo);
        return "SELECT " + (uriInfo.getSelectOption() == null ? "*" : decorateSelectColumnNames(uriInfo.getSelectOption().getText()))
                + " "
                + getFromClause(schema, table)
                + getWhereClause(uriInfo)
                + getSortingClause(uriInfo)
                + getLimitClause(uriInfo)
                + getOffsetClause(uriInfo);
    }

    /**
     * Decorates all targeted column names with double quotes in order to ensure case-sensitive metadata matching.
     * @param selectOption list of columns of the SELECT query
     * @return the decorated list
     */
    private String decorateSelectColumnNames(final String selectOption) {
        final String separator = ",";

        return Arrays.stream(selectOption.split(separator))
                .map(String::trim)
                .map(s -> "*".equals(s) ? "*" : ("\"" + s + "\""))
                .collect(Collectors.joining(separator));
    }

    /**
     * Builds a SELECT COUNT query targeting a database table of a schema, based on OData query options.
     * @param schema database schema id
     * @param table database table name
     * @param uriInfo uri information extracted from the OData request uri, contains query options
     * @return the SQL SELECT statement
     * @throws ODataApplicationException in case of an invalid parameter or filter
     */
    @Timed(CUSTOM_METRIC_PREFIX + "build_count_query_timer")
    public String buildCountQuery(final String schema, final String table, final UriInfo uriInfo) throws ODataApplicationException {
        nullCheck(uriInfo);
        return "SELECT COUNT(*) "
                + getFromClause(schema, table)
                + getWhereClause(uriInfo);
    }

    /**
     * Performs a null check.
     * @param uriInfo uri information extracted from the OData request uri, contains query options
     * @throws ODataApplicationException in case of a null value in the argument
     */
    private void nullCheck(final UriInfo uriInfo) throws ODataApplicationException {
        if (uriInfo == null) {
            throw new ODataApplicationException("Invalid parameter",
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
        }
    }

    /**
     * Composes a SQL FROM clause.
     * @param schema database schema id
     * @param table database table name
     * @return the SQL FROM clause
     */
    private String getFromClause(final String schema, final String table) {
        return "FROM " + schema + "." + table;
    }

    /**
     * Composes a SQL LIMIT clause.
     * @param uriInfo uri information extracted from the OData request uri, contains query options
     * @return the SQL LIMIT clause
     */
    private String getLimitClause(final UriInfo uriInfo) {
        if (uriInfo.getTopOption() != null) {
            return " LIMIT " + uriInfo.getTopOption().getValue();
        }
        return "";
    }

    /**
     * Composes a SQL OFFSET clause.
     * @param uriInfo uri information extracted from the OData request uri, contains query options
     * @return the SQL OFFSET clause
     */
    private String getOffsetClause(final UriInfo uriInfo) {
        if (uriInfo.getSkipOption() != null) {
            return " OFFSET " + uriInfo.getSkipOption().getValue();
        }
        return "";
    }

    /**
     * Composes a SQL ORDER BY clause.
     * @param uriInfo uri information extracted from the OData request uri, contains query options
     * @return the SQL ORDER BY clause
     */
    private String getSortingClause(final UriInfo uriInfo) throws ODataApplicationException {
        final OrderByOption orderByOption = uriInfo.getOrderByOption();

        if (orderByOption != null) {
            final String separator = ",";
            final String descendingOrder = " desc";
            final StringBuilder stringBuilder = new StringBuilder();

            try {
                for (int i = 0; i < orderByOption.getOrders().size(); i++) {
                    final Expression expression = orderByOption.getOrders().get(i).getExpression();
                    stringBuilder.append(expression.accept(new JpaQueryVisitor()));
                    if (orderByOption.getOrders().get(i).isDescending()) {
                        stringBuilder.append(descendingOrder);
                    }
                    if (i < orderByOption.getOrders().size() - 1) {
                        stringBuilder.append(separator);
                    }
                }
            } catch (ExpressionVisitException | ODataApplicationException e) {
                throw new ODataApplicationException("Error while processing sorting: " + e.getMessage(),
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH, e);
            }

            return " ORDER BY " + stringBuilder;
        }
        return "";
    }

    /**
     * Composes a SQL WHERE clause.
     * @param uriInfo uri information extracted from the OData request uri, contains query options
     * @return the SQL WHERE clause
     */
    private String getWhereClause(final UriInfo uriInfo) throws ODataApplicationException {
        final FilterOption filterOption = uriInfo.getFilterOption();
        String where = "";
        if (filterOption != null) {
            try {
                where = filterOption.getExpression().accept(new JpaQueryVisitor());
            } catch (ExpressionVisitException | ODataApplicationException e) {
                throw new ODataApplicationException("Error while processing filter: " + e.getMessage(),
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH, e);
            }
        }
        if (StringUtils.hasLength(where)) {
            where = " WHERE " + where;
        }
        return where;
    }
}

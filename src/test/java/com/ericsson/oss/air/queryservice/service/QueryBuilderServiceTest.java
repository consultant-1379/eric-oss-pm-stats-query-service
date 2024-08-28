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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.commons.core.edm.EdmPropertyImpl;
import org.apache.olingo.commons.core.edm.primitivetype.EdmDate;
import org.apache.olingo.commons.core.edm.primitivetype.EdmDateTimeOffset;
import org.apache.olingo.commons.core.edm.primitivetype.EdmDecimal;
import org.apache.olingo.commons.core.edm.primitivetype.EdmInt32;
import org.apache.olingo.commons.core.edm.primitivetype.EdmInt64;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.queryoption.SystemQueryOption;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.MethodKind;
import org.apache.olingo.server.core.uri.UriInfoImpl;
import org.apache.olingo.server.core.uri.UriResourcePrimitivePropertyImpl;
import org.apache.olingo.server.core.uri.queryoption.FilterOptionImpl;
import org.apache.olingo.server.core.uri.queryoption.OrderByItemImpl;
import org.apache.olingo.server.core.uri.queryoption.OrderByOptionImpl;
import org.apache.olingo.server.core.uri.queryoption.SelectOptionImpl;
import org.apache.olingo.server.core.uri.queryoption.expression.BinaryImpl;
import org.apache.olingo.server.core.uri.queryoption.expression.LambdaRefImpl;
import org.apache.olingo.server.core.uri.queryoption.expression.LiteralImpl;
import org.apache.olingo.server.core.uri.queryoption.expression.MemberImpl;
import org.apache.olingo.server.core.uri.queryoption.expression.MethodImpl;
import org.apache.olingo.server.core.uri.queryoption.expression.TypeLiteralImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Query builder service")
public class QueryBuilderServiceTest {

    private static final String SCHEMA = "test_schema";
    private static final String TABLE = "table";
    private static final String DATE_TIME = "date_time";
    private QueryBuilderService queryBuilderService;

    @BeforeEach
    void init() {
        queryBuilderService = new QueryBuilderService();
    }

    @Test
    void testWithEmptyUriInfo() throws ODataApplicationException {
        final UriInfo uriInfo = new UriInfoImpl();

        final String query = queryBuilderService.buildQuery(SCHEMA, TABLE, uriInfo);
        assertEquals("SELECT * FROM test_schema.table", query);
    }

    @Test
    void testDateTime_date() throws ODataApplicationException {
        final UriInfoImpl uriInfo = new UriInfoImpl();

        final List<Expression> dateMethodParameters = new ArrayList<>();
        dateMethodParameters.add(new MemberImpl(new UriInfoImpl().addResourcePart(new UriResourcePrimitivePropertyImpl(
                new EdmPropertyImpl(null, new CsdlProperty().setName(DATE_TIME)))), null));
        final Expression dateMethod = new MethodImpl(MethodKind.DATE, dateMethodParameters);

        final Expression literal = new LiteralImpl("2022-03-01", new EdmDate());
        uriInfo.setSystemQueryOption(new FilterOptionImpl().setExpression(
                new BinaryImpl(dateMethod, BinaryOperatorKind.EQ, literal, null)));

        final String query = queryBuilderService.buildQuery(SCHEMA, TABLE, uriInfo);
        assertEquals("SELECT * FROM test_schema.table WHERE DATE(\"date_time\")='2022-03-01'", query);
    }

    @Test
    void testDateTime_day() throws ODataApplicationException {
        final UriInfoImpl uriInfo = new UriInfoImpl();

        final List<Expression> dateMethodParameters = new ArrayList<>();
        dateMethodParameters.add(new MemberImpl(new UriInfoImpl().addResourcePart(
                new UriResourcePrimitivePropertyImpl(new EdmPropertyImpl(null, new CsdlProperty().setName(DATE_TIME)))), null));
        final Expression dateMethod = new MethodImpl(MethodKind.DAY, dateMethodParameters);

        final Expression literal = new LiteralImpl("1", new EdmInt32());
        uriInfo.setSystemQueryOption(new FilterOptionImpl().setExpression(
                new BinaryImpl(dateMethod, BinaryOperatorKind.EQ, literal, null)));

        final String query = queryBuilderService.buildQuery(SCHEMA, TABLE, uriInfo);
        assertEquals("SELECT * FROM test_schema.table WHERE EXTRACT(DAY FROM \"date_time\")=1", query);
    }

    @Test
    void testDateTime_fractionalSeconds() throws ODataApplicationException {
        final UriInfoImpl uriInfo = new UriInfoImpl();

        final List<Expression> dateMethodParameters = new ArrayList<>();
        dateMethodParameters.add(new MemberImpl(new UriInfoImpl().addResourcePart(
                new UriResourcePrimitivePropertyImpl(
                        new EdmPropertyImpl(null, new CsdlProperty().setName(DATE_TIME)))), null));
        final Expression dateMethod = new MethodImpl(MethodKind.FRACTIONALSECONDS, dateMethodParameters);

        final Expression literal = new LiteralImpl("0.206", new EdmDecimal());
        uriInfo.setSystemQueryOption(new FilterOptionImpl().setExpression(
                new BinaryImpl(dateMethod, BinaryOperatorKind.EQ, literal, null)));

        final String query = queryBuilderService.buildQuery(SCHEMA, TABLE, uriInfo);
        assertEquals("SELECT * FROM test_schema.table WHERE (FLOOR(EXTRACT(MILLISECONDS FROM \"date_time\"))"
                + " - 1000 * FLOOR(EXTRACT(SECONDS FROM \"date_time\")))/1000=0.206", query);
    }

    @Test
    void testDateTime_hour() throws ODataApplicationException {
        final UriInfoImpl uriInfo = new UriInfoImpl();

        final List<Expression> dateMethodParameters = new ArrayList<>();
        dateMethodParameters.add(new MemberImpl(new UriInfoImpl().addResourcePart(
                new UriResourcePrimitivePropertyImpl(
                        new EdmPropertyImpl(null, new CsdlProperty().setName(DATE_TIME)))), null));
        final Expression dateMethod = new MethodImpl(MethodKind.HOUR, dateMethodParameters);

        final Expression literal = new LiteralImpl("3", new EdmDecimal());
        uriInfo.setSystemQueryOption(new FilterOptionImpl().setExpression(
                new BinaryImpl(dateMethod, BinaryOperatorKind.EQ, literal, null)));

        final String query = queryBuilderService.buildQuery(SCHEMA, TABLE, uriInfo);
        assertEquals("SELECT * FROM test_schema.table WHERE EXTRACT(HOUR FROM \"date_time\")=3", query);
    }

    @Test
    void testDateTime_maxDateTime() throws ODataApplicationException {
        final UriInfoImpl uriInfo = new UriInfoImpl();

        final Expression dateMethod = new MethodImpl(MethodKind.MAXDATETIME, new ArrayList<>());
        final Expression member = new MemberImpl(new UriInfoImpl().addResourcePart(
                new UriResourcePrimitivePropertyImpl(
                        new EdmPropertyImpl(null, new CsdlProperty().setName(DATE_TIME)))), null);
        uriInfo.setSystemQueryOption(new FilterOptionImpl().setExpression(
                new BinaryImpl(member, BinaryOperatorKind.LT, dateMethod, null)));

        final String query = queryBuilderService.buildQuery(SCHEMA, TABLE, uriInfo);
        assertEquals("SELECT * FROM test_schema.table WHERE \"date_time\"<'INFINITY'", query);
    }

    @Test
    void testDateTime_minDateTime() throws ODataApplicationException {
        final UriInfoImpl uriInfo = new UriInfoImpl();

        final Expression dateMethod = new MethodImpl(MethodKind.MINDATETIME, new ArrayList<>());
        final Expression member = new MemberImpl(new UriInfoImpl().addResourcePart(
                new UriResourcePrimitivePropertyImpl(
                        new EdmPropertyImpl(null, new CsdlProperty().setName(DATE_TIME)))), null);
        uriInfo.setSystemQueryOption(new FilterOptionImpl().setExpression(
                new BinaryImpl(member, BinaryOperatorKind.LE, dateMethod, null)));

        final String query = queryBuilderService.buildQuery(SCHEMA, TABLE, uriInfo);
        assertEquals("SELECT * FROM test_schema.table WHERE \"date_time\"<='-INFINITY'", query);
    }

    @Test
    void testDateTime_minute() throws ODataApplicationException {
        final UriInfoImpl uriInfo = new UriInfoImpl();

        final List<Expression> dateMethodParameters = new ArrayList<>();
        dateMethodParameters.add(new MemberImpl(new UriInfoImpl().addResourcePart(
                new UriResourcePrimitivePropertyImpl(
                        new EdmPropertyImpl(null, new CsdlProperty().setName(DATE_TIME)))), null));
        final Expression dateMethod = new MethodImpl(MethodKind.MINUTE, dateMethodParameters);

        final Expression literal = new LiteralImpl("32", new EdmDecimal());
        uriInfo.setSystemQueryOption(new FilterOptionImpl().setExpression(
                new BinaryImpl(dateMethod, BinaryOperatorKind.EQ, literal, null)));

        final String query = queryBuilderService.buildQuery(SCHEMA, TABLE, uriInfo);
        assertEquals("SELECT * FROM test_schema.table WHERE EXTRACT(MINUTE FROM \"date_time\")=32", query);
    }

    @Test
    void testDateTime_month() throws ODataApplicationException {
        final UriInfoImpl uriInfo = new UriInfoImpl();

        final List<Expression> dateMethodParameters = new ArrayList<>();
        dateMethodParameters.add(new MemberImpl(new UriInfoImpl().addResourcePart(
                new UriResourcePrimitivePropertyImpl(
                        new EdmPropertyImpl(null, new CsdlProperty().setName(DATE_TIME)))), null));
        final Expression dateMethod = new MethodImpl(MethodKind.MONTH, dateMethodParameters);

        final Expression literal = new LiteralImpl("3", new EdmDecimal());
        uriInfo.setSystemQueryOption(new FilterOptionImpl().setExpression(
                new BinaryImpl(dateMethod, BinaryOperatorKind.EQ, literal, null)));

        final String query = queryBuilderService.buildQuery(SCHEMA, TABLE, uriInfo);
        assertEquals("SELECT * FROM test_schema.table WHERE EXTRACT(MONTH FROM \"date_time\")=3", query);
    }

    @Test
    void testDateTime_now() throws ODataApplicationException {
        final UriInfoImpl uriInfo = new UriInfoImpl();

        final Expression dateMethod = new MethodImpl(MethodKind.NOW, new ArrayList<>());
        final Expression member = new MemberImpl(new UriInfoImpl().addResourcePart(
                new UriResourcePrimitivePropertyImpl(
                        new EdmPropertyImpl(null, new CsdlProperty().setName(DATE_TIME)))), null);
        uriInfo.setSystemQueryOption(new FilterOptionImpl().setExpression(
                new BinaryImpl(member, BinaryOperatorKind.LE, dateMethod, null)));

        final String query = queryBuilderService.buildQuery(SCHEMA, TABLE, uriInfo);
        assertEquals("SELECT * FROM test_schema.table WHERE \"date_time\"<=NOW()", query);
    }

    @Test
    void testDateTime_second() throws ODataApplicationException {
        final UriInfoImpl uriInfo = new UriInfoImpl();

        final List<Expression> dateMethodParameters = new ArrayList<>();
        dateMethodParameters.add(new MemberImpl(new UriInfoImpl().addResourcePart(
                new UriResourcePrimitivePropertyImpl(
                        new EdmPropertyImpl(null, new CsdlProperty().setName(DATE_TIME)))), null));
        final Expression dateMethod = new MethodImpl(MethodKind.SECOND, dateMethodParameters);

        final Expression literal = new LiteralImpl("3", new EdmDecimal());
        uriInfo.setSystemQueryOption(new FilterOptionImpl().setExpression(
                new BinaryImpl(dateMethod, BinaryOperatorKind.EQ, literal, null)));

        final String query = queryBuilderService.buildQuery(SCHEMA, TABLE, uriInfo);
        assertEquals("SELECT * FROM test_schema.table WHERE FLOOR(EXTRACT(SECOND FROM \"date_time\"))=3", query);
    }

    @Test
    void testDateTime_time() throws ODataApplicationException {
        final UriInfoImpl uriInfo = new UriInfoImpl();

        final List<Expression> dateMethodParameters = new ArrayList<>();
        dateMethodParameters.add(new MemberImpl(new UriInfoImpl().addResourcePart(
                new UriResourcePrimitivePropertyImpl(
                        new EdmPropertyImpl(null, new CsdlProperty().setName(DATE_TIME)))), null));
        final Expression dateMethod = new MethodImpl(MethodKind.TIME, dateMethodParameters);

        final Expression literal = new LiteralImpl("11:07:38", new EdmDecimal());
        uriInfo.setSystemQueryOption(new FilterOptionImpl().setExpression(
                new BinaryImpl(dateMethod, BinaryOperatorKind.EQ, literal, null)));

        final String query = queryBuilderService.buildQuery(SCHEMA, TABLE, uriInfo);
        assertEquals("SELECT * FROM test_schema.table WHERE CAST(\"date_time\" AS TIME)=11:07:38", query);
    }

    @Test
    void testDateTime_totalOffsetMinutes() throws ODataApplicationException {
        final UriInfoImpl uriInfo = new UriInfoImpl();

        final List<Expression> dateMethodParameters = new ArrayList<>();
        dateMethodParameters.add(new MemberImpl(new UriInfoImpl().addResourcePart(
                new UriResourcePrimitivePropertyImpl(
                        new EdmPropertyImpl(null, new CsdlProperty().setName(DATE_TIME)))), null));
        final Expression dateMethod = new MethodImpl(MethodKind.TOTALOFFSETMINUTES, dateMethodParameters);

        final Expression literal = new LiteralImpl("22", new EdmDecimal());
        uriInfo.setSystemQueryOption(new FilterOptionImpl().setExpression(
                new BinaryImpl(dateMethod, BinaryOperatorKind.EQ, literal, null)));

        final String query = queryBuilderService.buildQuery(SCHEMA, TABLE, uriInfo);
        assertEquals("SELECT * FROM test_schema.table WHERE EXTRACT(TIMEZONE_MINUTE "
                + "FROM CAST(\"date_time\" AS TIMESTAMP WITH TIME ZONE))=22", query);
    }

    @Test
    void testDateTime_year() throws ODataApplicationException {
        final UriInfoImpl uriInfo = new UriInfoImpl();

        final List<Expression> dateMethodParameters = new ArrayList<>();
        dateMethodParameters.add(new MemberImpl(new UriInfoImpl().addResourcePart(
                new UriResourcePrimitivePropertyImpl(
                        new EdmPropertyImpl(null, new CsdlProperty().setName(DATE_TIME)))), null));
        final Expression dateMethod = new MethodImpl(MethodKind.YEAR, dateMethodParameters);

        final Expression literal = new LiteralImpl("2021", new EdmDecimal());
        uriInfo.setSystemQueryOption(new FilterOptionImpl().setExpression(
                new BinaryImpl(dateMethod, BinaryOperatorKind.EQ, literal, null)));

        final String query = queryBuilderService.buildQuery(SCHEMA, TABLE, uriInfo);
        assertEquals("SELECT * FROM test_schema.table WHERE EXTRACT(YEAR FROM \"date_time\")=2021", query);
    }

    @Test
    void testSelectOneColumn() throws ODataApplicationException {
        final UriInfoImpl uriInfo = new UriInfoImpl();

        uriInfo.setSystemQueryOption((SystemQueryOption) new SelectOptionImpl().setText(DATE_TIME));

        final String query = queryBuilderService.buildQuery(SCHEMA, TABLE, uriInfo);
        assertEquals("SELECT \"date_time\" FROM test_schema.table", query);
    }

    @Test
    void testSelectMultiColumn() throws ODataApplicationException {
        final String column = DATE_TIME + ",first_kpi";
        final UriInfoImpl uriInfo = new UriInfoImpl();

        uriInfo.setSystemQueryOption((SystemQueryOption) new SelectOptionImpl().setText(column));

        final String query = queryBuilderService.buildQuery(SCHEMA, TABLE, uriInfo);
        assertEquals("SELECT \"date_time\",\"first_kpi\" FROM test_schema.table", query);
    }

    @Test
    void testFilterLambdaReference() {
        final UriInfoImpl uriInfo = new UriInfoImpl();

        final Expression literal = new LiteralImpl("2022", new EdmDecimal());
        uriInfo.setSystemQueryOption(new FilterOptionImpl().setExpression(
                new BinaryImpl(new LambdaRefImpl("test"), BinaryOperatorKind.EQ, literal, null)));

        final ODataApplicationException exception = assertThrows(ODataApplicationException.class,
                () -> queryBuilderService.buildQuery(SCHEMA, TABLE, uriInfo));
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), exception.getStatusCode());
        assertEquals("Error while processing filter: Lamdba references are not implemented", exception.getMessage());
    }

    @Test
    void testNullUri() {
        final ODataApplicationException exception = assertThrows(ODataApplicationException.class,
                () -> queryBuilderService.buildQuery("", "", null));
        assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), exception.getStatusCode());
    }

    @Test
    void testFilterTypeLiteral() {
        final UriInfoImpl uriInfo = new UriInfoImpl();

        final Expression literal = new LiteralImpl("2023", new EdmDecimal());
        uriInfo.setSystemQueryOption(new FilterOptionImpl().setExpression(
                new BinaryImpl(new TypeLiteralImpl(EdmInt64.getInstance()), BinaryOperatorKind.EQ, literal, null)));

        final ODataApplicationException exception = assertThrows(ODataApplicationException.class,
                () -> queryBuilderService.buildQuery(SCHEMA, TABLE, uriInfo));
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), exception.getStatusCode());
        assertEquals("Error while processing filter: Type literals are not implemented", exception.getMessage());
    }

    @Test
    void testSortingUnsupportedOperator() {
        final UriInfoImpl uriInfo = new UriInfoImpl();

        final Expression literal = new LiteralImpl("2021", new EdmDecimal());
        uriInfo.setQueryOption(new OrderByOptionImpl().addOrder(new OrderByItemImpl().setExpression(
                new BinaryImpl(new TypeLiteralImpl(EdmInt64.getInstance()), BinaryOperatorKind.EQ, literal, null))));

        final ODataApplicationException exception = assertThrows(ODataApplicationException.class,
                () -> queryBuilderService.buildQuery(SCHEMA, TABLE, uriInfo));
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), exception.getStatusCode());
        Assertions.assertThat(exception.getMessage()).startsWith("Error while processing sorting:");
    }

    @Test
    void testSimpleOrOperator() throws ODataApplicationException {
        final UriInfoImpl uriInfo = new UriInfoImpl();

        final Expression idField = new MemberImpl(new UriInfoImpl().addResourcePart(
                new UriResourcePrimitivePropertyImpl(new EdmPropertyImpl(null, new CsdlProperty().setName("id")))), null);
        final Expression firstExpression = new BinaryImpl(idField, BinaryOperatorKind.EQ,
                new LiteralImpl("1", new EdmDecimal()), null);
        final Expression secondExpression = new BinaryImpl(idField, BinaryOperatorKind.EQ,
                new LiteralImpl("2", new EdmDecimal()), null);

        uriInfo.setSystemQueryOption(new FilterOptionImpl().setExpression(
                new BinaryImpl(firstExpression, BinaryOperatorKind.OR, secondExpression, null)));

        final String query = queryBuilderService.buildQuery(SCHEMA, TABLE, uriInfo);
        assertEquals("SELECT * FROM test_schema.table WHERE (\"id\"=1 OR \"id\"=2)", query);
    }

    @Test
    void testSimpleAndOperator() throws ODataApplicationException {
        final UriInfoImpl uriInfo = new UriInfoImpl();

        final Expression idField = new MemberImpl(new UriInfoImpl().addResourcePart(
                new UriResourcePrimitivePropertyImpl(new EdmPropertyImpl(null, new CsdlProperty().setName("id")))), null);
        final Expression firstExpression = new BinaryImpl(idField, BinaryOperatorKind.EQ,
                new LiteralImpl("1", new EdmDecimal()), null);
        final Expression secondExpression = new BinaryImpl(idField, BinaryOperatorKind.EQ,
                new LiteralImpl("2", new EdmDecimal()), null);

        uriInfo.setSystemQueryOption(new FilterOptionImpl().setExpression(
                new BinaryImpl(firstExpression, BinaryOperatorKind.AND, secondExpression, null)));

        final String query = queryBuilderService.buildQuery(SCHEMA, TABLE, uriInfo);
        assertEquals("SELECT * FROM test_schema.table WHERE (\"id\"=1 AND \"id\"=2)", query);
    }

    @Test
    void testComplexAndOperator() throws ODataApplicationException {
        final UriInfoImpl uriInfo = new UriInfoImpl();

        final Expression idField = new MemberImpl(new UriInfoImpl().addResourcePart(
                new UriResourcePrimitivePropertyImpl(
                        new EdmPropertyImpl(null, new CsdlProperty().setName("id")))), null);
        final Expression dateField = new MemberImpl(new UriInfoImpl().addResourcePart(
                new UriResourcePrimitivePropertyImpl(
                        new EdmPropertyImpl(null, new CsdlProperty().setName("time_created")))), null);

        final Expression firstIdFilter = new BinaryImpl(idField, BinaryOperatorKind.EQ,
                new LiteralImpl("1", new EdmDecimal()), null);
        final Expression secondIdFilter = new BinaryImpl(idField, BinaryOperatorKind.EQ,
                new LiteralImpl("2", new EdmDecimal()), null);
        final Expression orExpression = new BinaryImpl(firstIdFilter, BinaryOperatorKind.OR, secondIdFilter, null);

        final Expression dateFilter = new BinaryImpl(dateField, BinaryOperatorKind.EQ,
                new LiteralImpl("2022-10-21 10:00:00", new EdmDateTimeOffset()), null);

        uriInfo.setSystemQueryOption(new FilterOptionImpl().setExpression(
                new BinaryImpl(orExpression, BinaryOperatorKind.AND, dateFilter, null)));

        final String query = queryBuilderService.buildQuery(SCHEMA, TABLE, uriInfo);
        assertEquals("SELECT * FROM test_schema.table WHERE ((\"id\"=1 OR \"id\"=2)"
                + " AND \"time_created\"='2022-10-21 10:00:00')", query);
    }
}

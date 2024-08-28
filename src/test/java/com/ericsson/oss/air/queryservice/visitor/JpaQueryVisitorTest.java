/*******************************************************************************
 * COPYRIGHT Ericsson 2023
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

package com.ericsson.oss.air.queryservice.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.commons.core.edm.EdmPropertyImpl;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResourceProperty;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;
import org.apache.olingo.server.api.uri.queryoption.expression.MethodKind;
import org.apache.olingo.server.core.uri.UriInfoImpl;
import org.apache.olingo.server.core.uri.UriResourceComplexPropertyImpl;
import org.apache.olingo.server.core.uri.queryoption.expression.MemberImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Olingo JPA query visitor")
public class JpaQueryVisitorTest {

    private static final String INVALID_PARAMETER = "Invalid parameters!";

    @Test
    @DisplayName("Visit invalid binary operator")
    void testVisitInvalidBinaryOperator() {
        final ODataApplicationException exception = assertThrows(ODataApplicationException.class,
                () -> new JpaQueryVisitor().visitBinaryOperator(BinaryOperatorKind.IN, "left", "right"));

        assertEquals("Binary operation IN is not implemented", exception.getMessage());
        assertEquals(HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), exception.getStatusCode());
        assertEquals(HttpStatusCode.NOT_IMPLEMENTED.toString(), exception.getODataErrorCode());
    }

    @Test
    @DisplayName("Visit invalid unary operator")
    void testVisitInvalidUnaryOperator() {
        final ODataApplicationException exception = assertThrows(ODataApplicationException.class,
                () -> new JpaQueryVisitor().visitUnaryOperator(null, "operand"));

        assertEquals("Invalid type for unary operator", exception.getMessage());
        assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), exception.getStatusCode());
        assertEquals(HttpStatusCode.BAD_REQUEST.toString(), exception.getODataErrorCode());
    }

    @Test
    @DisplayName("Null params check")
    void testNullParamsCheck() {
        final ODataApplicationException visitMethodCall1 = assertThrows(ODataApplicationException.class,
                () -> new JpaQueryVisitor().visitMethodCall(MethodKind.TOTALSECONDS, null));
        final ODataApplicationException visitMethodCall2 = assertThrows(ODataApplicationException.class,
                () -> new JpaQueryVisitor().visitMethodCall(null, new ArrayList<>()));
        final ODataApplicationException visitLiteral = assertThrows(ODataApplicationException.class,
                () -> new JpaQueryVisitor().visitLiteral(null));
        final ODataApplicationException visitMember = assertThrows(ODataApplicationException.class,
                () -> new JpaQueryVisitor().visitMember(null));

        assertEquals(INVALID_PARAMETER, visitMethodCall1.getMessage());
        assertEquals(INVALID_PARAMETER, visitMethodCall2.getMessage());
        assertEquals(INVALID_PARAMETER, visitLiteral.getMessage());
        assertEquals(INVALID_PARAMETER, visitMember.getMessage());
    }

    @Test
    @DisplayName("Visit lambda expression")
    void testVisitLambdaExpression() {
        final ODataApplicationException exception = assertThrows(ODataApplicationException.class,
                () -> new JpaQueryVisitor().visitLambdaExpression("lambdaFunction", "lambdaVariable", null));

        assertEquals("Lambda expressions are not implemented", exception.getMessage());
        assertEquals(HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), exception.getStatusCode());
        assertEquals(HttpStatusCode.NOT_IMPLEMENTED.toString(), exception.getODataErrorCode());
    }

    @Test
    @DisplayName("Visit enum")
    void testVisitEnum() {
        final ODataApplicationException exception = assertThrows(ODataApplicationException.class,
                () -> new JpaQueryVisitor().visitEnum(null, null));

        assertEquals("Enums are not implemented", exception.getMessage());
        assertEquals(HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), exception.getStatusCode());
        assertEquals(HttpStatusCode.NOT_IMPLEMENTED.toString(), exception.getODataErrorCode());
    }

    @Test
    @DisplayName("Visit member - complex property not implemented")
    void testVisitMember_notPrimitiveProperty() {
        final UriResourceProperty complexProperty = new UriResourceComplexPropertyImpl(
                new EdmPropertyImpl(null, new CsdlProperty().setName("date_time")));
        final Member member = new MemberImpl(new UriInfoImpl().addResourcePart(complexProperty), null);
        final ODataApplicationException exception = assertThrows(ODataApplicationException.class,
                () -> new JpaQueryVisitor().visitMember(member));

        assertEquals("Only primitive properties are implemented in filter expressions", exception.getMessage());
        assertEquals(HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), exception.getStatusCode());
        assertEquals(HttpStatusCode.NOT_IMPLEMENTED.toString(), exception.getODataErrorCode());
    }

    @Test
    @DisplayName("Visit method call - invalid duration")
    void testVisitVisitMethodCall_invalidDuration() {
        final List<String> methodParameters = new ArrayList<>();
        methodParameters.add("invalidDuration");
        final ODataApplicationException exception = assertThrows(ODataApplicationException.class,
                () -> new JpaQueryVisitor().visitMethodCall(MethodKind.TOTALSECONDS, methodParameters));

        assertEquals("Failed to parse duration. org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException:"
                + " The literal 'invalidDuration' has illegal content.", exception.getMessage());
        assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), exception.getStatusCode());
        assertEquals(HttpStatusCode.BAD_REQUEST.toString(), exception.getODataErrorCode());
    }

}

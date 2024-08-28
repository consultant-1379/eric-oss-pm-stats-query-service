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

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.olingo.commons.api.edm.EdmEnumType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.commons.core.edm.primitivetype.EdmDate;
import org.apache.olingo.commons.core.edm.primitivetype.EdmDateTimeOffset;
import org.apache.olingo.commons.core.edm.primitivetype.EdmDuration;
import org.apache.olingo.commons.core.edm.primitivetype.EdmTimeOfDay;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitor;
import org.apache.olingo.server.api.uri.queryoption.expression.Literal;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;
import org.apache.olingo.server.api.uri.queryoption.expression.MethodKind;
import org.apache.olingo.server.api.uri.queryoption.expression.UnaryOperatorKind;

/**
 * Component responsible for implementing filter tree transformations into SQL clauses.
 */
public class JpaQueryVisitor implements ExpressionVisitor<String> {
    private static final String NUMERIC_CAST = "::numeric";

    /**
     * Replaces the OData filter operator with an equivalent SQL operator.
     * @param operator OData filter operator
     * @param left expression's left side
     * @param right expression's right side
     * @return partial of a SQL WHERE clause
     * @throws ODataApplicationException if the operator is not implemented
     */
    @Override
    public String visitBinaryOperator(final BinaryOperatorKind operator, final String left, final String right)
            throws ODataApplicationException {
        // Binary Operators are split up in three different kinds. Up to the kind of the
        // operator it can be applied to different types
        // - Arithmetic operations like add, minus, modulo, etc. are allowed on numeric
        // types like Edm.Int32
        // - Logical operations are allowed on numeric types and also Edm.String
        // - Boolean operations like and, or are allowed on Edm.Boolean
        // A detailed explanation can be found in OData Version 4.0 Part 2: URL
        // Conventions
        switch (operator) {
            case ADD:
                return left + "+" + right + NUMERIC_CAST;
            case SUB:
                return left + "-" + right + NUMERIC_CAST;
            case MUL:
                return left + "*" + right + NUMERIC_CAST;
            case DIV:
                return left + "/" + right + NUMERIC_CAST;
            case MOD:
                return left + "%" + right + NUMERIC_CAST;
            case EQ:
                return left + "=" + right;
            case NE:
                return left + "!=" + right;
            case GE:
                return left + ">=" + right;
            case GT:
                return left + ">" + right;
            case LE:
                return left + "<=" + right;
            case LT:
                return left + "<" + right;
            case AND:
                return "(" + left + " AND " + right + ")";
            case OR:
                return "(" + left + " OR " + right + ")";
            default:
                throw new ODataApplicationException("Binary operation " + operator.name() + " is not implemented",
                        HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH, HttpStatusCode.NOT_IMPLEMENTED.toString());
        }
    }

    /**
     * Replaces the OData filter operator with an equivalent SQL operator.
     * @param binaryOperatorKind OData filter operator
     * @param s left subtree
     * @param list right subtree
     * @throws ODataApplicationException when this method is invoked
     */
    @Override
    public String visitBinaryOperator(final BinaryOperatorKind binaryOperatorKind, final String s, final List<String> list)
            throws ODataApplicationException {
        throw new ODataApplicationException("Binary operators for string lists are not implemented",
                HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH, HttpStatusCode.NOT_IMPLEMENTED.toString());
    }

    /**
     * Replaces the OData filter unary operator with an equivalent SQL operator.
     * @param operator OData filter operator
     * @param operand expression's operand
     * @return partial of a SQL WHERE clause
     * @throws ODataApplicationException if the operator is not implemented
     */
    @Override
    public String visitUnaryOperator(final UnaryOperatorKind operator, final String operand)
            throws ODataApplicationException {
        if (UnaryOperatorKind.NOT.equals(operator)) {
            return "NOT " + operand;
        } else if (UnaryOperatorKind.MINUS.equals(operator)) {
            return "(-" + operand + ")";
        }

        // Operation not processed, throw an exception
        throw new ODataApplicationException("Invalid type for unary operator",
                HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH, HttpStatusCode.BAD_REQUEST.toString());
    }

    /**
     * Replaces the OData filter method call with an equivalent SQL method call.
     * @param methodCall OData filter method call
     * @param parameters method parameters
     * @return SQL method call
     * @throws ODataApplicationException if the method call is not implemented
     */
    @Override
    public String visitMethodCall(final MethodKind methodCall, final List<String> parameters)
            throws ODataApplicationException {
        if (methodCall == null || parameters == null) {
            throw new ODataApplicationException("Invalid parameters!",
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
        }
        switch (methodCall) {
            case CONCAT:
                return "CONCAT(" + parameters.stream().collect(Collectors.joining(",")) + ")";
            case CONTAINS:
                return parameters.get(0) + " LIKE CONCAT('%'," + parameters.get(1) + ",'%')";
            case STARTSWITH:
                return parameters.get(0) + " LIKE CONCAT(" + parameters.get(1) + ",'%')";
            case ENDSWITH:
                return parameters.get(0) + " LIKE CONCAT('%'," + parameters.get(1) + ")";
            case INDEXOF:
                return "POSITION(" + parameters.get(1) + " IN " + parameters.get(0) + ")";
            case LENGTH:
                return "LENGTH(" + parameters.get(0) + ")";
            case SUBSTRING:
                if (parameters.size() == 2) {
                    return "SUBSTRING(" + parameters.get(0) + ", " + parameters.get(1) + ")";
                } else {
                    return "SUBSTRING(" + parameters.get(0) + ", " + parameters.get(1) + ", " + parameters.get(2) + ")";
                }
            case TOLOWER:
                return "LOWER(" + parameters.get(0) + ")";
            case TOUPPER:
                return "UPPER(" + parameters.get(0) + ")";
            case TRIM:
                return "BTRIM(" + parameters.get(0) + ")";
            case CEILING:
                return "CEILING(" + parameters.get(0) + ")";
            case FLOOR:
                return "FLOOR(" + parameters.get(0) + ")";
            case ROUND:
                return "ROUND(" + parameters.get(0) + ")";
            case DATE:
                return "DATE(" + parameters.get(0) + ")";
            case DAY:
                return "EXTRACT(DAY FROM " + parameters.get(0) + ")";
            case FRACTIONALSECONDS:
                return "(FLOOR(EXTRACT(MILLISECONDS FROM " + parameters.get(0)
                        + ")) - 1000 * FLOOR(EXTRACT(SECONDS FROM " + parameters.get(0) + ")))/1000";
            case HOUR:
                return "EXTRACT(HOUR FROM " + parameters.get(0) + ")";
            case MAXDATETIME:
                return "'INFINITY'";
            case MINDATETIME:
                return "'-INFINITY'";
            case MINUTE:
                return "EXTRACT(MINUTE FROM " + parameters.get(0) + ")";
            case MONTH:
                return "EXTRACT(MONTH FROM " + parameters.get(0) + ")";
            case NOW:
                return "NOW()";
            case SECOND:
                return "FLOOR(EXTRACT(SECOND FROM " + parameters.get(0) + "))";
            case TIME:
                return "CAST(" + parameters.get(0) + " AS TIME)";
            case TOTALOFFSETMINUTES:
                return "EXTRACT(TIMEZONE_MINUTE FROM CAST(" + parameters.get(0) + " AS TIMESTAMP WITH TIME ZONE))";
            case TOTALSECONDS:
                return getDurationSeconds(parameters.get(0)).toString();
            case YEAR:
                return "EXTRACT(YEAR FROM " + parameters.get(0) + ")";
            default:
                throw new ODataApplicationException("Method call " + methodCall + " not implemented",
                        HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH, HttpStatusCode.NOT_IMPLEMENTED.toString());
        }
    }

    /**
     * Replaces the OData traversed lambda expression with an equivalent SQL expression.
     * @param lambdaFunction lambda function "ALL" or "ANY"
     * @param lambdaVariable lambda variable
     * @param expression lambda expression
     * @throws ODataApplicationException when this method is invoked
     */
    @Override
    public String visitLambdaExpression(final String lambdaFunction, final String lambdaVariable,
                                        final Expression expression) throws ODataApplicationException {
        throw new ODataApplicationException("Lambda expressions are not implemented",
                HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH, HttpStatusCode.NOT_IMPLEMENTED.toString());
    }

    /**
     * Replaces the OData traversed literal expression with a String.
     * @param literal literal
     * @throws ODataApplicationException in case of an invalid parameter
     */
    @Override
    public String visitLiteral(final Literal literal) throws ODataApplicationException {
        if (literal == null) {
            throw new ODataApplicationException("Invalid parameters!",
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
        }
        final String literalAsString = literal.getText();
        if (literal.getType() instanceof EdmDate || literal.getType() instanceof EdmTimeOfDay || literal.getType() instanceof EdmDateTimeOffset) {
            return "'" + literalAsString + "'";
        } else {
            return literalAsString;
        }
    }

    /**
     * Replaces the OData traversed member expression with a property name.
     * @param member member
     * @throws ODataApplicationException in case of an invalid parameter
     */
    @Override
    public String visitMember(final Member member) throws ODataApplicationException {
        if (member == null) {
            throw new ODataApplicationException("Invalid parameters!",
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
        }
        final UriInfoResource resourcePath = member.getResourcePath();
        final List<UriResource> uriResourceParts = resourcePath.getUriResourceParts();

        if (uriResourceParts.size() == 1 && uriResourceParts.get(0) instanceof UriResourcePrimitiveProperty) {
            final UriResourcePrimitiveProperty uriResourceProperty = (UriResourcePrimitiveProperty) uriResourceParts.get(0);
            return "\"" + uriResourceProperty.getProperty().getName() + "\"";
        } else {
            throw new ODataApplicationException("Only primitive properties are implemented in filter expressions",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH, HttpStatusCode.NOT_IMPLEMENTED.toString());
        }
    }

    /**
     * Replaces the OData alias with an equivalent SQL expression.
     * @param aliasName name of the alias
     * @throws ODataApplicationException when this method is invoked
     */
    @Override
    public String visitAlias(final String aliasName) throws ODataApplicationException {
        throw new ODataApplicationException("Aliases are not implemented",
                HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH, HttpStatusCode.NOT_IMPLEMENTED.toString());
    }

    /**
     * Replaces the OData type literal with an equivalent SQL expression.
     * @param type EDM type
     * @throws ODataApplicationException when this method is invoked
     */
    @Override
    public String visitTypeLiteral(final EdmType type) throws ODataApplicationException {
        throw new ODataApplicationException("Type literals are not implemented",
                HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH, HttpStatusCode.NOT_IMPLEMENTED.toString());
    }

    /**
     * Replaces the OData lambda reference with an equivalent SQL expression.
     * @param variableName name of the variable
     * @throws ODataApplicationException when this method is invoked
     */
    @Override
    public String visitLambdaReference(final String variableName) throws ODataApplicationException {
        throw new ODataApplicationException("Lamdba references are not implemented",
                HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH, HttpStatusCode.NOT_IMPLEMENTED.toString());
    }

    /**
     * Replaces the OData enumeration expression with an equivalent SQL expression.
     * @param type EDM enum type
     * @param enumValues list of EDM enum values
     * @throws ODataApplicationException when this method is invoked
     */
    @Override
    public String visitEnum(final EdmEnumType type, final List<String> enumValues) throws ODataApplicationException {
        throw new ODataApplicationException("Enums are not implemented", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
                Locale.ENGLISH, HttpStatusCode.NOT_IMPLEMENTED.toString());
    }

    /**
     * Converts EDM literal duration into numeric value.
     * @param duration literal duration
     * @return converted value
     * @throws ODataApplicationException if a parse error occurs.
     */
    private Double getDurationSeconds(final String duration) throws ODataApplicationException {
        final EdmDuration edmDuration = new EdmDuration();
        try {
            return edmDuration.valueOfString(edmDuration.fromUriLiteral(duration),
                    true, null, 15, null, true, Double.class);
        } catch (final EdmPrimitiveTypeException e) {
            throw new ODataApplicationException("Failed to parse duration. " + e,
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH, e, HttpStatusCode.BAD_REQUEST.toString());
        }
    }
}

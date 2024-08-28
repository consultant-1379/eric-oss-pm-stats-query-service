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

import java.math.BigDecimal;
import java.sql.JDBCType;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;

import jakarta.validation.constraints.NotNull;

/**
 * Utility class for EDM-related conversions.
 */
public final class EdmTypeConverter {

    private EdmTypeConverter() {
    }

    /**
     * Converts JDBC types to EDM primitive types.
     *
     * @param type constant used to identify generic SQL types
     * @return EDM type
     * @throws ODataApplicationException in case of an unsupported data type
     */
    public static EdmPrimitiveTypeKind convertJdbcTypeToEdmType(final JDBCType type) throws ODataApplicationException {
        switch (type == null ? JDBCType.OTHER : type) {
            case BIGINT:
                return EdmPrimitiveTypeKind.Int64;
            case DECIMAL:
            case NUMERIC:
            case DOUBLE:
            case FLOAT:
                return EdmPrimitiveTypeKind.Double;
            case BINARY:
            case BLOB:
            case CLOB:
            case LONGVARBINARY:
            case VARBINARY:
                return EdmPrimitiveTypeKind.Binary;
            case BIT:
            case BOOLEAN:
                return EdmPrimitiveTypeKind.Boolean;
            case CHAR:
            case LONGVARCHAR:
            case VARCHAR:
            case OTHER:
            case SQLXML:
                return EdmPrimitiveTypeKind.String;
            case DATE:
                return EdmPrimitiveTypeKind.Date;
            case INTEGER:
                return EdmPrimitiveTypeKind.Int32;
            case SMALLINT:
                return EdmPrimitiveTypeKind.Int16;
            case TIME:
            case TIME_WITH_TIMEZONE:
                return EdmPrimitiveTypeKind.TimeOfDay;
            case TIMESTAMP:
            case TIMESTAMP_WITH_TIMEZONE:
                return EdmPrimitiveTypeKind.DateTimeOffset;
            case TINYINT:
                return EdmPrimitiveTypeKind.Byte;
            case REAL:
                return EdmPrimitiveTypeKind.Single;
            default:
                throw new ODataApplicationException("Unsupported data type " + type,
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
    }

    /**
     * Converts JDBC array types to EDM primitive types.
     *
     * @param arrayType PostgreSql underlying data type of the array
     * @return EDM type
     * @throws ODataApplicationException in case of an unsupported data type
     */
    public static EdmPrimitiveTypeKind convertArrayTypeToEdmType(final String arrayType) throws ODataApplicationException {
        switch (arrayType == null ? "NULL" : arrayType) {
            case "_bit":
            case "_bool":
                return EdmPrimitiveTypeKind.Boolean;
            case "_box":
            case "_bpchar":
            case "_cidr":
            case "_circle":
            case "_inet":
            case "_interval":
            case "_json":
            case "_jsonb":
            case "_line":
            case "_lseg":
            case "_macaddr":
            case "_macaddr8":
            case "_path":
            case "_pg_lsn":
            case "_point":
            case "_polygon":
            case "_text":
            case "_tsquery":
            case "_tsvector":
            case "_uuid":
            case "_varbit":
            case "_varchar":
            case "_xml":
                return EdmPrimitiveTypeKind.String;
            case "_date":
                return EdmPrimitiveTypeKind.Date;
            case "_float4":
                return EdmPrimitiveTypeKind.Single;
            case "_float8":
            case "_numeric":
                return EdmPrimitiveTypeKind.Double;
            case "_int2":
                return EdmPrimitiveTypeKind.Int16;
            case "_int4":
                return EdmPrimitiveTypeKind.Int32;
            case "_int8":
                return EdmPrimitiveTypeKind.Int64;
            case "_time":
            case "_timetz":
                return EdmPrimitiveTypeKind.TimeOfDay;
            case "_timestamp":
            case "_timestamptz":
                return EdmPrimitiveTypeKind.DateTimeOffset;
            case "_bytea":
            case "_money":
            case "_pg_snapshot":
            case "_txid_snapshot":
            default:
                throw new ODataApplicationException("Unsupported (array) data type " + arrayType,
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
    }

    /**
     * Converts PostgreSql array wrapper objects to typed lists.
     *
     * @param pgArray wrapper object for a JDBC mapping of a PostgreSql array
     * @param arrayType PostgreSql underlying data type of the array
     * @return EDM type
     * @throws ODataApplicationException in case of a conversion error or an unsupported data type
     */
    public static List<Object> convertPgArrayToTypedList(final @NotNull Object pgArray, final String arrayType) throws ODataApplicationException {
        if (!pgArray.getClass().isArray()) {
            throw new ODataApplicationException("PostgreSql array value conversion error: result set element is not an array.",
                    HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
        switch (arrayType == null ? "NULL" : arrayType) {
            case "_bit":
            case "_bool":
                return Arrays.asList((Boolean[]) pgArray);
            case "_box":
            case "_cidr":
            case "_circle":
            case "_inet":
            case "_interval":
            case "_line":
            case "_lseg":
            case "_macaddr":
            case "_macaddr8":
            case "_path":
            case "_pg_lsn":
            case "_point":
            case "_polygon":
            case "_tsquery":
            case "_tsvector":
            case "_uuid":
            case "_xml":
            case "_varbit":
                return Arrays.stream((Object[]) pgArray)
                        .map(c -> Optional.ofNullable(c).map(Object::toString).orElse(null))
                        .collect(Collectors.toList());
            case "_bpchar":
            case "_json":
            case "_jsonb":
            case "_text":
            case "_varchar":
                return Arrays.asList((String[]) pgArray);
            case "_date":
                return Arrays.asList((java.sql.Date[]) pgArray);
            case "_float4":
                return Arrays.asList((Float[]) pgArray);
            case "_float8":
                return Arrays.asList((Double[]) pgArray);
            case "_numeric":
                return Arrays.asList((BigDecimal[]) pgArray);
            case "_int2":
                return Arrays.asList((Short[]) pgArray);
            case "_int4":
                return Arrays.asList((Integer[]) pgArray);
            case "_int8":
                return Arrays.asList((Long[]) pgArray);
            case "_time":
            case "_timetz":
                return Arrays.asList((java.sql.Time[]) pgArray);
            case "_timestamp":
            case "_timestamptz":
                return Arrays.asList((java.sql.Timestamp[]) pgArray);
            case "_bytea":
            case "_money":
            case "_pg_snapshot":
            case "_txid_snapshot":
            default:
                throw new ODataApplicationException("Unsupported (array) data type " + arrayType,
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
    }
}

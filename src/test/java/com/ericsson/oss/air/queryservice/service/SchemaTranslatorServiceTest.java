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

package com.ericsson.oss.air.queryservice.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.oss.air.queryservice.model.DatabaseColumn;

import lombok.SneakyThrows;

@SpringBootTest(classes = SchemaTranslatorService.class)
@DisplayName("Schema translator service")
public class SchemaTranslatorServiceTest {

    private static final String TESTSCHEMA = "TST";
    private static final String TESTTABLE = "TBL";
    private static final String STATUSCODE = "statusCode";

    @Autowired
    private SchemaTranslatorService schemaTranslatorService;
    @MockBean
    private DatabaseInfoService databaseInfoService;

    @SuppressWarnings("checkstyle:MultipleStringLiterals")
    @Test
    @SneakyThrows
    @DisplayName("JDBC to EDM type conversion for all types")
    void whenGetTableCalled_shouldReturnCorrectEntity() {
        final List<DatabaseColumn> testColumns = new ArrayList<>();
        final List<CsdlProperty> testProperties = new ArrayList<>();

        final List<JDBCType> knownTypes = List.of(
                JDBCType.BIGINT,
                JDBCType.DECIMAL, JDBCType.NUMERIC, JDBCType.DOUBLE, JDBCType.FLOAT,
                JDBCType.BINARY, JDBCType.BLOB, JDBCType.CLOB, JDBCType.LONGVARBINARY, JDBCType.VARBINARY,
                JDBCType.BIT, JDBCType.BOOLEAN,
                JDBCType.CHAR, JDBCType.LONGVARCHAR, JDBCType.VARCHAR, JDBCType.OTHER, JDBCType.SQLXML,
                JDBCType.DATE,
                JDBCType.INTEGER,
                JDBCType.SMALLINT,
                JDBCType.TIME, JDBCType.TIME_WITH_TIMEZONE,
                JDBCType.TIMESTAMP, JDBCType.TIMESTAMP_WITH_TIMEZONE,
                JDBCType.TINYINT,
                JDBCType.REAL);
        final List<String> knownFqns = List.of(
                "Edm.Int64",
                "Edm.Double", "Edm.Double", "Edm.Double", "Edm.Double",
                "Edm.Binary", "Edm.Binary", "Edm.Binary", "Edm.Binary", "Edm.Binary",
                "Edm.Boolean", "Edm.Boolean",
                "Edm.String", "Edm.String", "Edm.String", "Edm.String", "Edm.String",
                "Edm.Date",
                "Edm.Int32",
                "Edm.Int16",
                "Edm.TimeOfDay", "Edm.TimeOfDay",
                "Edm.DateTimeOffset", "Edm.DateTimeOffset",
                "Edm.Byte",
                "Edm.Single");

        for (int i = 0; i < knownTypes.size(); i++) {
            final String name = "c_" + knownTypes.get(i).getVendorTypeNumber();
            final DatabaseColumn column = new DatabaseColumn();
            column.setName(name);
            column.setType(knownTypes.get(i).getVendorTypeNumber());
            testColumns.add(column);
            testProperties.add(new CsdlProperty().setName(name).setType(knownFqns.get(i)));
        }

        doReturn(testColumns).when(databaseInfoService).getColumns(anyString(), anyString());

        final CsdlEntityType requiredResult = new CsdlEntityType().setName(TESTTABLE).setProperties(testProperties);

        final CsdlEntityType csdlEntityType = schemaTranslatorService.getTable(TESTSCHEMA, TESTTABLE);

        assertThat(csdlEntityType)
                .usingRecursiveComparison().withStrictTypeChecking().isEqualTo(requiredResult);
    }

    @SuppressWarnings("checkstyle:MultipleStringLiterals")
    @Test
    @SneakyThrows
    @DisplayName("JDBC to EDM type conversion for all array types")
    void whenGetTableCalledWithArrayColumns_shouldReturnCorrectEntity() {
        final List<DatabaseColumn> testColumns = new ArrayList<>();
        final List<CsdlProperty> testProperties = new ArrayList<>();

        final List<String> knownUdts = List.of(
                "_bit", "_bool",
                "_box", "_bpchar", "_cidr", "_circle", "_inet", "_interval", "_json", "_jsonb", "_line", "_lseg",
                "_macaddr", "_macaddr8", "_path", "_pg_lsn", "_point", "_polygon", "_text", "_tsquery", "_tsvector", "_uuid",
                "_varbit", "_varchar", "_xml",
                "_date",
                "_float4", "_float8",
                "_numeric",
                "_int2",
                "_int4",
                "_int8",
                "_time", "_timetz",
                "_timestamp", "_timestamptz"
        );
        final List<JDBCType> knownTypes = Collections.nCopies(knownUdts.size(), JDBCType.ARRAY);
        final List<String> knownFqns = List.of(
                "Edm.Boolean", "Edm.Boolean",
                "Edm.String", "Edm.String", "Edm.String", "Edm.String", "Edm.String",
                "Edm.String", "Edm.String", "Edm.String", "Edm.String", "Edm.String",
                "Edm.String", "Edm.String", "Edm.String", "Edm.String", "Edm.String",
                "Edm.String", "Edm.String", "Edm.String", "Edm.String", "Edm.String",
                "Edm.String", "Edm.String", "Edm.String",
                "Edm.Date",
                "Edm.Single",
                "Edm.Double", "Edm.Double",
                "Edm.Int16",
                "Edm.Int32",
                "Edm.Int64",
                "Edm.TimeOfDay", "Edm.TimeOfDay",
                "Edm.DateTimeOffset", "Edm.DateTimeOffset");

        for (int i = 0; i < knownTypes.size(); i++) {
            final String name = "c_" + knownTypes.get(i).getVendorTypeNumber();
            final DatabaseColumn column = new DatabaseColumn();
            column.setName(name);
            column.setType(knownTypes.get(i).getVendorTypeNumber());
            column.setUdtName(knownUdts.get(i));
            testColumns.add(column);
            testProperties.add(new CsdlProperty().setName(name).setType(knownFqns.get(i)).setNullable(true)
                    .setCollection(true));
        }

        doReturn(testColumns).when(databaseInfoService).getColumns(anyString(), anyString());

        final CsdlEntityType requiredResult = new CsdlEntityType().setName(TESTTABLE).setProperties(testProperties);

        final CsdlEntityType csdlEntityType = schemaTranslatorService.getTable(TESTSCHEMA, TESTTABLE);

        assertThat(csdlEntityType)
                .usingRecursiveComparison().withStrictTypeChecking().isEqualTo(requiredResult);
    }

    @Test
    @SneakyThrows
    @DisplayName("JDBC to EDM type conversion error handling given an unknown type")
    void whenGetTableCalledWithUnknownType_shouldThrowOdataApplicationException() {
        final List<DatabaseColumn> erroneousColumns = new ArrayList<>();
        final DatabaseColumn unknownTypedColumn = new DatabaseColumn();
        unknownTypedColumn.setName("N/A");
        unknownTypedColumn.setType(JDBCType.STRUCT.getVendorTypeNumber());
        erroneousColumns.add(unknownTypedColumn);
        doReturn(erroneousColumns).when(databaseInfoService).getColumns(anyString(), anyString());

        assertThatThrownBy(() -> schemaTranslatorService.getTable(TESTSCHEMA, TESTTABLE))
                .isInstanceOf(ODataApplicationException.class)
                .hasMessage("Unsupported data type STRUCT")
                .hasFieldOrPropertyWithValue(STATUSCODE, HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    @SneakyThrows
    @DisplayName("JDBC to EDM type conversion error handling given an unsupported array type")
    void whenGetTableCalledWithUnsupportedArrayType_shouldThrowOdataApplicationException() {
        final List<DatabaseColumn> erroneousColumns = new ArrayList<>();
        final DatabaseColumn unsupportedTypedColumn = new DatabaseColumn();
        unsupportedTypedColumn.setName("N/A");
        unsupportedTypedColumn.setType(JDBCType.ARRAY.getVendorTypeNumber());
        unsupportedTypedColumn.setUdtName("_pg_snapshot");
        erroneousColumns.add(unsupportedTypedColumn);
        doReturn(erroneousColumns).when(databaseInfoService).getColumns(anyString(), anyString());

        assertThatThrownBy(() -> schemaTranslatorService.getTable(TESTSCHEMA, TESTTABLE))
                .isInstanceOf(ODataApplicationException.class)
                .hasMessage("Unsupported (array) data type _pg_snapshot")
                .hasFieldOrPropertyWithValue(STATUSCODE, HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    @SneakyThrows
    @DisplayName("JDBC to EDM type conversion error handling given a null array type")
    void whenGetTableCalledWithNullArrayType_shouldThrowOdataApplicationException() {
        final List<DatabaseColumn> erroneousColumns = new ArrayList<>();
        final DatabaseColumn unsupportedTypedColumn = new DatabaseColumn();
        unsupportedTypedColumn.setName("N/A");
        unsupportedTypedColumn.setType(JDBCType.ARRAY.getVendorTypeNumber());
        unsupportedTypedColumn.setUdtName(null);
        erroneousColumns.add(unsupportedTypedColumn);
        doReturn(erroneousColumns).when(databaseInfoService).getColumns(anyString(), anyString());

        assertThatThrownBy(() -> schemaTranslatorService.getTable(TESTSCHEMA, TESTTABLE))
                .isInstanceOf(ODataApplicationException.class)
                .hasMessage("Unsupported (array) data type null")
                .hasFieldOrPropertyWithValue(STATUSCODE, HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    @DisplayName("PgArray conversion error when object is not an array")
    void whenArrayConverterCalledWithNotPgArrayType_shouldThrowOdataApplicationException() {
        final Object notArrayObject = new Object();
        assertThatThrownBy(() -> EdmTypeConverter.convertPgArrayToTypedList(notArrayObject, "_varchar"))
                .isInstanceOf(ODataApplicationException.class)
                .hasMessage("PostgreSql array value conversion error: result set element is not an array.")
                .hasFieldOrPropertyWithValue(STATUSCODE, HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    @DisplayName("PgArray conversion error when the underlying data type is not supported")
    void whenArrayConverterCalledWithUnsupportedArrayType_shouldThrowOdataApplicationException() {
        final Object arrayObject = new Object[1];
        assertThatThrownBy(() -> EdmTypeConverter.convertPgArrayToTypedList(arrayObject, "_pg_snapshot"))
                .isInstanceOf(ODataApplicationException.class)
                .hasMessage("Unsupported (array) data type _pg_snapshot")
                .hasFieldOrPropertyWithValue(STATUSCODE, HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
    }
}

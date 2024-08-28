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

package com.ericsson.oss.air.queryservice.olingo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import com.ericsson.oss.air.queryservice.model.DatabaseTable;
import com.ericsson.oss.air.queryservice.service.DatabaseInfoService;
import com.ericsson.oss.air.queryservice.service.SchemaTranslatorService;

/**
 * Entity data model provider class with fixed namespace and container. Implements OData Common Schema Definition
 * Language (CSDL) logic to define a representation of the model.
 */
@Component
@RequestScope
public class CustomEdmProvider extends CsdlAbstractEdmProvider {

    public static final String NAMESPACE = "public";
    public static final String ENTITY_CONTAINER_NAME = "Container";
    public static final FullQualifiedName ENTITY_CONTAINER = new FullQualifiedName(NAMESPACE, ENTITY_CONTAINER_NAME);

    private String schemaName;

    @Autowired
    private SchemaTranslatorService schemaTranslator;

    @Autowired
    private DatabaseInfoService databaseInfoService;

    /**
     * Sets the schema name.
     * @param schemaName schema name
     */
    public void setSchemaName(final String schemaName) {
        this.schemaName = schemaName;
    }

    /**
     * Obtains a {@link CustomEdmProvider CSDL} entity type from a namespace-qualified (FullQualified) type using the
     * schema translator.
     * @param entityTypeName namespace-qualified type name
     * @return {@link CustomEdmProvider CSDL} entity type
     * @throws ODataApplicationException if a null value is provided as type name
     */
    @Override
    public CsdlEntityType getEntityType(final FullQualifiedName entityTypeName) throws ODataApplicationException {
        if (entityTypeName == null) {
            throw new ODataApplicationException("Invalid parameter!", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
        }
        return schemaTranslator.getTable(schemaName, entityTypeName.getName());
    }

    /**
     * Creates a {@link CustomEdmProvider CSDL} entity set.
     * @param entityContainer namespace-qualified name of the entity container. This should match the fixed container
     *                        name defined for this class.
     * @param entitySetName name of the set to be created
     * @return {@link CustomEdmProvider CSDL} entity set
     * @throws ODataApplicationException if the container argument does not match the fixed container of this class.
     */
    @Override
    public CsdlEntitySet getEntitySet(final FullQualifiedName entityContainer, final String entitySetName)
            throws ODataApplicationException {
        if (!ENTITY_CONTAINER.equals(entityContainer)) {
            throw new ODataApplicationException("Invalid entity container " + entityContainer,
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
        }

        final CsdlEntitySet entitySet = new CsdlEntitySet();
        entitySet.setName(entitySetName);
        entitySet.setType(new FullQualifiedName(NAMESPACE, entitySetName));

        return entitySet;
    }

    /**
     * Creates a {@link CustomEdmProvider CSDL} entity container info.
     * @param entityContainerName namespace-qualified name of the entity container. This should match the fixed
     *                            container name defined for this class.
     * @return {@link CustomEdmProvider CSDL} entity container info containing the fixed container name defined for
     *     this class.
     * @throws ODataApplicationException if the container argument does not match the fixed container of this class.
     */
    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(final FullQualifiedName entityContainerName)

            throws ODataApplicationException {
        // This method is invoked when displaying the Service Document at e.g.
        // http://localhost:8080/DemoService/DemoService.svc
        if (entityContainerName == null || entityContainerName.equals(ENTITY_CONTAINER)) {
            final CsdlEntityContainerInfo entityContainerInfo = new CsdlEntityContainerInfo();
            entityContainerInfo.setContainerName(ENTITY_CONTAINER);
            return entityContainerInfo;
        } else {
            throw new ODataApplicationException("Invalid entity container " + entityContainerName,
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
        }
    }

    /**
     * Creates a list of {@link CustomEdmProvider CSDL} schema objects containing a single item which is created by
     * the schema translator service.
     * @return a list of {@link CustomEdmProvider CSDL} schema objects
     * @throws ODataApplicationException if there is an error while creating the entity container.
     */
    @Override
    public List<CsdlSchema> getSchemas() throws ODataApplicationException {
        final List<CsdlSchema> schemas = new ArrayList<>();
        final CsdlSchema schema = schemaTranslator.getSchema(schemaName);
        schema.setEntityContainer(getEntityContainer());
        schemas.add(schema);
        return schemas;
    }

    /**
     * Creates a {@link CustomEdmProvider CSDL} entity container containing entity sets created from database metadata.
     * @return {@link CustomEdmProvider CSDL} entity container
     * @throws ODataApplicationException if there is an error thrown by the database info service.
     */
    @Override
    public CsdlEntityContainer getEntityContainer() throws ODataApplicationException {
        // create EntitySets
        final List<CsdlEntitySet> entitySets = new ArrayList<>();

        final List<DatabaseTable> tables = databaseInfoService.getTables(schemaName);
        for (final DatabaseTable table : tables) {
            entitySets.add(getEntitySet(ENTITY_CONTAINER, table.getName()));
        }

        // create EntityContainer
        final CsdlEntityContainer entityContainer = new CsdlEntityContainer();
        entityContainer.setName(ENTITY_CONTAINER_NAME);
        entityContainer.setEntitySets(entitySets);

        return entityContainer;
    }
}

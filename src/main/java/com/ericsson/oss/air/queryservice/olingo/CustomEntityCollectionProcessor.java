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

import java.io.InputStream;
import java.util.List;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.queryoption.CountOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import com.ericsson.oss.air.queryservice.service.QueryService;

import lombok.Getter;
import lombok.Setter;

/**
 * Processor class for handling a collection of entities.
 */
@Component
@RequestScope
public class CustomEntityCollectionProcessor implements EntityCollectionProcessor {

    private OData odata;
    private @Getter @Setter String schema;
    private ServiceMetadata serviceMetadata;

    @Autowired
    private QueryService queryService;

    /**
     * Initializes private members. This method is called by the handler implementation in the Olingo library.
     * @param odata OData implementation
     * @param serviceMetadata service metadata implementation with entity data model reference
     */
    @Override
    public void init(final OData odata, final ServiceMetadata serviceMetadata) {
        this.odata = odata;
        this.serviceMetadata = serviceMetadata;
    }

    /**
     * Reads entity data using the query service and puts serialized content into the response.
     * @param request OData request object containing raw HTTP information
     * @param response OData response object
     * @param uriInfo parsed OData uri
     * @param responseFormat requested content type
     * @throws ODataApplicationException if there is an entity data model related failure or a query failure
     * @throws SerializerException if the response format is unsupported
     */
    @Override
    @SuppressWarnings("PMD.CloseResource")
    public void readEntityCollection(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
                                     final ContentType responseFormat)
            throws ODataApplicationException, SerializerException {
        // 1st we have retrieve the requested EntitySet from the uriInfo object
        // (representation of the parsed service URI)
        final List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        // in our example, the first segment is the EntitySet
        final UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
        final EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

        final SelectOption selectOption = uriInfo.getSelectOption();
        // 2nd: fetch the data from backend for this requested EntitySetName
        // it has to be delivered as EntitySet object
        final EntityCollection entityCollection = queryService.getData(edmEntitySet, schema, uriInfo);

        // 3rd: create a serializer based on the requested format (json)
        final ODataSerializer serializer = odata.createSerializer(responseFormat);

        // 4th: Now serialize the content: transform from the EntitySet object to
        // InputStream
        final EdmEntityType edmEntityType = edmEntitySet.getEntityType();
        final String selectList = odata.createUriHelper().buildContextURLSelectList(edmEntityType, null, selectOption);

        // Note: $select is handled by the lib, we only configure ContextURL + SerializerOptions
        // for performance reasons, it might be necessary to implement the $select manually
        final ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).selectList(selectList).build();
        final CountOption countOption = uriInfo.getCountOption();

        final String id = request.getRawBaseUri() + "/" + edmEntitySet.getName();
        final EntityCollectionSerializerOptions opts =
                EntityCollectionSerializerOptions
                        .with()
                        .select(selectOption)
                        .id(id)
                        .contextURL(contextUrl)
                        .count(countOption)
                        .build();

        final SerializerResult serializerResult = serializer.entityCollection(serviceMetadata, edmEntityType,
                entityCollection, opts);
        final InputStream serializedContent = serializerResult.getContent();

        // Finally: configure the response object: set the body, headers and status code
        response.setContent(serializedContent);
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }
}
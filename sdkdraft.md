# Query output

You can access the KPI results using the query output. It supports filtering for the output and allows access to historical data for a given retention period.
It is a REST interface that does not require interaction with any other capabilities.
It is based on the OData query language.

**Concepts**
- {schema} - Currently this is always "kpi".
- {entityset} - These are always KPI table names.

`GET /kpi-handling/exposure/v1/{schema}/$metadata` and
`GET /kpi-handling/exposure/v1/{schema}` endpoints allows you to get informed about the list of tables and their schemas.

`GET /kpi-handling/exposure/v1/{schema}/{entityset}` endpoint allows you to query KPI results.

OData filters can be applied in the query, allowing filtering such as:

- Querying the latest aggregation period of a table

  `GET http://eric-oss-pm-stats-query-service:8080/kpi-handling/exposure/v1/kpi/kpi_cell_guid_60?$orderby=aggregation_begin_time%20desc&$top=1`

- Querying the latest X hours in a table

  `GET http://eric-oss-pm-stats-query-service:8080/kpi-handling/exposure/v1/kpi/kpi_cell_guid_60?$filter=aggregation_begin_time%20eq%202023-07-13T00:00:00Z`

- Querying so that particular aggregation elements are filtered

  `GET http://eric-oss-pm-stats-query-service:8080/kpi-handling/exposure/v1/kpi/kpi_cell_guid_1440?$filter=contains(moFdn,%27ManagedElement15%27)&$orderby=pm_erab_rel_abnormal_mme_act%20desc`

- Querying for a specific KPI

  `GET http://eric-oss-pm-stats-query-service:8080/kpi-handling/exposure/v1/kpi/kpi_cell_guid_60?$select=pm_cell_downtime_auto_daily`

[OpenAPI]: #capabilities/psd-kpi-calculation/psd-kpi-querying-api-guide

**Retention**
The historical data is stored for 5 days. The retention period is set by the system administrator.
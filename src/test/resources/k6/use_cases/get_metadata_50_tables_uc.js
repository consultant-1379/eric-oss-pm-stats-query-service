import http from 'k6/http';
import { check, group } from 'k6';
import { Trend } from 'k6/metrics';

const getMetadataUCDuration = new Trend('get_metadata_schema_with_50_tables_duration');

export function getMetadataSchemaWith50TablesUC() {
  group("Get metadata from schema with 50 tables", function() {
      const res = http.get("http://query-service/kpi_exposure/loadtest2/$metadata");
      check(res, {
        'is status 200': (r) => r.status === 200
      });

      getMetadataUCDuration.add(res.timings.duration);
  })
}
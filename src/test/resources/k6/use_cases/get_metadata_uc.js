import http from 'k6/http';
import { check, group } from 'k6';
import { Trend } from 'k6/metrics';

const getMetadataUCDuration = new Trend('get_metadata_duration');

export function getMetadataUC() {
  group("Get metadata", function() {
      const res = http.get("http://query-service:8080/kpi_exposure/public/$metadata");
      check(res, {
        'is status 200': (r) => r.status === 200
      });

      getMetadataUCDuration.add(res.timings.duration);
  })
}
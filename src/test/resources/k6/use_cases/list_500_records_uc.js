import http from 'k6/http';
import { check, group } from 'k6';
import { Trend } from 'k6/metrics';

const listRecordsUCDuration = new Trend('list_500_records_duration');

export function list500RecordsUC() {
  group("List 500 records", function() {
      const res = http.get("http://query-service/kpi_exposure/loadtest1/table1");
      check(res, {
        'is status 200': (r) => r.status === 200
      });

      listRecordsUCDuration.add(res.timings.duration);
  })
}
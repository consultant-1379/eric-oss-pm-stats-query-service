import http from 'k6/http';
import { check, group } from 'k6';
import { Trend } from 'k6/metrics';

const listRecordsUCDuration = new Trend('list_2000_records_duration');

export function list2000RecordsUC() {
  group("List 2000 records", function() {
      const res = http.get("http://query-service/kpi_exposure/loadtest1/table3");
      check(res, {
        'is status 200': (r) => r.status === 200
      });

      listRecordsUCDuration.add(res.timings.duration);
  })
}
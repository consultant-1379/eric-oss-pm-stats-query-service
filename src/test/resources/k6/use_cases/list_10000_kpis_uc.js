import http from 'k6/http';
import { check, group } from 'k6';
import { Trend } from 'k6/metrics';

const listKpisUCDuration = new Trend('list_10000_kpis_duration');

export function list10000KpisUC() {
  group("List 10000 KPIS", function() {
      const res = http.get("http://query-service:8080/kpi_exposure/public/third_table?$top=10000");
      check(res, {
        'is status 200': (r) => r.status === 200
      });

      listKpisUCDuration.add(res.timings.duration);
  })
}
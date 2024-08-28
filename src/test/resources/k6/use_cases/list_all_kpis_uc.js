import http from 'k6/http';
import { check, group } from 'k6';
import { Trend } from 'k6/metrics';

const listKpisUCDuration = new Trend('list_all_kpis_duration');

export function listAllKpisUC() {
  group("List all KPIS", function() {
      const res = http.get("http://query-service:8080/kpi_exposure/public/third_table");
      check(res, {
        'is status 200': (r) => r.status === 200
      });

      listKpisUCDuration.add(res.timings.duration);
  })
}
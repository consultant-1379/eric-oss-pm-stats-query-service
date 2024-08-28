// The use cases as imported as modules published in the use_cases folder
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';
import { htmlReport } from "https://arm1s11-eiffel004.eiffel.gic.ericsson.se:8443/nexus/content/sites/oss-sites/common/k6/eric-k6-static-report-plugin/latest/bundle/eric-k6-static-report-plugin.js";
export {getMetadataUC} from "./use_cases/get_metadata_uc.js";
export {list1000KpisUC} from "./use_cases/list_1000_kpis_uc.js";
export {list10000KpisUC} from "./use_cases/list_10000_kpis_uc.js";
export {listAllKpisUC} from "./use_cases/list_all_kpis_uc.js";

export let options = {
    insecureSkipTLSVerify: true,
    hosts: {
      'query-service': '127.0.0.1'
    },
    thresholds: {
        "get_metadata_duration": ["max<3000"],
        "list_1000_kpis_duration": ["max<3000"],
        "list_10000_kpis_duration": ["max<3000"],
        "list_all_kpis_duration": ["max<3000"],
    },
    scenarios: {
        "get_metadata": {
          "exec": "getMetadataUC",
          "executor": "ramping-arrival-rate",
          "startTime": "0s",
          "timeUnit": "1s",
          preAllocatedVUs: 10,
          maxVUs: 50,
          "stages": [
            { "target": 10, "duration": "15s" },
            { "target": 50, "duration": "35s" },
            { "target": 0, "duration": "10s" }
          ]
        },
        "list_1000_kpis": {
          "exec": "list1000KpisUC",
          "executor": "ramping-arrival-rate",
          "startTime": "1m",
          "timeUnit": "1s",
          preAllocatedVUs: 10,
          maxVUs: 50,
          "stages": [
            { "target": 10, "duration": "15s" },
            { "target": 50, "duration": "35s" },
            { "target": 0, "duration": "10s" }
          ]
        },
        "list_10000_kpis": {
          "exec": "list10000KpisUC",
          "executor": "ramping-arrival-rate",
          "startTime": "2m",
          "timeUnit": "1s",
          preAllocatedVUs: 10,
          maxVUs: 50,
          "stages": [
            { "target": 10, "duration": "15s" },
            { "target": 30, "duration": "35s" },
            { "target": 0, "duration": "10s" }
          ]
        },
        "list_all_kpis": {
          "exec": "listAllKpisUC",
          "executor": "ramping-arrival-rate",
          "startTime": "3m",
          "timeUnit": "1s",
          preAllocatedVUs: 10,
          maxVUs: 50,
          "stages": [
            { "target": 5, "duration": "15s" },
            { "target": 10, "duration": "35s" },
            { "target": 0, "duration": "10s" }
          ]
        }
    }
};

// main default function that drives the test
export default function () {
    getMetadataUC();
}

// handleSummary produces the reports
export function handleSummary(data) {
  console.log('Preparing the end-of-test summary...');

  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }), // Show the text summary to stdout...
    './result.html': htmlReport(data),
  }
}
// The use cases as imported as modules published in the use_cases folder
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';
import { htmlReport } from "https://arm1s11-eiffel004.eiffel.gic.ericsson.se:8443/nexus/content/sites/oss-sites/common/k6/eric-k6-static-report-plugin/latest/bundle/eric-k6-static-report-plugin.js";
export {getMetadataSchemaWith10TablesUC} from "./use_cases/get_metadata_10_tables_uc.js";
export {getMetadataSchemaWith50TablesUC} from "./use_cases/get_metadata_50_tables_uc.js";
export {getMetadataSchemaWith100TablesUC} from "./use_cases/get_metadata_100_tables_uc.js";
export {list500RecordsUC} from "./use_cases/list_500_records_uc.js";
export {list1000RecordsUC} from "./use_cases/list_1000_records_uc.js";
export {list2000RecordsUC} from "./use_cases/list_2000_records_uc.js";
export {list5000RecordsUC} from "./use_cases/list_5000_records_uc.js";

export let options = {
    insecureSkipTLSVerify: true,
    discardResponseBodies: true,
    thresholds: {
        "get_metadata_schema_with_10_tables_duration": ["avg<=500"],
        "get_metadata_schema_with_50_tables_duration": ["avg<=1000"],
        "get_metadata_schema_with_100_tables_duration": ["avg<=2000"],
        "list_500_records_duration": ["avg<=1000"],
        "list_1000_records_duration": ["avg<=2000"],
        "list_2000_records_duration": ["avg<=2000"],
        "list_5000_records_duration": ["avg<=3000"],
    },
    scenarios: {
        "get_metadata_with_10_entity_types": {
          "exec": "getMetadataSchemaWith10TablesUC",
          "executor": "constant-arrival-rate",
          "startTime": "0s",
          duration: '30s',
          "timeUnit": "1s",
          rate: 50,
          preAllocatedVUs: 10,
          maxVUs: 50
        },
        "get_metadata_with_50_entity_types": {
          "exec": "getMetadataSchemaWith50TablesUC",
          "executor": "constant-arrival-rate",
          "startTime": "30s",
          duration: '30s',
          "timeUnit": "1s",
          rate: 50,
          preAllocatedVUs: 10,
          maxVUs: 50
        },
        "get_metadata_with_100_entity_types": {
          "exec": "getMetadataSchemaWith100TablesUC",
          "executor": "constant-arrival-rate",
          "startTime": "1m",
          duration: '30s',
          "timeUnit": "1s",
          rate: 50,
          preAllocatedVUs: 10,
          maxVUs: 50
        },
        "list_entities_with_500_records": {
          "exec": "list500RecordsUC",
          "executor": "constant-arrival-rate",
          "startTime": "1m30s",
          duration: '30s',
          "timeUnit": "1s",
          rate: 50,
          preAllocatedVUs: 10,
          maxVUs: 50
        },
        "list_entities_with_1000_records": {
          "exec": "list1000RecordsUC",
          "executor": "constant-arrival-rate",
          "startTime": "2m",
          duration: '30s',
          "timeUnit": "1s",
          rate: 50,
          preAllocatedVUs: 10,
          maxVUs: 50
        },
        "list_entities_with_2000_records": {
          "exec": "list2000RecordsUC",
          "executor": "constant-arrival-rate",
          "startTime": "2m30s",
          duration: '30s',
          "timeUnit": "1s",
          rate: 50,
          preAllocatedVUs: 10,
          maxVUs: 50
        },
        "list_entities_with_5000_records": {
          "exec": "list5000RecordsUC",
          "executor": "constant-arrival-rate",
          "startTime": "3m",
          duration: '30s',
          "timeUnit": "1s",
          rate: 50,
          preAllocatedVUs: 10,
          maxVUs: 50
        }
    }
};

// main default function that drives the test
export default function () {
}

// handleSummary produces the reports
export function handleSummary(data) {
  console.log('Preparing the end-of-test summary...');

  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }), // Show the text summary to stdout...
    './result.html': htmlReport(data),
  }
}
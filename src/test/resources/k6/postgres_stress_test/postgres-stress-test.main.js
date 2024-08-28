import sql from 'k6/x/sql';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';
import { Trend } from 'k6/metrics';
export { selectOneColumn } from './select_one_column.js';
export { selectTwoColumns } from './select_two_columns.js';
export { selectThreeColumns } from './select_three_columns.js';

const queryResultRowCount = new Trend('query_result_row_count');

const connStr = "postgres://postgres:superpwd@localhost:5433/dynamic_schema?sslmode=disable";
const db = sql.open('postgres', connStr);

const minBigInt = -922337203685477580;
const maxBigint = 922337203685477580;
const min = -21474836;
const max = 21474836

export function getMinBigInt() {
  return minBigInt;
}

export function getMaxBigInt() {
  return maxBigint;
}

export function getMin() {
  return min;
}

export function getMax() {
  return max;
}

export function getDb() {
  return db;
}

export const options = {
  scenarios: {
          "select_one_column": {
            "exec": "selectOneColumn",
            "executor": "constant-arrival-rate",
            "startTime": "0s",
            duration: '30s',
            "timeUnit": "1s",
            rate: 50,
            preAllocatedVUs: 2,
            maxVUs: 10
          },
          "select_two_columns": {
            "exec": "selectTwoColumns",
            "executor": "constant-arrival-rate",
            "startTime": "30s",
            duration: '30s',
            "timeUnit": "1s",
            rate: 50,
            preAllocatedVUs: 2,
            maxVUs: 10
          },
          "select_three_columns": {
            "exec": "selectThreeColumns",
            "executor": "constant-arrival-rate",
            "startTime": "1m",
            duration: '30s',
            "timeUnit": "1s",
            rate: 50,
            preAllocatedVUs: 2,
            maxVUs: 10
          },
        },
};

export function setup() {
  db.exec(`CREATE TABLE IF NOT EXISTS postgres_test_stress (
             date_time TIMESTAMP,
             first_kpi BIGINT,
             second_kpi INTEGER,
             third_kpi NUMERIC(10, 2)
           );`
         );

  db.exec(
    `CREATE OR REPLACE FUNCTION random_number_between(low BIGINT, high BIGINT, isDecimal BOOLEAN)
        RETURNS BIGINT AS
     $$
     BEGIN
       IF isDecimal = true THEN
         RETURN FLOOR(RANDOM() * (high-low + 1) + low);
       ELSE
         RETURN ROUND(CAST((RANDOM() * ((high-low) + 1) + low) as NUMERIC), 2);
       END IF;
     END;
     $$ LANGUAGE 'plpgsql' STRICT;`
  );

  db.exec(
      `CREATE OR REPLACE FUNCTION random_bigint_between(low BIGINT, high BIGINT)
          RETURNS BIGINT AS
       $$
       BEGIN
         RETURN FLOOR(RANDOM() * ((high-low) + 1) + low);
       END;
       $$ LANGUAGE 'plpgsql' STRICT;`
    );

  db.exec(
    `do $$
     BEGIN
       FOR r IN 1..1000000 loop
         INSERT INTO postgres_test_stress(date_time, first_kpi, second_kpi, third_kpi) VALUES
         ((select timestamp '2010-01-01 00:00:00' +
                 random() * (timestamp '2022-03-11 00:00:00' -
                             timestamp '2010-01-01 00:00:00')),
           random_number_between(-922337203685477580, 922337203685477580, false),
           random_number_between(-21474836, 21474836, false),
           random_number_between(-21474836, 21474836, true)
         );
       END loop;
     END;
     $$;`
  );
}

export function teardown() {
  db.exec('DELETE FROM postgres_test_stress;');
  db.exec('DROP TABLE postgres_test_stress;');
  db.close();
}

export function getRandomInt(min, max) {
    min = Math.ceil(min);
    max = Math.floor(max);
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

export default function () {
}

export function handleSummary(data) {
  console.log('Preparing the end-of-test summary...');
  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }), // Show the text summary to stdout...
  }
}
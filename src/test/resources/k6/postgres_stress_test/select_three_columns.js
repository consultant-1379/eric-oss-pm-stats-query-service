import sql from 'k6/x/sql';
import { Trend } from 'k6/metrics';
import { getMin, getMax, getMinBigInt, getMaxBigInt, getRandomInt, getDb } from './postgres-stress-test.main.js';

const queryThreeColumnsRowCount = new Trend('query_three_columns_row_count');

export function selectThreeColumns() {
  let condition1 = 'first_kpi > ' + getRandomInt(getMinBigInt(), 0) + ' AND first_kpi < ' + getRandomInt(0, getMaxBigInt());
  let condition2 = 'second_kpi > ' + getRandomInt(getMin(), 0) + ' AND second_kpi < ' + getRandomInt(0, getMax());
  let condition3 = 'third_kpi > ' + getRandomInt(getMin(), 0) + ' AND third_kpi < ' + getRandomInt(0, getMax());
  let results = sql.query(getDb(), 'SELECT * FROM postgres_test_stress WHERE (' + condition1 + ') AND (' + condition2 + ') AND (' + condition3 + ');');
  queryThreeColumnsRowCount.add(results.length);
}
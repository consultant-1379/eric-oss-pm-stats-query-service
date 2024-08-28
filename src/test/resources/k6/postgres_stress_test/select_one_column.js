import sql from 'k6/x/sql';
import { Trend } from 'k6/metrics';
import { getMin, getMax, getMinBigInt, getMaxBigInt, getRandomInt, getDb } from './postgres-stress-test.main.js';

const queryOneColumnRowCount = new Trend('query_one_column_row_count');

export function selectOneColumn() {
  let condition = 'first_kpi > ' + getRandomInt(getMinBigInt(), 0) + ' AND first_kpi < ' + getRandomInt(0, getMaxBigInt()) + ';';
  let results = sql.query(getDb(), 'SELECT * FROM postgres_test_stress WHERE ' + condition);
  queryOneColumnRowCount.add(results.length);
}
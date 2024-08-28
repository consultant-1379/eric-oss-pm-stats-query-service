CREATE TABLE first_table(
    date_time TIMESTAMP WITHOUT TIME ZONE,
    first_kpi SMALLINT,
    second_kpi INTEGER,
    third_kpi BIGINT,
    fourth_kpi DECIMAL(9, 4),
    fifth_kpi NUMERIC(10, 5),
    sixth_kpi REAL,
    seventh_kpi DOUBLE PRECISION
);

CREATE TABLE second_table(
    date_time TIMESTAMP,
    first_kpi BIGINT,
    second_kpi INTEGER,
    third_kpi NUMERIC(10, 5)
);

CREATE VIEW first_view AS SELECT date_time, second_kpi, fourth_kpi, sixth_kpi FROM first_table;
CREATE VIEW second_view AS SELECT date_time, first_kpi, third_kpi FROM second_table;

INSERT INTO first_table(date_time, first_kpi, second_kpi, third_kpi, fourth_kpi, fifth_kpi, sixth_kpi, seventh_kpi) VALUES
('2022-03-01 11:07:38.206333', -32768, -2147483648, -9223372036854775808, 12345.6789, 12345.67890, 987.654321, 1234567890.123456789012345),
('2022-03-01 10:23:02.206333', 32767, 2147483647, 9223372036854775807, -12345.6789, -12345.67890, -987.654321, -1234567890.123456789012345),
('2022-02-10 22:42:01.206333', 0, 21474, 92236854775807, -1235.679, -1235.690, -98.6521, -12347890.123456712345);

INSERT INTO second_table(date_time, first_kpi, second_kpi, third_kpi) VALUES
('2022-03-01 11:07:38.206333', 9223372036854775807, -2147483648, -12345.67890),
('2022-03-02 08:30:38.206333', 56546234234, -545334, 86955.483);
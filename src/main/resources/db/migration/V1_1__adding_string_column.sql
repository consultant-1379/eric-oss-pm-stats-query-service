ALTER TABLE second_table ADD COLUMN fourth_kpi VARCHAR;

INSERT INTO second_table(date_time, first_kpi, second_kpi, third_kpi, fourth_kpi) VALUES
('2022-03-01 11:07:38.206333', 9223372036854775807, -2147483648, -12345.67890, 'asd'),
('2022-03-02 08:30:38.206333', 56546234234, -545334, 86955.483, 'qwertz');
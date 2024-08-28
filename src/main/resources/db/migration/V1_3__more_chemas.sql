CREATE SCHEMA schema1;
CREATE SCHEMA schema2;
CREATE SCHEMA schema3;

CREATE TABLE schema1.entity(
    date_time TIMESTAMP,
    first_kpi BIGINT,
    second_kpi INTEGER,
    third_kpi NUMERIC(10, 2),
    fourth_kpi VARCHAR
);

CREATE TABLE schema2.entity(
    date_time TIMESTAMP,
    first_kpi BIGINT,
    second_kpi INTEGER,
    third_kpi NUMERIC(10, 2),
    fourth_kpi VARCHAR
);

CREATE TABLE schema3.entity(
    date_time TIMESTAMP,
    first_kpi BIGINT,
    second_kpi INTEGER,
    third_kpi NUMERIC(10, 2),
    fourth_kpi VARCHAR
);

do $$
BEGIN
  FOR r IN 1..100000 loop
    INSERT INTO schema1.entity(date_time, first_kpi, second_kpi, third_kpi, fourth_kpi) VALUES
    ((select timestamp '2010-01-01 00:00:00' +
            random() * (timestamp '2022-03-11 00:00:00' -
                        timestamp '2010-01-01 00:00:00')),
      random_integer_between(-21474836, 21474836),
      random_integer_between(-21474836, 21474836),
      random_integer_between(-21474836, 21474836),
      'schema1'
    );
  END loop;
END;
$$;

do $$
BEGIN
  FOR r IN 1..100000 loop
    INSERT INTO schema2.entity(date_time, first_kpi, second_kpi, third_kpi, fourth_kpi) VALUES
    ((select timestamp '2010-01-01 00:00:00' +
            random() * (timestamp '2022-03-11 00:00:00' -
                        timestamp '2010-01-01 00:00:00')),
      random_integer_between(-21474836, 21474836),
      random_integer_between(-21474836, 21474836),
      random_integer_between(-21474836, 21474836),
      'schema2'
    );
  END loop;
END;
$$;

do $$
BEGIN
  FOR r IN 1..100000 loop
    INSERT INTO schema3.entity(date_time, first_kpi, second_kpi, third_kpi, fourth_kpi) VALUES
    ((select timestamp '2010-01-01 00:00:00' +
            random() * (timestamp '2022-03-11 00:00:00' -
                        timestamp '2010-01-01 00:00:00')),
      random_integer_between(-21474836, 21474836),
      random_integer_between(-21474836, 21474836),
      random_integer_between(-21474836, 21474836),
      'schema3'
    );
  END loop;
END;
$$;
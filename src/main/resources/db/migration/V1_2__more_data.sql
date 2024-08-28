CREATE OR replace function random_string() returns text LANGUAGE SQL AS $$
  SELECT string_agg (substr('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789', ceil (random() * 62)::integer, 1), '')
  FROM generate_series(1, 5)
$$;

CREATE OR REPLACE FUNCTION random_integer_between(low INT ,high INT)
   RETURNS INT AS
$$
BEGIN
   RETURN floor(random()* (high-low + 1) + low);
END;
$$ LANGUAGE 'plpgsql' STRICT;

CREATE TABLE third_table(
    date_time TIMESTAMP,
    first_kpi BIGINT,
    second_kpi INTEGER,
    third_kpi NUMERIC(10, 2)
);

do $$
BEGIN
  FOR r IN 1..100000 loop
    INSERT INTO third_table(date_time, first_kpi, second_kpi, third_kpi) VALUES
    ((select timestamp '2010-01-01 00:00:00' +
            random() * (timestamp '2022-03-11 00:00:00' -
                        timestamp '2010-01-01 00:00:00')),
      random_integer_between(-21474836, 21474836),
      random_integer_between(-21474836, 21474836),
      random_integer_between(-21474836, 21474836)
    );
  END loop;
END;
$$;
CREATE SCHEMA loadtest1;

do $$
BEGIN
  FOR r IN 1..10 loop
    EXECUTE format('CREATE TABLE loadtest1.table%s (date_time TIMESTAMP WITHOUT TIME ZONE, first_kpi INT);', r);
  END loop;
END;
$$;

do $$
BEGIN
  FOR r IN 1..500 loop
    INSERT INTO loadtest1.table1(date_time, first_kpi) VALUES
    ((select timestamp '2010-01-01 00:00:00' +
            random() * (timestamp '2022-03-11 00:00:00' -
                        timestamp '2010-01-01 00:00:00')),
      r
    );
  END loop;
END;
$$;

do $$
BEGIN
  FOR r IN 1..1000 loop
    INSERT INTO loadtest1.table2(date_time, first_kpi) VALUES
    ((select timestamp '2010-01-01 00:00:00' +
            random() * (timestamp '2022-03-11 00:00:00' -
                        timestamp '2010-01-01 00:00:00')),
      r
    );
  END loop;
END;
$$;

do $$
BEGIN
  FOR r IN 1..2000 loop
    INSERT INTO loadtest1.table3(date_time, first_kpi) VALUES
    ((select timestamp '2010-01-01 00:00:00' +
            random() * (timestamp '2022-03-11 00:00:00' -
                        timestamp '2010-01-01 00:00:00')),
      r
    );
  END loop;
END;
$$;

do $$
BEGIN
  FOR r IN 1..5000 loop
    INSERT INTO loadtest1.table4(date_time, first_kpi) VALUES
    ((select timestamp '2010-01-01 00:00:00' +
            random() * (timestamp '2022-03-11 00:00:00' -
                        timestamp '2010-01-01 00:00:00')),
      r
    );
  END loop;
END;
$$;

CREATE SCHEMA loadtest2;

do $$
BEGIN
  FOR r IN 1..50 loop
    EXECUTE format('CREATE TABLE loadtest2.table%s (date_time TIMESTAMP WITHOUT TIME ZONE, first_kpi INT);', r);
  END loop;
END;
$$;

CREATE SCHEMA loadtest3;

do $$
BEGIN
  FOR r IN 1..100 loop
    EXECUTE format('CREATE TABLE loadtest3.table%s (date_time TIMESTAMP WITHOUT TIME ZONE, first_kpi INT);', r);
  END loop;
END;
$$;
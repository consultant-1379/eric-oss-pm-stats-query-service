--
-- COPYRIGHT Ericsson 2023
--
-- The copyright to the computer program(s) herein is the property of
-- Ericsson Inc. The programs may be used and/or copied only with written
-- permission from Ericsson Inc. or in accordance with the terms and
-- conditions stipulated in the agreement/contract under which the
-- program(s) have been supplied.
--

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

INSERT INTO first_table(date_time, first_kpi, second_kpi, third_kpi, fourth_kpi, fifth_kpi, sixth_kpi, seventh_kpi) VALUES
('2022-03-01 11:07:38.206333', -32768, -2147483648, -9223372036854775808, 12345.6789, 12345.67890, 987.654321, 1234567890.123456789012345),
('2022-03-01 10:23:02.206333', 32767, 2147483647, 9223372036854775807, -12345.6789, -12345.67890, -987.654321, -1234567890.123456789012345),
('2022-02-10 22:42:01.206333', 0, 21474, 92236854775807, -1235.679, -1235.690, -98.6521, -12347890.123456712345);

CREATE TABLE second_table(
    date_time TIMESTAMP,
    first_kpi BIGINT,
    second_kpi INTEGER,
    third_kpi NUMERIC(10, 5)
);

INSERT INTO second_table(date_time, first_kpi, second_kpi, third_kpi) VALUES
('2022-03-01 11:07:38.206333', 9223372036854775807, -2147483648, -12345.67890),
('2022-03-02 08:30:38.206333', 56546234234, -545334, 86955.483);

ALTER TABLE second_table ADD COLUMN fourth_kpi VARCHAR;

INSERT INTO second_table(date_time, first_kpi, second_kpi, third_kpi, fourth_kpi) VALUES
('2022-03-01 11:07:38.206333', 9223372036854775807, -2147483648, -12345.67890, 'asd'),
('2022-03-02 08:30:38.206333', 56546234234, -545334, 86955.483, 'qwertz');

CREATE TABLE third_table(
    date_time TIMESTAMP,
    first_kpi BIGINT,
    second_kpi INTEGER,
    third_kpi NUMERIC(10, 2)
);

CREATE OR replace function random_string() returns text LANGUAGE SQL AS $$
  SELECT string_agg (substr('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789', ceil (random() * 62)::integer, 1), '')
  FROM generate_series(1, 5)
$$;

CREATE OR REPLACE FUNCTION random_integer_between(low INT ,high INT)
   RETURNS INT AS
'
BEGIN
   RETURN floor(random()* (high-low + 1) + low);
END;
' LANGUAGE PLPGSQL STRICT;

do '
BEGIN
  FOR r IN 1..100000 loop
    INSERT INTO third_table(date_time, first_kpi, second_kpi, third_kpi) VALUES
    ((select timestamp ''2010-01-01 00:00:00'' +
            random() * (timestamp ''2022-03-11 00:00:00'' -
                        timestamp ''2010-01-01 00:00:00'')),
      random_integer_between(-21474836, 21474836),
      random_integer_between(-21474836, 21474836),
      random_integer_between(-21474836, 21474836)
    );
  END loop;
END;
';

CREATE VIEW first_view AS SELECT date_time, second_kpi, fourth_kpi, sixth_kpi FROM first_table;

CREATE VIEW second_view AS SELECT date_time, first_kpi, third_kpi FROM second_table;


CREATE SCHEMA new_schema;

CREATE TABLE new_schema.new_table(
    date_time TIMESTAMP,
    first_kpi BIGINT,
    second_kpi INTEGER,
    third_kpi NUMERIC(10, 5),
    fourth_kpi VARCHAR
);

INSERT INTO new_schema.new_table(date_time, first_kpi, second_kpi, third_kpi, fourth_kpi) VALUES
  ('2022-03-01 11:07:38.206333', 45, 468221, 33465, 'new_kpi'),
  ('2022-03-01 11:07:38.206333', 26, 218221, 89346, 'old_kpi');

CREATE TABLE new_schema.table_all_types(
    c_1 bigint,
    c_2 bigserial,
    c_3 bit(1),
    c_4 bit varying(3),
    c_5 boolean,
    c_6 box,
    c_7 bytea,
    c_8 character(3),
    c_9 character varying(4),
    c_10 cidr,
    c_11 circle,
    c_12 date,
    c_13 double precision,
    c_14 inet,
    c_15 integer,
    c_16 interval,
    c_17 json,
    c_18 jsonb,
    c_19 line,
    c_20 lseg,
    c_21 macaddr,
    c_22 macaddr8,
    c_23 money,
    c_24 numeric(8, 3),
    c_25 path,
    c_26 pg_lsn,
    c_27 pg_snapshot,
    c_28 point,
    c_29 polygon,
    c_30 real,
    c_31 smallint,
    c_32 smallserial,
    c_33 serial,
    c_34 text,
    c_35 time without time zone,
    c_36 time with time zone,
    c_37 timestamp without time zone,
    c_38 timestamp with time zone,
    c_39 tsquery,
    c_40 tsvector,
    c_41 txid_snapshot,
    c_42 uuid,
    c_43 xml
);

INSERT INTO new_schema.table_all_types(c_1, c_2, c_3, c_4, c_5, c_6, c_7, c_8, c_9, c_10, c_11, c_12, c_13, c_14, c_15, c_16, c_17, c_18, c_19, c_20, c_21, c_22, c_23, c_24, c_25, c_26, c_27, c_28, c_29, c_30, c_31, c_32, c_33, c_34, c_35, c_36, c_37, c_38, c_39, c_40, c_41, c_42, c_43) VALUES
(1, 3, B'1', B'101', true, BOX( POINT( 3,5 ), POINT( 1,2 )), decode('013d7d16d7ad4fefb61bd95b765c8ceb', 'hex'), 'chr', 'test', '192.168/16', CIRCLE(POINT(1.2, 123.1), 10), '2013-06-01', 99.2, '192.168.2.1/24', 23, interval '40 minute', '{"prop": "value"}', '{"prop": "value"}', LINE(POINT(1, 1), POINT(2, 2)), LSEG(POINT(1, 1), POINT(2, 2)), '08:00:2b:01:02:03', '08:00:2b:01:02:03:04:05', '123', 12345.32, path('((0,0),(1,1),(2,0))'::polygon), '16/B374D848', null, POINT(2, 2), polygon('((0,0),2.0)'::circle), 1, 2, 3, 4, 'text', '11:10:33', '04:05:06-08:00', '2004-10-19 10:23:54', '2004-10-19 10:23:54+02', 'fat & rat', 'test tsvector', null, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', '<foo>bar</foo>'::xml),
(2, 4, B'1', B'101', true, BOX( POINT( 3,5 ), POINT( 1,2 )), decode('013d7d16d7ad4fefb61bd95b765c8ceb', 'hex'), 'chr', 'test', '192.168/16', CIRCLE(POINT(1.2, 123.1), 10), '2013-06-01', 99.2, '192.168.2.1/24', 23, interval '40 minute', '{"prop": "value"}', '{"prop": "value"}', LINE(POINT(1, 1), POINT(2, 2)), LSEG(POINT(1, 1), POINT(2, 2)), '08:00:2b:01:02:03', '08:00:2b:01:02:03:04:05', '123', 12345.32, path('((0,0),(1,1),(2,0))'::polygon), '16/B374D848', null, POINT(2, 2), polygon('((0,0),2.0)'::circle), 1, 2, 3, 4, 'text', '11:10:33', '04:05:06-08:00', '2004-10-19 10:23:54', '2004-10-19 10:23:54+02', 'fat & rat', 'test tsvector', null, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', '<foo>bar</foo>'::xml),
(3, 5, B'1', B'101', true, BOX( POINT( 3,5 ), POINT( 1,2 )), decode('013d7d16d7ad4fefb61bd95b765c8ceb', 'hex'), 'chr', 'test', '192.168/16', CIRCLE(POINT(1.2, 123.1), 10), '2013-06-01', 99.2, '192.168.2.1/24', 23, interval '40 minute', '{"prop": "value"}', '{"prop": "value"}', LINE(POINT(1, 1), POINT(2, 2)), LSEG(POINT(1, 1), POINT(2, 2)), '08:00:2b:01:02:03', '08:00:2b:01:02:03:04:05', '123', 12345.32, path('((0,0),(1,1),(2,0))'::polygon), '16/B374D848', null, POINT(2, 2), polygon('((0,0),2.0)'::circle), 1, 2, 3, 4, 'text', '11:10:33', '04:05:06-08:00', '2004-10-19 10:23:54', '2004-10-19 10:23:54+02', 'fat & rat', 'test tsvector', null, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', '<foo>bar</foo>'::xml);


-- #Segment: PostgreSql arrays

CREATE TABLE IF NOT EXISTS new_schema.table_all_array_types
(
    c_1 bigint[],
    c_3 bit(1)[],
    c_4 bit varying(3)[],
    c_5 boolean[],
    c_6 box[],
    c_8 character(3)[] COLLATE pg_catalog."default",
    c_9 character varying(4)[] COLLATE pg_catalog."default",
    c_10 cidr[],
    c_11 circle[],
    c_12 date[],
    c_13 double precision[],
    c_14 inet[],
    c_15 integer[],
    c_16 interval[],
    c_17 json[],
    c_18 jsonb[],
    c_19 line[],
    c_20 lseg[],
    c_21 macaddr[],
    c_22 macaddr8[],
    c_24 numeric(8,3)[],
    c_25 path[],
    c_26 pg_lsn[],
    c_28 point[],
    c_29 polygon[],
    c_30 real[],
    c_31 smallint[],
    c_34 text[] COLLATE pg_catalog."default",
    c_35 time without time zone[],
    c_36 time with time zone[],
    c_37 timestamp without time zone[],
    c_38 timestamp with time zone[],
    c_39 tsquery[],
    c_40 tsvector[],
    c_42 uuid[],
    c_43 xml[]
);

INSERT INTO new_schema.table_all_array_types(
	c_1, c_3, c_4, c_5, c_6, c_8, c_9, c_10, c_11, c_12, c_13, c_14, c_15, c_16, c_17, c_18, c_19, c_20, c_21, c_22, c_24,
	c_25, c_26, c_28, c_29, c_30, c_31, c_34, c_35, c_36, c_37, c_38, c_39, c_40, c_42, c_43)
	VALUES
	(
		ARRAY[123456,234567],  											                -- c_1
		ARRAY[B'1', B'0'], 												                -- c_3
		ARRAY[B'101', B'100'],											                -- c_4
		ARRAY[true, false],												                -- c_5
		ARRAY[BOX(POINT(1,2), POINT(3,5)), BOX(POINT(8,4), POINT(10,6))],				-- c_6
		ARRAY['chr', 'abc'],												            -- c_8
		ARRAY['test', 'abcd'],											                -- c_9
		'{"192.168/16", "10.1.2.3/32"}',										        -- c_10
		ARRAY[CIRCLE(POINT(1.2, 123.1), 10), CIRCLE(POINT(3.4, 5.6), 20)],				-- c_11
		'{"2013-06-01", "2022-09-10"}',										            -- c_12
		ARRAY[99.2, 3.14159],												            -- c_13
		'{"192.168.2.1/24", "192.168.0.1/24"}',									        -- c_14
		ARRAY[23, 45], 												                    -- c_15
		ARRAY[interval '40 minute', interval '2 hours'],								-- c_16
		ARRAY['{"prop1": "value1"}', '{"prop2": "value2"}']::json[],					-- c_17
		ARRAY['{"single": "value"}']::jsonb[],									        -- c_18
		ARRAY[LINE(POINT(1, 1), POINT(2, 2))],									        -- c_19
		ARRAY[LSEG(POINT(1, 1), POINT(2, 2))],  									    -- c_20
		ARRAY['08:00:2b:01:02:03']::macaddr[],									        -- c_21
		ARRAY['08:00:2b:01:02:03:04:05']::macaddr8[], 								    -- c_22
		ARRAY[12345.32, 6789.45],											            -- c_24
		ARRAY[path('((0,0),(1,1),(2,0))'::polygon)],								    -- c_25
		ARRAY['16/B374D848']::pg_lsn[],										            -- c_26
		ARRAY[POINT(2, 2),POINT(3, 3)],										            -- c_28
		ARRAY[polygon('((0,0),2.0)'::circle)],									        -- c_29
		ARRAY[1.1, 2.2, 3.3],												            -- c_30
		ARRAY[2,3,4,5,6], 												                -- c_31
		ARRAY['text1','text2'],											                -- c_34
		ARRAY['11:10:33']::time without time zone[],								    -- c_35
		ARRAY['04:05:06-08:00']::time with time zone[], 								-- c_36
		ARRAY['2004-10-19 10:23:54']::timestamp without time zone[], 					-- c_37
		ARRAY['2004-10-19 10:23:54+02']::timestamp with time zone[], 					-- c_38
		ARRAY['fat & rat']::tsquery[],										            -- c_39
		ARRAY['test tsvector']::tsvector[], 										    -- c_40
		ARRAY['a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11']::uuid[], 							-- c_42
		ARRAY['<foo>bar</foo>'::xml, '<dev>null</dev>'::xml]							-- c_43
	),
	(
		ARRAY[789012,34567],  											                -- c_1
		ARRAY[B'0', B'1'], 												                -- c_3
		ARRAY[B'001', B'010'],											                -- c_4
		ARRAY[false, false, true],											            -- c_5
		ARRAY[BOX(POINT(10,20), POINT(30,50)), BOX(POINT(80,40), POINT(100,60))],       -- c_6
		ARRAY['def', 'ghi'],												            -- c_8
		ARRAY['efgh', 'spqr'],											                -- c_9
		'{"190.168/16", "10.4.5.6/32"}',										        -- c_10
		ARRAY[CIRCLE(POINT(3.4, 456.7), 11), CIRCLE(POINT(4.6, 7.8), 21)],				-- c_11
		'{"2014-07-02", "2023-05-11"}',										            -- c_12
		ARRAY[188.3, 2.781],												            -- c_13
		'{"192.168.1.1/24", "232.104.0.1/24"}',									        -- c_14
		ARRAY[67, 89], 												                    -- c_15
		ARRAY[interval '20 minute', interval '4 hours'],								-- c_16
		ARRAY['{"prop3": "value3"}', '{"prop4": "value4"}']::json[],					-- c_17
		ARRAY['{"single2": "value2"}']::jsonb[],									    -- c_18
		ARRAY[LINE(POINT(10, 10), POINT(20, 20))],									    -- c_19
		ARRAY[LSEG(POINT(10, 10), POINT(20, 20))], 									    -- c_20
		ARRAY['09:00:2b:01:02:03']::macaddr[],									        -- c_21
		ARRAY['09:00:2b:01:02:03:04:05']::macaddr8[], 								    -- c_22
		ARRAY[98765.43, 5432.1], 											            -- c_24
		ARRAY[path('((1,1),(2,2),(3,0))'::polygon)],								    -- c_25
		ARRAY['16/B374D848']::pg_lsn[],										            -- c_26
		ARRAY[POINT(3, 3),POINT(9, 9)],										            -- c_28
		ARRAY[polygon('((0,0),3.0)'::circle)],									        -- c_29
		ARRAY[2.2,7.7], 												                -- c_30
		ARRAY[4,5,6,7], 												                -- c_31
		ARRAY['text3','text4','text5','text6'],									        -- c_34
		ARRAY['12:10:33']::time without time zone[],							    	-- c_35
		ARRAY['05:05:06-08:00']::time with time zone[], 								-- c_36
		ARRAY['2005-10-19 10:23:54',
		      '2006-11-20 11:34:05']::timestamp without time zone[], 					-- c_37
		ARRAY['2005-10-19 10:23:54+02']::timestamp with time zone[], 					-- c_38
		ARRAY['fat & cat']::tsquery[],										            -- c_39
		ARRAY['test2 tsvector']::tsvector[], 									        -- c_40
		ARRAY['a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
		      'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11']::uuid[], 							-- c_42
		ARRAY['<foo>bar2</foo>'::xml, '<dev>null0</dev>'::xml]							-- c_43
	),
	(
    	ARRAY[1,null],  											                    -- c_1
    	ARRAY[B'1', null, B'0'], 												        -- c_3
    	ARRAY[B'101', null, B'100'],											        -- c_4
    	ARRAY[true, false, null],												        -- c_5
    	ARRAY[BOX(POINT(1,2), POINT(3,5)), BOX(POINT(8,4), POINT(10,6)), null],			-- c_6
    	ARRAY['chr', 'abc', null],												        -- c_8
    	ARRAY[null, 'test', 'abcd'],											        -- c_9
    	'{"192.168/16", "10.1.2.3/32", null}',										    -- c_10
    	ARRAY[CIRCLE(POINT(1.2, 123.1), 10), CIRCLE(POINT(3.4, 5.6), 20), null],		-- c_11
    	'{"2013-06-01", "2022-09-10", null}',										    -- c_12
    	ARRAY[99.2, null, 3.14159],												        -- c_13
    	'{null, "192.168.2.1/24", "192.168.0.1/24"}',									-- c_14
    	ARRAY[23, null, 45], 												            -- c_15
    	ARRAY[interval '40 minute', interval '2 hours', null],							-- c_16
    	ARRAY['{"prop1": "value1"}', '{"prop2": "value2"}', null]::json[],				-- c_17
    	ARRAY['{"single": null}']::jsonb[],									            -- c_18
    	ARRAY[LINE(POINT(1, 1), POINT(2, 2)), null],									-- c_19
    	ARRAY[null, LSEG(POINT(1, 1), POINT(2, 2))],  									-- c_20
    	ARRAY['08:00:2b:01:02:03', null]::macaddr[],									-- c_21
    	ARRAY[null, '08:00:2b:01:02:03:04:05']::macaddr8[], 							-- c_22
    	ARRAY[12345.32, 6789.45, null],											        -- c_24
    	ARRAY[path('((0,0),(1,1),(2,0))'::polygon), null],								-- c_25
    	ARRAY['16/B374D848', null]::pg_lsn[],										    -- c_26
    	ARRAY[POINT(2, 2),POINT(3, 3), null],										    -- c_28
    	ARRAY[polygon('((0,0),2.0)'::circle), null],									-- c_29
    	ARRAY[1.1, 2.2, null, 3.3],												        -- c_30
    	ARRAY[null, 2,3,4,5,6], 												        -- c_31
    	ARRAY['text1',null,'text2'],											        -- c_34
    	ARRAY['11:10:33',null]::time without time zone[],								-- c_35
    	ARRAY['04:05:06-08:00',null]::time with time zone[], 							-- c_36
    	ARRAY[null,'2004-10-19 10:23:54']::timestamp without time zone[], 				-- c_37
    	ARRAY[null,'2004-10-19 10:23:54+02']::timestamp with time zone[], 				-- c_38
    	ARRAY[null, 'fat & rat', null]::tsquery[],										-- c_39
    	ARRAY[null, 'test tsvector']::tsvector[], 										-- c_40
    	ARRAY['a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', null]::uuid[], 					-- c_42
    	ARRAY['<foo>bar</foo>'::xml, '<dev>null</dev>'::xml, null]						-- c_43
    );

INSERT INTO new_schema.table_all_array_types(c_1)
	VALUES (null);

-- END OF #Segment: PostgreSql arrays


-- #Segment: partitioned tables

CREATE SCHEMA partitest;

CREATE TABLE IF NOT EXISTS partitest.kpi_rolling_aggregation_1440
(
    agg_column_0             INTEGER,
    aggregation_begin_time   TIMESTAMP NOT NULL,
    aggregation_end_time     TIMESTAMP,
    rolling_sum_integer_1440 INTEGER,
    rolling_max_integer_1440 INTEGER
) PARTITION BY RANGE (aggregation_begin_time);

CREATE TABLE IF NOT EXISTS partitest.kpi_rolling_aggregation_1440_p_2023_01_01
    PARTITION OF partitest.kpi_rolling_aggregation_1440
    FOR
    VALUES
    FROM ('2023-01-01 00:00:00') TO ('2023-01-02 00:00:00');

CREATE TABLE IF NOT EXISTS partitest.kpi_rolling_aggregation_1440_p_2023_01_02
    PARTITION OF partitest.kpi_rolling_aggregation_1440
    FOR
    VALUES
    FROM ('2023-01-02 00:00:00') TO ('2023-01-03 00:00:00');

CREATE TABLE IF NOT EXISTS partitest.kpi_rolling_aggregation_1440_p_2023_01_03
    PARTITION OF partitest.kpi_rolling_aggregation_1440
    FOR
    VALUES
    FROM ('2023-01-03 00:00:00') TO ('2023-01-04 00:00:00');

INSERT INTO partitest.kpi_rolling_aggregation_1440 ("agg_column_0", "aggregation_begin_time", "aggregation_end_time" ,
"rolling_sum_integer_1440", "rolling_max_integer_1440") VALUES
(1, '2023-01-01 00:00:00','2023-01-02 00:00:00',1,2),
(2, '2023-01-02 00:00:00','2023-01-03 00:00:00',1,2),
(3, '2023-01-03 00:00:00','2023-01-04 00:00:00',1,2),
(4, '2023-01-01 00:00:00','2023-01-02 00:00:00',1,2),
(5, '2023-01-02 00:00:00','2023-01-03 00:00:00',1,2);

CREATE TABLE partitest.kpi_simple
(
    agg_column_0             INTEGER,
    agg_column_1             INTEGER,
    dim_1                    CHARACTER VARYING(10),
    dim_2                    CHARACTER VARYING(255),
    rolling_sum              INTEGER
);

INSERT INTO partitest.kpi_simple ("agg_column_0", "agg_column_1", "dim_1", "dim_2", "rolling_sum") VALUES
(2022, 11, 'EU', '4G', 10),
(2022, 12, 'EU', '4G', 20),
(2023, 1, 'EU', '5G', 100),
(2023, 2, 'EU', '5G', 200);

CREATE TABLE IF NOT EXISTS partitest.sample_regional
(
     towercell               CHARACTER VARYING(50),
     region                  CHARACTER VARYING(10),
     signal                  REAL,
     CONSTRAINT kpi_regional_pkey PRIMARY KEY (towercell, region)
) PARTITION BY LIST(region);

CREATE TABLE IF NOT EXISTS partitest.sample_regional_west
	PARTITION OF partitest.sample_regional
    FOR VALUES IN ('WEST');

CREATE TABLE IF NOT EXISTS partitest.sample_regional_east
	PARTITION OF partitest.sample_regional
    FOR VALUES IN ('EAST');

CREATE TABLE IF NOT EXISTS partitest.sample_regional_north
	PARTITION OF partitest.sample_regional
    FOR VALUES IN ('NORTH');

CREATE TABLE IF NOT EXISTS partitest.sample_regional_south
	PARTITION OF partitest.sample_regional
    FOR VALUES IN ('SOUTH', 'SOUTHEAST', 'SOUTHWEST');

INSERT INTO partitest.sample_regional ("towercell", "region", "signal") VALUES
('A001_T01_C01', 'WEST', 0.76),
('A001_T01_C02', 'WEST', 0.51),
('A001_T02_C01', 'WEST', 0.92),
('A061_T01_C01', 'EAST', 0.66),
('A061_T31_C45', 'EAST', 0.70),
('A191_T11_C05', 'NORTH', 0.98),
('A191_T11_C06', 'NORTH', 0.88),
('A667_T60_C65', 'SOUTH', 0.18),
('A697_T60_C66', 'SOUTHWEST', 0.32);

-- END OF #Segment: partitioned tables


-- #Segment: case sensitive columns

CREATE SCHEMA mixed_schema;

CREATE TABLE mixed_schema.kpi_sensitive (
    date_time     TIMESTAMP WITHOUT TIME ZONE,
    "First_kpi"   SMALLINT,
    "sECOND_kpi"  INTEGER,
    "third_KPI"   BIGINT,
    "fOURTH_KPi"  DECIMAL(9, 4),
    "Fifth_kpI"   NUMERIC(10, 5),
    "SIXTH_KPI"   REAL,
    "seventh_kpi" DOUBLE PRECISION
);

CREATE TABLE mixed_schema.kpi_csac (
    "moFdn"       CHARACTER VARYING(255),
    "nodeFDN"     CHARACTER VARYING(255),
    "workHours"   INTEGER
);

INSERT INTO mixed_schema.kpi_sensitive
  (date_time, "First_kpi", "sECOND_kpi", "third_KPI", "fOURTH_KPi", "Fifth_kpI", "SIXTH_KPI", "seventh_kpi") VALUES
('2023-05-01 07:21:23.444333', 0, 10, 92236854775807, -1235.679, -1235.690, -98.6521, -12347890.123456712345),
('2023-05-02 08:31:34.555678', 1, 11, -9223372036854775808, 12345.6789, 12345.67890, 987.654321, 1234567890.123456789012345),
('2023-05-03 09:41:45.667776', 2, 12, 9223372036854775807, -12345.6789, -12345.67890, -987.654321, -1234567890.123456789012345),
('2023-05-04 10:51:56.788877', 3, 13, -9223372036854775808, 12345.6789, 12345.67890, 987.654321, 1234567890.123456789012345),
('2023-05-05 11:00:00.820425', 4, 14, 9223372036854775807, -12345.6789, -12345.67890, -987.654321, -1234567890.123456789012345);

INSERT INTO mixed_schema.kpi_csac ("moFdn", "nodeFDN", "workHours") VALUES
('/epg:epg/pgw/ns[name=131-000259]/apn[name=apn01.ericsson.se]', 'ManagedElement=NodeFDNManagedElement0002,Equipment=3,SupportUnit=2',12),
('/epg:epg/pgw/ns[name=131-000259]/apn[name=apn01.ericsson.se]', 'ManagedElement=NodeFDNManagedElement0099,Equipment=2,SupportUnit=1',16),
('/epg:epg/ftc/ns[name=123-001896]/apn[name=offline.ericsson.hu]', 'ManagedElement=NodeFDNManagedElement0002,Equipment=3,SupportUnit=2',20);

-- END OF #Segment: case sensitive columns


-- #Segment: paging

CREATE SCHEMA paging_schema;

CREATE TABLE paging_schema.medium_table(
    date_time DATE,
    int_kpi INTEGER,
    flt_kpi NUMERIC(10, 2)
);

CREATE TABLE paging_schema.large_table(
    date_time DATE,
    int_kpi INTEGER,
    flt_kpi NUMERIC(10, 2)
);

do '
BEGIN
  FOR r IN 1..100 loop
    INSERT INTO paging_schema.medium_table(date_time, int_kpi, flt_kpi) VALUES
    ((SELECT date(''2023-01-01'') + r - 1),
      1000 + r,
      0.1 + r
    );
  END loop;
END;
';

do '
BEGIN
  FOR r IN 1..20000 loop
    INSERT INTO paging_schema.large_table(date_time, int_kpi, flt_kpi) VALUES
    ((SELECT date(''1990-01-01'') + r - 1),
      100000 + r,
      0.1 + r
    );
  END loop;
END;
';

VACUUM (ANALYZE) paging_schema.medium_table;
VACUUM (ANALYZE) paging_schema.large_table;

-- END OF #Segment: paging
-- Add all your SQL setup statements here. 

CREATE TABLE Users_lyledeng (
    username varchar(20) PRIMARY KEY NOT NULL,
    password varbinary(144) NOT NULL,
    balance int NOT NULL
);

CREATE TABLE Reservations_lyledeng (
    rid varchar(100) PRIMARY KEY NOT NULL,
    username varchar(20) REFERENCES Users_lyledeng NOT NULL,
    fid1 int FOREIGN KEY REFERENCES Flights(fid) NOT NULL,
    fid2 int FOREIGN KEY REFERENCES Flights(fid),
    paid int NOT NULL,
    cost int NOT NULL,
    date int NOT NULL
);  

-- When we test your submission, you can assume that the following base
-- tables have been created and loaded with data.  However, before testing
-- your own code, you will need to create and populate them on your
-- SQLServer instance

-- Do not alter the following tables' contents or schema in your code.

-- FLIGHTS(fid int primary key, 
--         month_id int,        -- 1-12
--         day_of_month int,    -- 1-31 
--         day_of_week_id int,  -- 1-7, 1 = Monday, 2 = Tuesday, etc
--         carrier_id varchar(7), 
--         flight_num int,
--         origin_city varchar(34), 
--         origin_state varchar(47), 
--         dest_city varchar(34), 
--         dest_state varchar(46), 
--         departure_delay int, -- in mins
--         taxi_out int,        -- in mins
--         arrival_delay int,   -- in mins
--         canceled int,        -- 1 means canceled
--         actual_time int,     -- in mins
--         distance int,        -- in miles
--         capacity int, 
--         price int            -- in $             
--         )

-- CARRIERS(cid varchar(7) primary key,
--          name varchar(83))

-- MONTHS(mid int primary key,
--        month varchar(9));	

-- WEEKDAYS(did int primary key,
--          day_of_week varchar(9));

CREATE TABLE Owner (
                            owner_id SERIAL PRIMARY KEY,
                            name VARCHAR(255) NOT NULL,
                            email VARCHAR(255) UNIQUE NOT NULL,
                            password VARCHAR(255) NOT NULL
);

CREATE TABLE Store (
                       store_id SERIAL PRIMARY KEY,
                       owner_id INTEGER NOT NULL,
                       name VARCHAR(255) NOT NULL,
                       FOREIGN KEY (owner_id)
                           REFERENCES Owner(owner_id)
                           ON DELETE CASCADE -- If the owner is deleted, their stores are too
);

CREATE TABLE StoreDaySchedule (
                                  store_day_schedule_id SERIAL PRIMARY KEY,
                                  store_id INTEGER NOT NULL,
                                  day_of_week VARCHAR(10) NOT NULL,
                                  open_time TIME NOT NULL,
                                  close_time TIME NOT NULL,
                                  FOREIGN KEY (store_id)
                                      REFERENCES Store(store_id)
                                      ON DELETE CASCADE -- If the store is deleted, its schedules are too
);

CREATE TABLE Employee (
                          employee_id SERIAL PRIMARY KEY,
                          store_id INTEGER NOT NULL,
                          name VARCHAR(255) NOT NULL,
                          required_hours DECIMAL(4, 2),
                          constraints TEXT,
                          preferences TEXT,
                          FOREIGN KEY (store_id)
                              REFERENCES Store(store_id)
                              ON DELETE CASCADE -- If the store is deleted, its employees are too
);

CREATE TABLE Shift (
                       shift_id SERIAL PRIMARY KEY,
                       employee_id INTEGER NOT NULL,
                       store_id INTEGER NOT NULL,
                       day DATE NOT NULL,
                       start_time TIME NOT NULL,
                       end_time TIME NOT NULL,
                       FOREIGN KEY (employee_id)
                           REFERENCES Employee(employee_id)
                           ON DELETE CASCADE, -- If the employee is deleted, their shifts are removed
                       FOREIGN KEY (store_id)
                           REFERENCES Store(store_id)
    -- No ON DELETE CASCADE here, as we might want to keep shift history even if the store link is broken (though linking to the Store is redundant if linked to Employee, it's kept for clarity)
);

CREATE TABLE GeneratedSchedule (
                                   generated_schedule_id SERIAL PRIMARY KEY,
                                   store_id INTEGER NOT NULL,
                                   week DATE NOT NULL,
                                   FOREIGN KEY (store_id)
                                       REFERENCES Store(store_id)
                                       ON DELETE CASCADE -- If the store is deleted, its generated schedules are too
);
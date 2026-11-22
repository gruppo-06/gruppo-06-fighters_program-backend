CREATE TABLE subcard (
    id           SMALLSERIAL  PRIMARY KEY,
    code         VARCHAR(10)  NOT NULL UNIQUE,
    name         VARCHAR(60)  NOT NULL,
    description  VARCHAR(255),
    atk_percent  SMALLINT     NOT NULL DEFAULT 0 CHECK (atk_percent BETWEEN -50 AND 50),
    def_percent  SMALLINT     NOT NULL DEFAULT 0 CHECK (def_percent BETWEEN -50 AND 50),
    vel_percent  SMALLINT     NOT NULL DEFAULT 0 CHECK (vel_percent BETWEEN -50 AND 50),
    crt_percent  SMALLINT     NOT NULL DEFAULT 0 CHECK (crt_percent BETWEEN -50 AND 50)
);

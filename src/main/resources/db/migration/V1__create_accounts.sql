CREATE TABLE accounts
(
    id       UUID           PRIMARY KEY,
    currency CHAR(3)        NOT NULL,
    balance  NUMERIC(19, 4) NOT NULL CHECK ( balance >= 0 ),
    version  BIGINT         NOT NULL CHECK ( version >= 0 )
);


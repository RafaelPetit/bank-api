CREATE TABLE ledger_transactions
(
    id          UUID PRIMARY KEY,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ledger_entries
(
    transaction_id UUID           NOT NULL REFERENCES ledger_transactions (id),
    position       SMALLINT       NOT NULL,
    account_id     UUID           NOT NULL,
    direction      VARCHAR(6)     NOT NULL CHECK ( direction IN ('DEBIT', 'CREDIT')),
    amount         NUMERIC(19, 4) NOT NULL CHECK ( amount > 0 ),
    currency       CHAR(3)        NOT NULL,
    PRIMARY KEY (transaction_id, position)
);

CREATE INDEX ledger_entries_account_id_idx ON ledger_entries (account_id);

CREATE FUNCTION reject_ledger_change() RETURNS trigger AS
$$
BEGIN
    RAISE EXCEPTION 'O ledger é append-only: % em % não é permitido', TG_OP, TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER ledger_transactions_append_only
    BEFORE UPDATE OR DELETE ON ledger_transactions
    FOR EACH ROW EXECUTE FUNCTION reject_ledger_change();

CREATE TRIGGER ledger_entries_append_only
    BEFORE UPDATE OR DELETE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION reject_ledger_change();

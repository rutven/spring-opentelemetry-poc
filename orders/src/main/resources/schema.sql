CREATE TABLE IF NOT EXISTS orders (
    id         BIGSERIAL PRIMARY KEY,
    item       VARCHAR(100) NOT NULL,
    quantity   INT          NOT NULL,
    user_id    VARCHAR(100),
    price      NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL
);

ALTER TABLE books
    ADD COLUMN IF NOT EXISTS deleted boolean NOT NULL DEFAULT false;

ALTER TABLE loans
    ADD COLUMN IF NOT EXISTS renewals_used integer NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS book_reservations (
    id bigserial PRIMARY KEY,
    book_id bigint NOT NULL REFERENCES books (id),
    user_id bigint NOT NULL REFERENCES library_users (id),
    created_at timestamp(6) without time zone NOT NULL,
    status varchar(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_book_reservations_queue
    ON book_reservations (book_id, status, created_at, id);

CREATE INDEX IF NOT EXISTS idx_book_reservations_user
    ON book_reservations (user_id, created_at);

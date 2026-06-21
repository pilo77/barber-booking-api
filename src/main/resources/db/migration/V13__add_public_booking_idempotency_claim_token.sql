ALTER TABLE public_booking_idempotency_keys
    ADD COLUMN claim_token VARCHAR(64);

UPDATE public_booking_idempotency_keys
SET claim_token = md5(
    company_id::text
    || ':' || branch_id::text
    || ':' || idempotency_key
    || ':' || COALESCE(created_at::text, '')
    || ':' || random()::text
)
WHERE claim_token IS NULL;

ALTER TABLE public_booking_idempotency_keys
    ALTER COLUMN claim_token SET NOT NULL;

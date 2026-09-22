ALTER TABLE reward_redemptions
    DROP COLUMN vnd_per_star;

ALTER TABLE reward_redemptions
    ADD COLUMN stars_per_redemption INTEGER NOT NULL DEFAULT 10,
    ADD COLUMN value_per_redemption BIGINT NOT NULL DEFAULT 50000;

ALTER TABLE reward_redemptions
    ADD CONSTRAINT ck_redemption_stars_per_unit
        CHECK (stars_per_redemption > 0),
    ADD CONSTRAINT ck_redemption_value_per_unit
        CHECK (value_per_redemption > 0),
    ADD CONSTRAINT ck_redemption_full_units
        CHECK (stars_redeemed % stars_per_redemption = 0),
    ADD CONSTRAINT ck_redemption_amount
        CHECK (
            amount_vnd =
            (stars_redeemed / stars_per_redemption) * value_per_redemption
        );
-- Fix answer_votes.user_id FK: it was pointing to regular_users, which prevented business accounts from voting.

ALTER TABLE answer_votes
    DROP FOREIGN KEY FKeovt71gx6r5ts4eyxde7eguqq;

ALTER TABLE answer_votes
    ADD CONSTRAINT FKeovt71gx6r5ts4eyxde7eguqq
        FOREIGN KEY (user_id) REFERENCES appusers (id);
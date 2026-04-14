-- Fix answers.user_id FK: was pointing to regular_users, now points to appusers
-- so that business accounts can also post answers.

ALTER TABLE answers
    DROP FOREIGN KEY FKd69sr7tl197vsdwfatvaw3mxy;

ALTER TABLE answers
    ADD CONSTRAINT FKd69sr7tl197vsdwfatvaw3mxy
        FOREIGN KEY (user_id) REFERENCES appusers (id);

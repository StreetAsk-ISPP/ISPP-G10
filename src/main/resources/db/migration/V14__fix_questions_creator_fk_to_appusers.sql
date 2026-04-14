-- Fix questions.creator_id FK: was pointing to regular_users, now points to appusers
-- so that business accounts can also create questions.

ALTER TABLE questions
    DROP FOREIGN KEY FKa79qkldsqx3sntac53gvte9q5;

ALTER TABLE questions
    ADD CONSTRAINT FKa79qkldsqx3sntac53gvte9q5
        FOREIGN KEY (creator_id) REFERENCES appusers (id);

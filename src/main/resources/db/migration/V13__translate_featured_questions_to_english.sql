-- Translate permanent featured questions to English.

UPDATE questions
SET
    title = 'What is the best night plan in Seville? 🍻',
    content = 'Cocktail bars, rooftops, flamenco shows... what is Seville nightlife really like?'
WHERE id = UUID_TO_BIN('dd000000-0000-0000-0000-000000000001');

UPDATE questions
SET
    title = 'Betis or Sevilla? 😏',
    content = 'You are standing in front of Villamarin. There is no middle ground here. Which side are you on?'
WHERE id = UUID_TO_BIN('dd000000-0000-0000-0000-000000000002');

UPDATE questions
SET
    title = 'Do you have a crush on someone at Reina? 😳',
    content = 'Come on, confess... is there someone on campus who makes you nervous every time you see them? 👀'
WHERE id = UUID_TO_BIN('dd000000-0000-0000-0000-000000000003');

UPDATE questions
SET
    title = 'Who is going out tonight? 🎉',
    content = 'Do you have plans tonight or are you staying home? Come on, share with us...'
WHERE id = UUID_TO_BIN('dd000000-0000-0000-0000-000000000004');

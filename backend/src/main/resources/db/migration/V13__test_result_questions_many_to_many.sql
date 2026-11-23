-- TestResultEntity.correctQuestions / incorrectQuestions were @OneToMany, which put a unique key on the
-- question column: the second student to submit the same teacher test failed with a duplicate key.
-- They are @ManyToMany now. Replace each unique key with a plain index (the foreign keys need one).
-- Guarded, so it also runs on a database whose keys have other names or are already gone.

SET @stmt = (SELECT IF(COUNT(*) > 0,
    'ALTER TABLE test_result_entity_correct_questions ADD INDEX idx_trcq_question (correct_questions_id), DROP INDEX UKdmhh085sff9n1070uddaqk2eg',
    'DO 0')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'test_result_entity_correct_questions'
      AND index_name = 'UKdmhh085sff9n1070uddaqk2eg');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @stmt = (SELECT IF(COUNT(*) > 0,
    'ALTER TABLE test_result_entity_incorrect_questions ADD INDEX idx_triq_question (incorrect_questions_id), DROP INDEX UKb3ym9o1f39dtp9kag00r6lvq1',
    'DO 0')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'test_result_entity_incorrect_questions'
      AND index_name = 'UKb3ym9o1f39dtp9kag00r6lvq1');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

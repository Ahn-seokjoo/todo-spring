ALTER TABLE `todo`
    DROP FOREIGN KEY `FK4ggwae0141n58mw6oc6x4ner`,
    DROP KEY `FK4ggwae0141n58mw6oc6x4ner`,
    ADD CONSTRAINT `fk_todo_owner` FOREIGN KEY (`owner_todo_user_id`) REFERENCES `todo_user` (`todo_user_id`);

ALTER TABLE `todo_category`
    DROP FOREIGN KEY `FKcyvi5ouyhwmfqnxrpvqh4xfi3`,
    DROP KEY `FKcyvi5ouyhwmfqnxrpvqh4xfi3`,
    DROP FOREIGN KEY `FKi3073x7gl07du43tlbwdqgaiv`,
    DROP KEY `FKi3073x7gl07du43tlbwdqgaiv`,
    ADD CONSTRAINT `fk_todo_category_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`category_id`),
    ADD CONSTRAINT `fk_todo_category_todo` FOREIGN KEY (`todo_id`) REFERENCES `todo` (`todo_id`);

ALTER TABLE `category`
    RENAME KEY `UK46ccwnsi9409t36lurvtyljak` TO `uk_category_name`;

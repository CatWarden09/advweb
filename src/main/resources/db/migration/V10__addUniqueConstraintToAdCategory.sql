ALTER TABLE advertisement_categories
    ADD CONSTRAINT uk_parent_name UNIQUE (parent_id, name);
CREATE UNIQUE INDEX uk_root_category_name
    ON advertisement_categories(name)
    WHERE parent_id IS NULL; -- case for root categories, where parent is always null. If only first constraint is used, then same names for root categories will be allowed
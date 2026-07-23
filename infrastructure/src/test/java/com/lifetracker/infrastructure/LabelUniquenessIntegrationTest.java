package com.lifetracker.infrastructure;

import com.lifetracker.domain.labeling.Label;
import com.lifetracker.domain.labeling.LabelId;
import com.lifetracker.domain.labeling.LabelName;
import com.lifetracker.domain.labeling.LabelRepository;
import com.lifetracker.domain.ledger.OwnerId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Sibling-name uniqueness at the DATABASE level (migration 010), against real Postgres.
 *
 * <p>The application already refuses a duplicate sibling in {@link com.lifetracker.domain.labeling.LabelTree},
 * so these tests save straight through the {@link LabelRepository} port — bypassing that check on
 * purpose — to prove the two partial unique indexes are a genuine backstop rather than a rule the app
 * merely promises to keep.
 *
 * <p>The root case is why the indexes are split in two. A naive {@code UNIQUE (owner_id, parent_id,
 * lower(name))} would <em>silently</em> accept two roots both named {@code food}, because in SQL a NULL
 * is never equal to another NULL — a parent_id of NULL never collides. {@code uq_labels_root_name}
 * (partial, {@code where parent_id is null}) exists solely to close that, and this pins it.
 */
class LabelUniquenessIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    LabelRepository labels;

    private static OwnerId owner() {
        return OwnerId.of(UUID.randomUUID());
    }

    private static Label root(String name) {
        return Label.root(LabelId.generate(), new LabelName(name));
    }

    private static Label under(Label parent, String name) {
        return Label.under(LabelId.generate(), new LabelName(name), parent.id());
    }

    @Test
    void the_database_refuses_two_root_labels_with_the_same_name_ignoring_case() {
        OwnerId owner = owner();
        labels.save(owner, root("food"));

        assertThrows(DataIntegrityViolationException.class,
                () -> labels.save(owner, root("FOOD")),
                "the NULL-parent trap: two roots named the same must be refused by uq_labels_root_name");
    }

    @Test
    void the_database_refuses_two_children_of_one_parent_with_the_same_name_ignoring_case() {
        OwnerId owner = owner();
        Label food = root("food");
        labels.save(owner, food);
        labels.save(owner, under(food, "snacks"));

        assertThrows(DataIntegrityViolationException.class,
                () -> labels.save(owner, under(food, "Snacks")),
                "uq_labels_child_name refuses duplicate siblings under one parent");
    }

    @Test
    void the_same_root_name_is_allowed_in_a_different_book() {
        OwnerId a = owner();
        OwnerId b = owner();
        labels.save(a, root("food"));

        assertDoesNotThrow(() -> labels.save(b, root("food")),
                "uniqueness is scoped by owner_id — one Book's tree never collides with another's");
    }

    @Test
    void the_same_name_is_allowed_under_two_different_parents() {
        OwnerId owner = owner();
        Label food = root("food");
        Label transport = root("transport");
        labels.save(owner, food);
        labels.save(owner, transport);
        labels.save(owner, under(food, "other"));

        assertDoesNotThrow(() -> labels.save(owner, under(transport, "other")),
                "reusing a name under a different parent is exactly what the tree is for");
    }
}

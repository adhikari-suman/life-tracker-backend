package com.lifetracker.application.labeling;

import com.lifetracker.domain.account.AccountKind;
import com.lifetracker.domain.labeling.Label;
import com.lifetracker.domain.labeling.LabelHasChildrenException;
import com.lifetracker.domain.labeling.LabelId;
import com.lifetracker.domain.labeling.LabelInUseException;
import com.lifetracker.domain.ledger.OwnerId;
import com.lifetracker.domain.transaction.PostingId;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The label lifecycle (ADR-0015) and the rule about which postings can carry one (ADR-0014). */
class LabelUseCasesTest {

    private final InMemoryLabelRepository labels = new InMemoryLabelRepository();
    private final InMemoryPostingLabelRepository postingLabels = new InMemoryPostingLabelRepository();
    private final InMemoryPostingKinds postingKinds = new InMemoryPostingKinds();

    private final CreateLabel create = new CreateLabel(labels);
    private final UpdateLabel update = new UpdateLabel(labels);
    private final DeleteLabel delete = new DeleteLabel(labels, postingLabels);
    private final AssignPostingLabel assign = new AssignPostingLabel(labels, postingLabels, postingKinds);
    private final ClearPostingLabel clear = new ClearPostingLabel(postingLabels, postingKinds);

    private final OwnerId owner = OwnerId.of(UUID.randomUUID());
    private final OwnerId other = OwnerId.of(UUID.randomUUID());

    private LabelId create(String name, LabelId parent) {
        return create.execute(new CreateLabelCommand(owner, name, parent == null ? null : parent.value()));
    }

    private PostingId posting(AccountKind kind) {
        PostingId id = PostingId.generate();
        postingKinds.put(owner, id, kind);
        return id;
    }

    // ---------- Creating ----------

    @Test
    void creates_a_root_and_a_child() {
        LabelId food = create("food", null);
        LabelId fastFood = create("fast food", food);

        assertTrue(labels.findById(owner, food).orElseThrow().isRoot());
        assertEquals(Optional.of(food), labels.findById(owner, fastFood).orElseThrow().parent());
    }

    @Test
    void refuses_a_parent_that_is_not_in_this_book() {
        assertThrows(LabelNotFoundException.class,
                () -> create.execute(new CreateLabelCommand(owner, "orphan", UUID.randomUUID())));
    }

    @Test
    void one_owners_labels_are_invisible_to_another() {
        create("food", null);
        assertTrue(labels.findAllByOwner(other).isEmpty());
    }

    // ---------- Renaming, reparenting, archiving ----------

    @Test
    void renames_without_moving() {
        LabelId food = create("food", null);
        LabelId fastFood = create("fast food", food);

        update.execute(new UpdateLabelCommand(owner, fastFood.value(), "takeaway", false, null, null));

        Label updated = labels.findById(owner, fastFood).orElseThrow();
        assertEquals("takeaway", updated.name().value());
        assertEquals(Optional.of(food), updated.parent(), "an omitted parent must leave the parent alone");
    }

    @Test
    void an_explicit_null_parent_moves_a_label_to_the_root() {
        LabelId food = create("food", null);
        LabelId fastFood = create("fast food", food);

        // reparent=true with a null target is "move to the root" -- distinct from omitting it entirely.
        update.execute(new UpdateLabelCommand(owner, fastFood.value(), null, true, null, null));

        assertTrue(labels.findById(owner, fastFood).orElseThrow().isRoot());
    }

    @Test
    void archiving_retires_a_label_without_disturbing_it() {
        LabelId wedding = create("wedding", null);

        update.execute(new UpdateLabelCommand(owner, wedding.value(), null, false, null, true));
        assertTrue(labels.findById(owner, wedding).orElseThrow().isArchived());

        update.execute(new UpdateLabelCommand(owner, wedding.value(), null, false, null, false));
        assertFalse(labels.findById(owner, wedding).orElseThrow().isArchived());
    }

    @Test
    void an_archived_label_cannot_be_newly_applied() {
        LabelId wedding = create("wedding", null);
        update.execute(new UpdateLabelCommand(owner, wedding.value(), null, false, null, true));
        PostingId expense = posting(AccountKind.EXPENSE);

        assertThrows(LabelArchivedException.class, () -> assign.execute(owner, expense, wedding));
    }

    // ---------- Deleting ----------

    @Test
    void deletes_a_label_that_nothing_depends_on() {
        LabelId food = create("food", null);

        delete.execute(owner, food);

        assertTrue(labels.findById(owner, food).isEmpty());
    }

    @Test
    void refuses_to_delete_a_label_with_children() {
        LabelId food = create("food", null);
        create("fast food", food);

        assertThrows(LabelHasChildrenException.class, () -> delete.execute(owner, food));
    }

    @Test
    void refuses_to_delete_a_label_still_tagged_on_a_posting() {
        LabelId food = create("food", null);
        assign.execute(owner, posting(AccountKind.EXPENSE), food);

        assertThrows(LabelInUseException.class, () -> delete.execute(owner, food),
                "deleting is tidying, not destroying history -- archive it instead");
    }

    // ---------- Attaching ----------

    @Test
    void tags_an_expense_or_income_posting() {
        LabelId food = create("food", null);
        PostingId expense = posting(AccountKind.EXPENSE);
        PostingId income = posting(AccountKind.INCOME);

        assign.execute(owner, expense, food);
        assign.execute(owner, income, food);

        assertEquals(Optional.of(food), postingLabels.findByPosting(owner, expense));
        assertEquals(Optional.of(food), postingLabels.findByPosting(owner, income));
    }

    @Test
    void refuses_to_tag_a_posting_to_an_account_you_hold() {
        LabelId food = create("food", null);

        for (AccountKind kind : new AccountKind[]{AccountKind.ASSET, AccountKind.LIABILITY, AccountKind.EQUITY}) {
            PostingId held = posting(kind);
            assertThrows(LabelNotApplicableException.class, () -> assign.execute(owner, held, food),
                    kind + " records money moving between accounts you hold, so there is nothing to categorize");
        }
    }

    @Test
    void retagging_replaces_the_previous_label() {
        LabelId food = create("food", null);
        LabelId transport = create("transport", null);
        PostingId expense = posting(AccountKind.EXPENSE);

        assign.execute(owner, expense, food);
        assign.execute(owner, expense, transport);

        assertEquals(Optional.of(transport), postingLabels.findByPosting(owner, expense));
        assertEquals(1, postingLabels.size(), "a posting carries at most one label");
        assertFalse(postingLabels.isInUse(owner, food), "the old label is free to delete again");
    }

    @Test
    void clearing_leaves_the_posting_uncategorized_and_is_idempotent() {
        LabelId food = create("food", null);
        PostingId expense = posting(AccountKind.EXPENSE);
        assign.execute(owner, expense, food);

        clear.execute(owner, expense);
        clear.execute(owner, expense);

        assertEquals(Optional.empty(), postingLabels.findByPosting(owner, expense));
    }

    @Test
    void tagging_an_unknown_posting_is_refused() {
        LabelId food = create("food", null);

        assertThrows(PostingNotFoundException.class, () -> assign.execute(owner, PostingId.generate(), food));
    }
}

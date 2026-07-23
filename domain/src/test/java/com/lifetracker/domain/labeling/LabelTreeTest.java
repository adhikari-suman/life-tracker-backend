package com.lifetracker.domain.labeling;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rules that span more than one label: depth, cycles, sibling names, and the roll-up chain. */
class LabelTreeTest {

    private final List<Label> labels = new ArrayList<>();

    private Label root(String name) {
        Label label = Label.root(LabelId.generate(), new LabelName(name));
        labels.add(label);
        return label;
    }

    private Label under(Label parent, String name) {
        Label label = Label.under(LabelId.generate(), new LabelName(name), parent.id());
        labels.add(label);
        return label;
    }

    private LabelTree tree() {
        return new LabelTree(labels);
    }

    // ---------- Depth ----------

    @Test
    void depth_is_one_based_from_the_root() {
        Label food = root("food");
        Label restaurants = under(food, "restaurants");
        Label fastFood = under(restaurants, "fast food");

        LabelTree tree = tree();
        assertEquals(1, tree.depthOf(food.id()));
        assertEquals(2, tree.depthOf(restaurants.id()));
        assertEquals(3, tree.depthOf(fastFood.id()));
    }

    @Test
    void a_fourth_level_is_refused() {
        Label food = root("food");
        Label restaurants = under(food, "restaurants");
        Label fastFood = under(restaurants, "fast food");

        assertThrows(LabelDepthExceededException.class,
                () -> tree().requireCanAdd(Optional.of(fastFood.id()), new LabelName("burgers")));
    }

    @Test
    void three_levels_are_allowed() {
        Label food = root("food");
        Label restaurants = under(food, "restaurants");

        assertDoesNotThrow(() -> tree().requireCanAdd(Optional.of(restaurants.id()), new LabelName("fast food")));
    }

    /**
     * The bug this exists to catch: checking only the label being moved passes, because the label
     * itself would land at a legal depth. It is the DESCENDANTS that break the cap.
     */
    @Test
    void moving_a_subtree_checks_the_whole_subtree_not_just_the_moved_label() {
        Label food = root("food");
        Label restaurants = under(food, "restaurants");   // depth 2, so anything under it may go to 3

        Label travel = root("travel");
        Label flights = under(travel, "flights");         // a two-deep subtree: height 2

        // Moving `travel` (height 2) under `restaurants` (depth 2) would put `flights` at depth 4.
        assertThrows(LabelDepthExceededException.class,
                () -> tree().requireCanMove(travel.id(), Optional.of(restaurants.id()), travel.name()));

        // The moved label alone would have been fine at depth 3 -- proving the check is subtree-aware.
        assertEquals(2, tree().heightOf(travel.id()));
        assertEquals(1, tree().heightOf(flights.id()));
    }

    @Test
    void moving_a_leaf_deeper_is_allowed_while_it_fits() {
        Label food = root("food");
        Label restaurants = under(food, "restaurants");
        Label snacks = root("snacks");

        assertDoesNotThrow(() -> tree().requireCanMove(snacks.id(), Optional.of(restaurants.id()), snacks.name()));
    }

    // ---------- Cycles ----------

    @Test
    void a_label_cannot_move_under_its_own_descendant() {
        Label food = root("food");
        Label restaurants = under(food, "restaurants");

        assertThrows(LabelCycleException.class,
                () -> tree().requireCanMove(food.id(), Optional.of(restaurants.id()), food.name()));
    }

    @Test
    void a_label_cannot_move_under_itself() {
        Label food = root("food");

        assertThrows(LabelCycleException.class,
                () -> tree().requireCanMove(food.id(), Optional.of(food.id()), food.name()));
    }

    // ---------- Sibling names ----------

    @Test
    void siblings_cannot_share_a_name_ignoring_case() {
        Label food = root("food");
        under(food, "Fast Food");

        assertThrows(DuplicateLabelNameException.class,
                () -> tree().requireCanAdd(Optional.of(food.id()), new LabelName("fast food")));
    }

    /**
     * The root case is its own test on purpose. In Postgres a NULL never equals another NULL, so a
     * naive UNIQUE (owner_id, parent_id, lower(name)) silently permits two roots both named 'food' —
     * the database half of this rule needs a separate partial index, and this is the domain half.
     */
    @Test
    void two_roots_cannot_share_a_name_either() {
        root("food");

        assertThrows(DuplicateLabelNameException.class,
                () -> tree().requireCanAdd(Optional.empty(), new LabelName("FOOD")));
    }

    @Test
    void the_same_name_may_live_under_two_different_parents() {
        Label food = root("food");
        Label transport = root("transport");
        under(food, "other");

        assertDoesNotThrow(() -> tree().requireCanAdd(Optional.of(transport.id()), new LabelName("other")));
    }

    @Test
    void renaming_a_label_does_not_collide_with_itself() {
        Label food = root("food");
        Label fastFood = under(food, "fast food");

        assertDoesNotThrow(() -> tree().requireCanRename(fastFood.id(), new LabelName("Fast Food")));
    }

    @Test
    void renaming_onto_a_siblings_name_is_refused() {
        Label food = root("food");
        under(food, "groceries");
        Label fastFood = under(food, "fast food");

        assertThrows(DuplicateLabelNameException.class,
                () -> tree().requireCanRename(fastFood.id(), new LabelName("Groceries")));
    }

    // ---------- Paths and the roll-up chain ----------

    @Test
    void the_path_reads_from_the_root_down() {
        Label food = root("food");
        Label restaurants = under(food, "restaurants");
        Label fastFood = under(restaurants, "fast food");

        assertEquals("food / restaurants / fast food", tree().pathOf(fastFood.id()));
        assertEquals("food", tree().pathOf(food.id()));
    }

    @Test
    void the_roll_up_chain_is_the_label_and_every_ancestor() {
        Label food = root("food");
        Label restaurants = under(food, "restaurants");
        Label fastFood = under(restaurants, "fast food");

        List<LabelId> chain = tree().selfAndAncestorsOf(fastFood.id());

        assertEquals(List.of(fastFood.id(), restaurants.id(), food.id()), chain);
        assertTrue(tree().isSelfOrDescendantOf(fastFood.id(), food.id()));
        assertTrue(tree().hasChildren(food.id()));
    }
}

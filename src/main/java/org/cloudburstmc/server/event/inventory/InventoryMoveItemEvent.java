package org.cloudburstmc.server.event.inventory;

import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.inventory.Inventory;
import org.cloudburstmc.server.inventory.InventoryHolder;
import org.cloudburstmc.server.item.behavior.Item;

/**
 * @author CreeperFace
 * <p>
 * Called when inventory transaction is not caused by a player
 */
public class InventoryMoveItemEvent extends InventoryEvent implements Cancellable {

    private final Inventory targetInventory;
    private final InventoryHolder source;

    private Item item;

    private final Action action;

    public InventoryMoveItemEvent(Inventory from, Inventory targetInventory, InventoryHolder source, Item item, Action action) {
        super(from);
        this.targetInventory = targetInventory;
        this.source = source;
        this.item = item;
        this.action = action;
    }

    public Inventory getTargetInventory() {
        return targetInventory;
    }

    public InventoryHolder getSource() {
        return source;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Action getAction() {
        return action;
    }

    public enum Action {
        SLOT_CHANGE, //transaction between 2 inventories
        PICKUP,
        DROP,
        DISPENSE
    }
}
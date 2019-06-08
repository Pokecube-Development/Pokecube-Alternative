package pokecube.alternative.container.belt;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ItemStack;

public class ContainerPlayerPokemon extends Container
{
    /** The crafting matrix inventory. */
    public InventoryPokemon pokemon;
    /** Determines if inventory manipulation should be handled. */
    public boolean          isLocalWorld;
    final PlayerEntity      thePlayer;

    public ContainerPlayerPokemon(InventoryPlayer playerInv, boolean par2, PlayerEntity player)
    {
        this.isLocalWorld = par2;
        this.thePlayer = player;
        pokemon = new InventoryPokemon(player);
        pokemon.setEventHandler(this);
        for (int c = 0; c < 6; c++)
        {
            this.addSlotToContainer(new SlotPokemon(pokemon, c, 8 - 32, 7 + 47 + c * 18));
        }
    }

    /** Callback for when the crafting matrix is changed. */
    @Override
    public void onCraftMatrixChanged(IInventory par1IInventory)
    {
    }

    /** Called when the container is closed. */
    @Override
    public void onContainerClosed(PlayerEntity player)
    {
        super.onContainerClosed(player);
        if (!player.world.isRemote)
        {
            pokemon.closeInventory(player);
        }
    }

    @Override
    public boolean canInteractWith(PlayerEntity par1PlayerEntity)
    {
        return true;
    }

    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickTypeIn, PlayerEntity player)
    {
        return super.slotClick(slotId, dragType, clickTypeIn, player);
    }

    /** Called when a player shift-clicks on a slot. You must override this or
     * you will crash when someone does that. */
    @Override
    public ItemStack transferStackInSlot(PlayerEntity par1PlayerEntity, int par2)
    {
        Slot slot = this.inventorySlots.get(par2);
        return slot.getStack();
    }

    @Override
    public boolean canMergeSlot(ItemStack par1ItemStack, Slot par2Slot)
    {
        return super.canMergeSlot(par1ItemStack, par2Slot);
    }

    public ItemStack getStackInSlot(int index)
    {
        return pokemon.getStackInSlot(index);
    }

}

package pokecube.alternative.container.card;

import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import thut.core.common.handlers.PlayerDataHandler.PlayerData;
import thut.lib.CompatWrapper;

public class CardPlayerData extends PlayerData
{
    public final InventoryBasic inventory = new InventoryBasic("pokecube-alternative.bag", false, 8);

    public CardPlayerData()
    {
    }

    @Override
    public String getIdentifier()
    {
        return "pokealternative-card";
    }

    @Override
    public String dataFileName()
    {
        return "TrainerCard";
    }

    @Override
    public boolean shouldSync()
    {
        return true;
    }

    @Override
    public void writeToNBT(CompoundNBT nbt)
    {
        for (int n = 0; n < inventory.getSizeInventory(); n++)
        {
            ItemStack i = inventory.getStackInSlot(n);
            if (CompatWrapper.isValid(i))
            {
                CompoundNBT tag = new CompoundNBT();
                i.writeToNBT(tag);
                nbt.put("slot" + n, tag);
            }
        }
    }

    @Override
    public void readFromNBT(CompoundNBT nbt)
    {
        CompoundNBT compound = nbt;
        for (int n = 0; n < inventory.getSizeInventory(); n++)
        {
            INBT temp = compound.getTag("slot" + n);
            if (temp instanceof CompoundNBT)
            {
                CompoundNBT tag = (CompoundNBT) temp;
                inventory.setInventorySlotContents(n, new ItemStack(tag));
            }
        }
    }

}

package pokecube.alternative.network;

import javax.xml.ws.handler.MessageContext;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import pokecube.adventures.entity.helper.capabilities.CapabilityHasPokemobs;
import pokecube.alternative.PokecubeAlternative;
import pokecube.alternative.container.belt.BeltPlayerData;

public class PacketSyncBelt implements IMessage, IMessageHandler<PacketSyncBelt, IMessage>
{

    int            playerId;
    BeltPlayerData belt = new BeltPlayerData();

    public PacketSyncBelt()
    {
    }

    public PacketSyncBelt(BeltPlayerData belt, int playerId)
    {
        this.belt = belt;
        this.playerId = playerId;
    }

    @Override
    public void toBytes(ByteBuf buffer)
    {
        buffer.writeInt(playerId);
        CompoundNBT nbt = new CompoundNBT();
        belt.writeToNBT(nbt);
        ByteBufUtils.writeTag(buffer, nbt);
    }

    @Override
    public void fromBytes(ByteBuf buffer)
    {
        playerId = buffer.readInt();
        CompoundNBT nbt = ByteBufUtils.readTag(buffer);
        if (nbt != null) belt.readFromNBT(nbt);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public IMessage onMessage(final PacketSyncBelt message, MessageContext ctx)
    {
        Minecraft.getInstance().addScheduledTask(new Runnable()
        {
            @Override
            public void run()
            {
                processMessage(message);
            }
        });
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    void processMessage(PacketSyncBelt message)
    {
        World world = PokecubeAlternative.proxy.getClientWorld();
        if (world == null) return;
        Entity p = world.getEntityByID(message.playerId);
        if (p != null && p instanceof PlayerEntity)
        {
            BeltPlayerData cap = BeltPlayerData.getBelt(p);
            BeltPlayerData capData = (BeltPlayerData) p.getCapability(CapabilityHasPokemobs.HASPOKEMOBS_CAP, null);
            capData.wrapper.setData(cap);
            for (int i = 0; i < 6; i++)
            {
                cap.setCube(i, message.belt.getCube(i));
                cap.setSlotID(i, message.belt.getSlotID(i));
            }
            cap.setSlot(message.belt.getSlot());
            cap.setType(message.belt.getType());
            cap.setCanMegaEvolve(message.belt.canMegaEvolve());
            cap.setGender(message.belt.getGender());
        }
        return;
    }

}

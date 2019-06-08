package pokecube.alternative.network;

import java.util.UUID;

import javax.xml.ws.handler.MessageContext;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import pokecube.alternative.PokecubeAlternative;
import pokecube.alternative.container.belt.BeltPlayerData;
import pokecube.core.events.handlers.PCEventsHandler;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.core.items.pokecubes.PokecubeManager;
import thut.lib.CompatWrapper;

public class PacketKeyUse implements IMessage, IMessageHandler<PacketKeyUse, IMessage>
{
    public static final byte SLOTUP   = 0;
    public static final byte SLOTDOWN = 1;
    public static final byte SENDOUT  = 2;
    public static final byte RECALL   = 3;
    public static final byte OPENCARD = 4;
    public static final byte USEITEM  = 5;

    byte                     messageId;
    int                      ticks    = 0;

    public PacketKeyUse()
    {
    }

    public PacketKeyUse(byte message)
    {
        this.messageId = message;
    }

    public PacketKeyUse(byte message, int ticks)
    {
        this.messageId = message;
        this.ticks = ticks;
    }

    @Override
    public void toBytes(ByteBuf buffer)
    {
        buffer.writeByte(messageId);
        buffer.writeInt(ticks);
    }

    @Override
    public void fromBytes(ByteBuf buffer)
    {
        messageId = buffer.readByte();
        ticks = buffer.readInt();
    }

    @Override
    public IMessage onMessage(final PacketKeyUse message, final MessageContext ctx)
    {
        FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(new Runnable()
        {
            @Override
            public void run()
            {
                processMessage(ctx.getServerHandler().player, message);
            }
        });
        return null;
    }

    void processMessage(PlayerEntity player, PacketKeyUse message)
    {
        BeltPlayerData cap = BeltPlayerData.getBelt(player);
        if (message.messageId == OPENCARD)
        {
            player.openGui(PokecubeAlternative.instance, 0, player.getEntityWorld(), 0, 0, 0);
            return;
        }
        if (message.messageId == USEITEM)
        {
            int id = message.ticks;
            Entity entity = player.getEntityWorld().getEntityByID(id);
            if (entity instanceof LivingEntity) player.interactOn(entity, Hand.MAIN_HAND);
            return;
        }
        if (message.messageId == SENDOUT)
        {
            ItemStack cube = cap.getCube(cap.getSlot());
            if (CompatWrapper.isValid(cube))
            {
                cube.getItem().onPlayerStoppedUsing(cube, player.world, player, message.ticks);
                cube.setCount(1);
                cap.setCube(cap.getSlot(), ItemStack.EMPTY);
                UUID id = PokecubeManager.getUUID(cube);
                cap.setSlotID(cap.getSlot(), id);
            }
        }
        else if (message.messageId == RECALL)
        {
            int id = message.ticks;
            if (id == -1)
            {
                PCEventsHandler.recallAllPokemobs(player);
            }
            else
            {
                Entity mob = player.world.getEntityByID(id);
                IPokemob pokemob = CapabilityPokemob.getPokemobFor(mob);
                if (pokemob != null)
                {
                    pokemob.returnToPokecube();
                }
                else
                {
                    PCEventsHandler.recallAllPokemobs(player);
                }
            }
        }
        else if (message.messageId == SLOTUP)
        {
            int currentSlot = cap.getSlot();
            if (currentSlot <= 0)
            {
                cap.setSlot(5);
            }
            else
            {
                cap.setSlot(currentSlot - 1);
            }
        }
        else if (message.messageId == SLOTDOWN)
        {
            int currentSlot = cap.getSlot();
            if (currentSlot >= 5)
            {
                cap.setSlot(0);
            }
            else
            {
                cap.setSlot(currentSlot + 1);
            }
        }
        PacketSyncBelt packet = new PacketSyncBelt(cap, player.getEntityId());
        PacketHandler.INSTANCE.sendToAll(packet);
        return;
    }

}

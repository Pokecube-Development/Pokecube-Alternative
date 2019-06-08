package pokecube.alternative.event;

import java.util.HashSet;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.StartTracking;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import pokecube.adventures.PokecubeAdv;
import pokecube.adventures.entity.helper.capabilities.CapabilityHasPokemobs;
import pokecube.alternative.Config;
import pokecube.alternative.Reference;
import pokecube.alternative.container.belt.BeltPlayerData;
import pokecube.alternative.container.belt.BeltPlayerData.CapWrapper;
import pokecube.alternative.container.belt.IPokemobBelt;
import pokecube.alternative.network.PacketHandler;
import pokecube.alternative.network.PacketSyncBelt;
import pokecube.alternative.network.PacketSyncEnabled;
import pokecube.core.blocks.pc.InventoryPC;
import pokecube.core.events.handlers.PCEventsHandler;
import pokecube.core.events.pokemob.RecallEvent;
import pokecube.core.interfaces.IPokecube;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.core.interfaces.pokemob.ai.CombatStates;
import pokecube.core.items.pokecubes.PokecubeManager;
import thut.api.maths.Vector3;
import thut.lib.CompatWrapper;

public class EventHandlerCommon
{
    final static ResourceLocation POKEMOBSCAP  = new ResourceLocation(PokecubeAdv.ID, "pokemobs");

    static HashSet<UUID>          syncSchedule = new HashSet<UUID>();

    public EventHandlerCommon()
    {
    }

    @SubscribeEvent
    public void PlayerUseItemEvent(PlayerInteractEvent.RightClickItem event)
    {
        ItemStack item = event.getItemStack();
        PlayerEntity player = event.getPlayerEntity();
        if (player.world.isRemote) return;
        int slotIndex = event.getPlayerEntity().inventory.currentItem;
        if (slotIndex != 0)
        {
            if (PokecubeManager.isFilled(item))
            {
                if (FMLCommonHandler.instance().getEffectiveSide() == Dist.DEDICATED_SERVER)
                {
                    UUID itemID = PokecubeManager.getUUID(item);
                    IPokemobBelt cap = BeltPlayerData.getBelt(player);
                    boolean toBelt = false;
                    for (int i = 0; i < 6; i++)
                    {
                        ItemStack stack = cap.getCube(i);
                        if (CompatWrapper.isValid(stack)) continue;
                        if (itemID.equals(cap.getSlotID(i)))
                        {
                            cap.setCube(i, item);
                            player.inventory.setInventorySlotContents(slotIndex, ItemStack.EMPTY);
                            toBelt = true;
                            break;
                        }
                    }
                    if (!toBelt) for (int i = 0; i < 6; i++)
                    {
                        if (!CompatWrapper.isValid(cap.getCube(i)))
                        {
                            cap.setCube(i, item);
                            player.inventory.setInventorySlotContents(slotIndex, ItemStack.EMPTY);
                            syncPokemon(player);
                            toBelt = true;
                            break;
                        }
                    }
                    if (toBelt)
                    {
                        player.sendMessage(new TranslationTextComponent(Reference.MODID + ".pokebelt.tobelt",
                                item.getDisplayName()));
                    }
                    ((ServerPlayerEntity) player).sendAllContents(player.inventoryContainer,
                            player.inventoryContainer.inventoryItemStacks);
                    syncPokemon(player);
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public void PlayerLoggedInEvent(PlayerLoggedInEvent event)
    {
        if (event.player instanceof ServerPlayerEntity) PacketHandler.INSTANCE
                .sendTo(new PacketSyncEnabled(Config.instance.isEnabled), (ServerPlayerEntity) event.player);
        Side side = FMLCommonHandler.instance().getEffectiveSide();
        if (side == Dist.DEDICATED_SERVER)
        {
            EventHandlerCommon.syncSchedule.add(event.player.getUniqueID());
        }
    }

    @SubscribeEvent
    public void attachCap(AttachCapabilitiesEvent<Entity> event)
    {
        if (event.getObject() instanceof PlayerEntity)
        {
            CapWrapper wrapper = new CapWrapper();
            wrapper.setData(new BeltPlayerData());
            event.addCapability(POKEMOBSCAP, wrapper);
        }
    }

    @SubscribeEvent
    public void EntityHurt(LivingHurtEvent event)
    {
        if (Config.instance.autoThrow && event.getMobEntity() instanceof PlayerEntity
                && CapabilityPokemob.getPokemobFor(event.getSource().getImmediateSource()) != null)
        {
            if (PCEventsHandler.getOutMobs(event.getMobEntity()).isEmpty())
            {
                IPokemobBelt cap = BeltPlayerData.getBelt(event.getMobEntity());
                for (int i = 0; i < 6; i++)
                {
                    int index = (cap.getSlot() + i) % 6;
                    if (CompatWrapper.isValid(cap.getCube(index)))
                    {
                        ItemStack cube = cap.getCube(index);
                        if (PokecubeManager.isFilled(cube) && PokecubeManager
                                .itemToPokemob(cube, event.getEntity().getEntityWorld()).getEntity().getHealth() > 0)
                        {
                            Entity target = event.getSource().getImmediateSource();
                            Vector3 here = Vector3.getNewVector().set(event.getMobEntity());
                            Vector3 t = Vector3.getNewVector().set(target);
                            t.set(t.subtractFrom(here).scalarMultBy(0.5).addTo(here));
                            ((IPokecube) cube.getItem()).throwPokecubeAt(target.getEntityWorld(),
                                    event.getMobEntity(), cube, t, null);
                            ITextComponent text = new TranslationTextComponent("pokecube.trainer.toss",
                                    event.getMobEntity().getDisplayName(), cube.getDisplayName());
                            cap.setCube(index, ItemStack.EMPTY);
                            syncPokemon((PlayerEntity) event.getMobEntity());
                            target.sendMessage(text);
                            return;
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void playerTick(PlayerEvent.LivingUpdateEvent event)
    {
        if (event.getMobEntity().world.isRemote) return;
        if (event.getEntity() instanceof PlayerEntity)
        {
            PlayerEntity player = (PlayerEntity) event.getEntity();

            if (!syncSchedule.isEmpty() && syncSchedule.contains(player.getUniqueID()) && player.ticksExisted > 20)
            {
                syncPokemon(player);
                for (PlayerEntity player2 : event.getEntity().world.playerEntities)
                {
                    BeltPlayerData cap = BeltPlayerData.getBelt(player2);
                    PacketHandler.INSTANCE.sendTo(new PacketSyncBelt(cap, player2.getEntityId()),
                            (ServerPlayerEntity) player);
                }
                syncSchedule.remove(player.getUniqueID());
            }
        }
    }

    public static void syncPokemon(PlayerEntity player)
    {
        if (FMLCommonHandler.instance().getEffectiveSide() == Dist.CLIENT)
        {
            Thread.dumpStack();
            return;
        }
        BeltPlayerData cap = BeltPlayerData.getBelt(player);
        BeltPlayerData capData = (BeltPlayerData) player.getCapability(CapabilityHasPokemobs.HASPOKEMOBS_CAP, null);
        capData.wrapper.setData(cap);
        BeltPlayerData.save(player);
        PacketHandler.INSTANCE.sendToAll(new PacketSyncBelt(cap, player.getEntityId()));
    }

    @SubscribeEvent
    public void startTracking(StartTracking event)
    {
        if (event.getTarget() instanceof ServerPlayerEntity && event.getPlayerEntity().isServerWorld())
        {
            BeltPlayerData cap = BeltPlayerData.getBelt(event.getTarget());
            PacketHandler.INSTANCE.sendTo(new PacketSyncBelt(cap, event.getTarget().getEntityId()),
                    (ServerPlayerEntity) event.getPlayerEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void recallPokemon(RecallEvent event)
    {
        IPokemob pokemon = event.recalled;
        if (pokemon == null) return;
        if (pokemon.getPokemonOwner() instanceof PlayerEntity)
        {
            if (pokemon.getCombatState(CombatStates.MEGAFORME))
                pokemon = pokemon.megaEvolve(pokemon.getPokedexEntry().getBaseForme());
            ItemStack pokemonStack = PokecubeManager.pokemobToItem(pokemon);
            PlayerEntity player = (PlayerEntity) pokemon.getPokemonOwner();
            UUID itemID = PokecubeManager.getUUID(pokemonStack);
            IPokemobBelt cap = BeltPlayerData.getBelt(player);
            boolean toBelt = false;
            for (int i = 0; i < 6; i++)
            {
                ItemStack stack = cap.getCube(i);
                if (CompatWrapper.isValid(stack)) continue;
                if (itemID.equals(cap.getSlotID(i)))
                {
                    cap.setCube(i, pokemonStack);
                    toBelt = true;
                    break;
                }
            }
            if (!toBelt) for (int i = 0; i < 6; i++)
            {
                ItemStack stack = cap.getCube(i);
                if (!CompatWrapper.isValid(stack)
                        || PokecubeManager.getUUID(pokemonStack).equals(PokecubeManager.getUUID(stack)))
                {
                    cap.setCube(i, pokemonStack);
                    toBelt = true;
                    break;
                }
            }
            if (toBelt)
            {
                ITextComponent mess = new TranslationTextComponent("pokemob.action.return",
                        pokemon.getPokemonDisplayName().getFormattedText());
                pokemon.displayMessageToOwner(mess);
            }
            else
            {
                InventoryPC.addPokecubeToPC(pokemonStack, player.getEntityWorld());
            }
            syncPokemon(player);
            pokemon.getEntity().setDead();
            pokemon.setPokemonOwner((UUID) null);
            MinecraftForge.EVENT_BUS.post(new AddCube.OnRecall(player, pokemonStack));
            event.setCanceled(true);
        }
    }
}

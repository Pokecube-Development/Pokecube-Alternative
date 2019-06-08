package pokecube.alternative;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import pokecube.alternative.container.card.ContainerCard;
import pokecube.alternative.event.EventHandlerCommon;

public class CommonProxy implements IGuiHandler {

    public void preInit(FMLCommonSetupEvent event) {

    }

    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new EventHandlerCommon());
    }

    public void postInit(FMLPostInitializationEvent event) {

    }

    @Override
    public Object getClientGuiElement(int ID, PlayerEntity player, World world, int x, int y, int z) {
        return null;
    }

    @Override
    public Object getServerGuiElement(int ID, PlayerEntity player, World world, int x, int y, int z) {
        return new ContainerCard(player);
    }

    public World getClientWorld() {
        return null;
    }

}

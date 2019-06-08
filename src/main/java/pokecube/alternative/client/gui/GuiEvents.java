package pokecube.alternative.client.gui;

import java.util.Map;

import com.google.common.collect.Maps;

import net.java.games.input.Mouse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent.Post;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.client.event.GuiScreenEvent.KeyboardInputEvent;
import net.minecraftforge.client.event.GuiScreenEvent.MouseInputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import pokecube.alternative.Config;
import pokecube.alternative.network.PacketHandler;
import pokecube.alternative.network.PacketPokemobGui;

public class GuiEvents
{
    public static Map<String, int[]> whitelistedGuis = Maps.newHashMap();

    static
    {
        whitelistedGuis.put("pokecube.core.client.gui.blocks.GuiHealTable", new int[2]);
        whitelistedGuis.put("pokecube.core.client.gui.blocks.GuiPC", new int[] { 0, 0 });
        whitelistedGuis.put("pokecube.core.client.gui.blocks.GuiTradingTable", new int[2]);
    }

    public GuiEvents()
    {
        MinecraftForge.EVENT_BUS.register(new GuiBattleHandler());
    }

    GuiScreen        lastcontainer;
    GuiPlayerPokemon gui;

    @OnlyIn(value = Dist.CLIENT)
    @SubscribeEvent
    public void guiEvent(GuiScreenEvent event)
    {
        if (!Config.instance.isEnabled) return;
        if (event.getGui() == gui) return;
        String guiClass;
        if (event.getGui() instanceof GuiContainer
                && whitelistedGuis.containsKey(guiClass = event.getGui().getClass().getName()))
        {
            if (event.getGui() != lastcontainer)
            {
                int[] offset = whitelistedGuis.get(event.getGui().getClass().getName());
                gui = new GuiPlayerPokemon(Minecraft.getInstance().player, event.getGui());
                lastcontainer = event.getGui();
                gui.initToOther(lastcontainer);
                gui.guiTop += offset[0];
                gui.guiLeft += offset[1];
            }
            if (event instanceof InitGuiEvent.Post)
            {

            }
            if (event instanceof DrawScreenEvent.Post)
            {
                DrawScreenEvent.Post evt = (Post) event;
                try
                {
                    gui.drawScreen(evt.getMouseX(), evt.getMouseY(), evt.getRenderPartialTicks());
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            if (event instanceof ActionPerformedEvent.Post)
            {
                if (guiClass.equals("pokecube.core.client.gui.blocks.GuiHealTable"))
                {
                    PacketPokemobGui packet = new PacketPokemobGui();
                    packet.data.putBoolean("H", true);
                    PacketHandler.INSTANCE.sendToServer(packet);
                }
            }
            if (event instanceof MouseInputEvent.Post)
            {
                try
                {
                    if (Mouse.getEventButtonState())
                    {
                        int i = Mouse.getEventX() * gui.width / gui.mc.displayWidth;
                        int j = gui.height - Mouse.getEventY() * gui.height / gui.mc.displayHeight - 1;
                        int k = Mouse.getEventButton();
                        gui.mouseClicked(i, j, k);
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            if (event instanceof KeyboardInputEvent.Post)
            {
                try
                {
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

}

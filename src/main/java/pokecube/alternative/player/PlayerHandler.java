package pokecube.alternative.player;

import java.util.HashMap;

import net.minecraft.entity.player.PlayerEntity;
import pokecube.alternative.container.belt.InventoryPokemon;

public class PlayerHandler
{

    private static HashMap<String, InventoryPokemon> playerPokemon = new HashMap<String, InventoryPokemon>();

    public static void clearPlayerPokemon(PlayerEntity player)
    {
        playerPokemon.remove(player.getDisplayNameString());
    }

    public static InventoryPokemon getPlayerPokemon(PlayerEntity player)
    {
        return new InventoryPokemon(player);
    }
}

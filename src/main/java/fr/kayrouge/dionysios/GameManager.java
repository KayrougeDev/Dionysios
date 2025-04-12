package fr.kayrouge.dionysios;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Optional;

public class GameManager {

    @Getter
    private final HashMap<Integer, Game> games = new HashMap<>();
    public final NamespacedKey PLAYER_GAME_KEY;
    public final Plugin PLUGIN;

    @Nullable
    @Getter
    @Setter
    private Location afterGameLocation;

    public GameManager(Plugin plugin) {
        PLUGIN = plugin;
        PLAYER_GAME_KEY = new NamespacedKey(PLUGIN, "game");
    }

    public Game createGame(Game game) {
        int id = 0;
        Optional<Integer> biggestId = games.keySet().stream().max(Integer::compareTo);
        if(biggestId.isPresent()) {
            id = biggestId.get()+1;
        }
        game.settings.setId(id);
        PLUGIN.getServer().getPluginManager().registerEvents(game, PLUGIN);
        games.put(id, game);
        return game;
    }

    public void deleteGame(int id) {
        if(games.containsKey(id)) {
            HandlerList.unregisterAll(games.get(id));
            games.remove(id);
        }
    }

}

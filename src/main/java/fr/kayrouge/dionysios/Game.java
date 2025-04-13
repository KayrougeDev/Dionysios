package fr.kayrouge.dionysios;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class Game implements Listener {

    protected final HashMap<UUID, GRole> players = new HashMap<>();


    @Getter
    @Setter
    private GState state;
    protected final GameManager manager;
    @Getter
    protected final GameSettings settings;

    public Game(GameManager manager, GameSettings settings) {
        this.manager = manager;
        this.settings = settings;
    }

    public boolean isState(GState state) {
        return this.state == state;
    }

    public void onGameWaiting() {
    }

    public void onGameStarting() {
    }

    public void onGamePlaying() {
    }

    public void onGameFinished() {
        AtomicInteger timer = new AtomicInteger((int)settings.getTimeToTerminate());
        Bukkit.getScheduler().runTaskTimer(manager.PLUGIN, bukkitTask -> {
             if(timer.get() == 0) {
                 bukkitTask.cancel();
                 setStateAndCall(GState.TERMINATED);
                 return;
             }
             int time = timer.getAndDecrement();

             if(time == 30 || time == 15 || time < 11) {
                 if(!settings.getGameFinishedMessage().isEmpty()) {
                     sendMessageToAllPlayer(settings.getGameFinishedMessage().replace("$TIMELEFT", String.valueOf(time)));
                 }
             }
        }, 0, settings.getTimeToTerminate() * 20L);
    }

    public void onGameTerminated() {
        manager.deleteGame(settings.getId());
        if(settings.isTpAfterGameTerminated()) {
            players.forEach((uuid, gRole) -> {
                Player player = Bukkit.getPlayer(uuid);
                if(player != null) {
                    Location location = manager.getAfterGameLocation();
                    if(location != null) {
                        player.teleport(location);
                    }
                }
            });
        }
    }

    public void playerJoin(Player player, boolean isSpectator) {
        if(players.containsKey(player.getUniqueId())) return;
        if(isState(GState.WAITING) && getPlayerCount(GRole.PLAYER) < settings.getMaxPlayerCount() && !isSpectator) {
            players.put(player.getUniqueId(), GRole.PLAYER);
        }
        else {
            players.put(player.getUniqueId(), GRole.SPECTATOR);
        }
        player.getPersistentDataContainer().set(manager.PLAYER_GAME_KEY, PersistentDataType.INTEGER, settings.getId());
        sendMessageToAllPlayer(player.getDisplayName()+" has joined the game ("+getPlayerCount(GRole.PLAYER)+"/"+settings.getMaxPlayerCount()+")");

        if(getPlayerCount(GRole.PLAYER) >= settings.getMinPlayerCount()) {
            startCounter();
        }
    }

    public void lose(Player player) {
        GRole role = getRole(player);
        if(role == null || role == GRole.SPECTATOR) return;

        players.put(player.getUniqueId(), GRole.SPECTATOR);
        checkplayerAndStop();
    }

    public void startCounter() {
        if(!isState(GState.WAITING)) return;

        AtomicInteger remainTimeBeforeStart = new AtomicInteger(10);
        Bukkit.getScheduler().runTaskTimer(manager.PLUGIN, bukkitTask -> {
            if(checkAndStop(bukkitTask)) return;
            int playerCount = getPlayerCount(GRole.PLAYER);
            if(getPlayerCount(GRole.PLAYER) < settings.getMinPlayerCount()) {
                sendMessageToAllPlayer("Canceling game launch because a player quit the game ("+playerCount+"/"+settings.getMinPlayerCount()+" players)");
                bukkitTask.cancel();
                return;
            }

            if(remainTimeBeforeStart.get() > 0) {
                sendMessageToAllPlayer("Game starting in "+remainTimeBeforeStart.getAndDecrement()+" seconds ("+playerCount+"/"+settings.getMinPlayerCount()+" players)");
            }
            else {
                bukkitTask.cancel();
                setStateAndCall(GState.STARTING);
            }

        }, 0L, 20L);
    }

    public void playerQuit(Player player) {
        players.remove(player.getUniqueId());
        player.getPersistentDataContainer().remove(manager.PLAYER_GAME_KEY);

        checkplayerAndStop();
    }

    public void sendMessageToAllPlayer(String message) {
        players.forEach((uuid, role) -> {
            Player player = Bukkit.getPlayer(uuid);
            if(player != null) {
                player.sendMessage(message);
            }
        });
    }

    @EventHandler
    public void onQuitServer(PlayerQuitEvent event) {
        playerQuit(event.getPlayer());
    }

    public boolean isInGame(Player player) {
        return players.containsKey(player.getUniqueId());
    }

    @Nullable
    public GRole getRole(Player player) {
        return players.get(player.getUniqueId());
    }


    public int getPlayerCount(GRole role) {
        return (int)players.values().stream().filter(gRole -> gRole == role).count();
    }

    public Stream<UUID> getPlayers(GRole role) {
        return players.keySet().stream().filter(uuid -> players.get(uuid) == role);
    }

    public int getTotalPlayerCount() {
        return players.size();
    }

    public boolean checkAndStop(BukkitTask task) {
        if(isState(GState.FINISHED) || isState(GState.TERMINATED)) {
            task.cancel();
            return true;
        }
        return false;
    }

    public boolean checkplayerAndStop() {
        if(getPlayerCount(GRole.PLAYER) == 0 && !isState(GState.WAITING)) {
            setStateAndCall(GState.FINISHED);
            return true;
        }
        return false;
    }

    public void setStateAndCall(GState state) {
        this.state = state;
        switch (state) {
            case WAITING:
                onGameWaiting();
                break;
            case STARTING:
                onGameStarting();
                break;
            case PLAYING:
                onGamePlaying();
                break;
            case FINISHED:
                onGameFinished();
                break;
            case TERMINATED:
                onGameTerminated();
                break;
        }
    }
}

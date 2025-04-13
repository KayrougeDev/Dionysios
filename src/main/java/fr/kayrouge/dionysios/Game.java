package fr.kayrouge.dionysios;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
        }, 0, 20L);
    }

    public void onGameTerminated() {
        getPlayers().forEach(this::playerQuit);

        manager.deleteGame(settings.getId());
    }

    public void playerJoin(Player player, AtomicBoolean isSpectator) {
        if(players.containsKey(player.getUniqueId())) return;
        if((isState(GState.WAITING) || isState(GState.PRESTART)) && getPlayerCount(GRole.PLAYER) < settings.getMaxPlayerCount() && !isSpectator.get()) {
            players.put(player.getUniqueId(), GRole.PLAYER);
        }
        else {
            players.put(player.getUniqueId(), GRole.SPECTATOR);
            isSpectator.set(true);
        }
        player.getPersistentDataContainer().set(manager.PLAYER_GAME_KEY, PersistentDataType.INTEGER, settings.getId());
        sendMessageToAllPlayer(player.getDisplayName()+" has joined the game ("+getPlayerCount(GRole.PLAYER)+"/"+settings.getMaxPlayerCount()+")");

        if(getPlayerCount(GRole.PLAYER) >= settings.getMinPlayerCount()) {
            if(isState(GState.WAITING)) {
                setStateAndCall(GState.PRESTART);
            }
        }
    }

    public void lose(Player player) {
        GRole role = getRole(player);
        if(role == null || role == GRole.SPECTATOR) return;

        players.put(player.getUniqueId(), GRole.SPECTATOR);
        checkPlayerAndStop();
    }

    public void preStart() {
        AtomicInteger remainTimeBeforeStart = new AtomicInteger(10);
        Bukkit.getScheduler().runTaskTimer(manager.PLUGIN, bukkitTask -> {
            if(checkAndStop(bukkitTask)) return;
            int playerCount = getPlayerCount(GRole.PLAYER);
            if(getPlayerCount(GRole.PLAYER) < settings.getMinPlayerCount()) {
                sendMessageToAllPlayer("Canceling game launch because a player quit the game ("+playerCount+"/"+settings.getMinPlayerCount()+" players required)");
                bukkitTask.cancel();
                setStateAndCall(GState.WAITING);
                return;
            }

            if(remainTimeBeforeStart.get() > 0) {
                sendMessageToAllPlayer("Game starting in "+remainTimeBeforeStart.getAndDecrement()+" seconds ("+playerCount+"/"+settings.getMaxPlayerCount()+" players)");
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

        if(this.manager.getAfterGameLocation() != null) {
            player.teleport(this.manager.getAfterGameLocation());
        }

        checkPlayerAndStop();
    }

    public void sendMessageToAllPlayer(String message) {
        getPlayers().forEach(player -> player.sendMessage(message));
    }

    @EventHandler
    public void onQuitServer(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if(!players.containsKey(player.getUniqueId())) return;
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

    public List<Player> getPlayersByRole(GRole role) {
        return players.keySet().stream()
                .filter(uuid -> players.get(uuid) == role)
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<Player> getPlayers() {
        return players.keySet().stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
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

    public boolean checkPlayerAndStop() {
        if(getPlayerCount(GRole.PLAYER) == 0 && !isState(GState.WAITING) && !this.isState(GState.FINISHED) && !this.isState(GState.TERMINATED)) {
            if(getPlayerCount(GRole.SPECTATOR) == 0) {
                setStateAndCall(GState.TERMINATED);
            }
            else {
                setStateAndCall(GState.FINISHED);
            }
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
            case PRESTART:
                preStart();
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
/*
 * This file is licensed under the MIT License (MIT).
 *
 * Copyright (c) 2014 Daniel Ennis <http://aikar.co>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package co.aikar.timings;

import com.nukkitx.protocol.bedrock.BedrockPacket;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.event.Event;
import org.cloudburstmc.server.scheduler.PluginTask;
import org.cloudburstmc.server.scheduler.TaskHandler;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import static co.aikar.timings.TimingIdentifier.DEFAULT_GROUP;

@Log4j2
public final class Timings {
    private static boolean timingsEnabled = false;
    private static boolean verboseEnabled = false;
    private static boolean privacy = false;
    private static Set<String> ignoredConfigSections = new HashSet<>();

    private static final int MAX_HISTORY_FRAMES = 12;
    private static int historyInterval = -1;
    private static int historyLength = -1;

    public static final FullServerTickTiming fullServerTickTimer;
    public static final Timing timingsTickTimer;
    public static final Timing pluginEventTimer;

    public static final Timing connectionTimer;
    public static final Timing schedulerTimer;
    public static final Timing schedulerAsyncTimer;
    public static final Timing schedulerSyncTimer;
    public static final Timing commandTimer;
    public static final Timing serverCommandTimer;
    public static final Timing levelSaveTimer;

    public static final Timing playerNetworkSendTimer;
    public static final Timing playerNetworkReceiveTimer;
    public static final Timing playerChunkOrderTimer;
    public static final Timing playerChunkSendTimer;
    public static final Timing playerCommandTimer;
    public static final Timing playerEntityLookingAtTimer;
    public static final Timing playerEntityAtPositionTimer;

    public static final Timing tickEntityTimer;
    public static final Timing tickBlockEntityTimer;
    public static final Timing entityMoveTimer;
    public static final Timing entityBaseTickTimer;
    public static final Timing livingEntityBaseTickTimer;

    public static final Timing generationTimer;
    public static final Timing populationTimer;
    public static final Timing generationCallbackTimer;

    public static final Timing permissibleCalculationTimer;
    public static final Timing permissionDefaultTimer;

    static {
        setTimingsEnabled(CloudServer.getInstance().getConfig().getTimings().isEnabled());
        setVerboseEnabled(CloudServer.getInstance().getConfig().getTimings().isVerbose());
        setHistoryInterval(CloudServer.getInstance().getConfig().getTimings().getHistoryInterval());
        setHistoryLength(CloudServer.getInstance().getConfig().getTimings().getHistoryLength());

        privacy = CloudServer.getInstance().getConfig().getTimings().isPrivacy();
        ignoredConfigSections.addAll(CloudServer.getInstance().getConfig().getTimings().getIgnore());

        log.debug("Timings: \n" +
                "Enabled - " + isTimingsEnabled() + "\n" +
                "Verbose - " + isVerboseEnabled() + "\n" +
                "History Interval - " + getHistoryInterval() + "\n" +
                "History Length - " + getHistoryLength());

        fullServerTickTimer = new FullServerTickTiming();
        timingsTickTimer = TimingsManager.getTiming(DEFAULT_GROUP.name, "Timings Tick", fullServerTickTimer);
        pluginEventTimer = TimingsManager.getTiming("Plugin Events");

        connectionTimer = TimingsManager.getTiming("Connection Handler");
        schedulerTimer = TimingsManager.getTiming("Scheduler");
        schedulerAsyncTimer = TimingsManager.getTiming("## Scheduler - Async Tasks");
        schedulerSyncTimer = TimingsManager.getTiming("## Scheduler - Sync Tasks");
        commandTimer = TimingsManager.getTiming("Commands");
        serverCommandTimer = TimingsManager.getTiming("Server Command");
        levelSaveTimer = TimingsManager.getTiming("Level Save");

        playerNetworkSendTimer = TimingsManager.getTiming("Player Network Send");
        playerNetworkReceiveTimer = TimingsManager.getTiming("Player Network Receive");
        playerChunkOrderTimer = TimingsManager.getTiming("Player Order Chunks");
        playerChunkSendTimer = TimingsManager.getTiming("Player Send Chunks");
        playerCommandTimer = TimingsManager.getTiming("Player Command");
        playerEntityLookingAtTimer = TimingsManager.getTiming("## Player: Entity Looking At");
        playerEntityAtPositionTimer = TimingsManager.getTiming(DEFAULT_GROUP.name, "## Player: Entity At Position", playerEntityLookingAtTimer);

        tickEntityTimer = TimingsManager.getTiming("## Entity Tick");
        tickBlockEntityTimer = TimingsManager.getTiming("## BlockEntity Tick");
        entityMoveTimer = TimingsManager.getTiming("## Entity Move");
        entityBaseTickTimer = TimingsManager.getTiming("## Entity Base Tick");
        livingEntityBaseTickTimer = TimingsManager.getTiming("## LivingEntity Base Tick");

        generationTimer = TimingsManager.getTiming("Level Generation");
        populationTimer = TimingsManager.getTiming("Level Population");
        generationCallbackTimer = TimingsManager.getTiming("Level Generation Callback");

        permissibleCalculationTimer = TimingsManager.getTiming("Permissible Calculation");
        permissionDefaultTimer = TimingsManager.getTiming("Default Permission Calculation");
    }

    public static boolean isTimingsEnabled() {
        return timingsEnabled;
    }

    public static void setTimingsEnabled(boolean enabled) {
        timingsEnabled = enabled;
        TimingsManager.reset();
    }

    public static boolean isVerboseEnabled() {
        return verboseEnabled;
    }

    public static void setVerboseEnabled(boolean enabled) {
        verboseEnabled = enabled;
        TimingsManager.needsRecheckEnabled = true;
    }

    public static boolean isPrivacy() {
        return privacy;
    }

    public static Set<String> getIgnoredConfigSections() {
        return ignoredConfigSections;
    }

    public static int getHistoryInterval() {
        return historyInterval;
    }

    public static void setHistoryInterval(int interval) {
        historyInterval = Math.max(20 * 60, interval);
        //Recheck the history length with the new Interval
        if (historyLength != -1) {
            setHistoryLength(historyLength);
        }
    }

    public static int getHistoryLength() {
        return historyLength;
    }

    public static void setHistoryLength(int length) {
        //Cap at 12 History Frames, 1 hour at 5 minute frames.
        int maxLength = historyInterval * MAX_HISTORY_FRAMES;
        //For special cases of servers with special permission to bypass the max.
        //This max helps keep data file sizes reasonable for processing on Aikar's Timing parser side.
        //Setting this will not help you bypass the max unless Aikar has added an exception on the API side.
        if (CloudServer.getInstance().getConfig().getTimings().isBypassMax()) {
            maxLength = Integer.MAX_VALUE;
        }

        historyLength = Math.max(Math.min(maxLength, length), historyInterval);

        Queue<TimingsHistory> oldQueue = TimingsManager.HISTORY;
        int frames = (getHistoryLength() / getHistoryInterval());
        if (length > maxLength) {
            log.warn("Timings Length too high. Requested " + length + ", max is " + maxLength
                    + ". To get longer history, you must increase your interval. Set Interval to "
                    + Math.ceil((float) length / MAX_HISTORY_FRAMES)
                    + " to achieve this length.");
        }

        TimingsManager.HISTORY = new TimingsManager.BoundedQueue<>(frames);
        TimingsManager.HISTORY.addAll(oldQueue);
    }

    public static void reset() {
        TimingsManager.reset();
    }


    public static Timing getCommandTiming(Command command) {
        return TimingsManager.getTiming(DEFAULT_GROUP.name, "Command: " + command.getLabel(), commandTimer);
    }

    public static Timing getTaskTiming(TaskHandler handler, long period) {
        String repeating = " ";
        if (period > 0) {
            repeating += "(interval:" + period + ")";
        } else {
            repeating += "(Single)";
        }

        if (handler.getTask() instanceof PluginTask) {
            String owner = ((PluginTask<?>) handler.getTask()).getContainer().getDescription().getName();
            return TimingsManager.getTiming(owner, "PluginTask: " + handler.getTaskId() + repeating, schedulerSyncTimer);
        } else if (!handler.isAsynchronous()) {
            return TimingsManager.getTiming(DEFAULT_GROUP.name, "Task: " + handler.getTaskId() + repeating, schedulerSyncTimer);
        } else {
            return null;
        }
    }

    public static Timing getPluginEventTiming(Class<? extends Event> event, Object listener, Method method, PluginContainer plugin) {
        Timing group = TimingsManager.getTiming(plugin.getDescription().getName(), "Combined Total", pluginEventTimer);

        return TimingsManager.getTiming(plugin.getDescription().getName(), "Event: " + listener.getClass().getName() + "."
                + (method.getName())
                + " (" + event.getSimpleName() + ")", group);
    }

    public static Timing getEntityTiming(EntityType<?> type) {
        return TimingsManager.getTiming(DEFAULT_GROUP.name, "## Entity Tick: " + type.getIdentifier(), tickEntityTimer);
    }

    public static Timing getBlockEntityTiming(BlockEntity blockEntity) {
        return TimingsManager.getTiming(DEFAULT_GROUP.name, "## BlockEntity Tick: " + blockEntity.getClass().getSimpleName(), tickBlockEntityTimer);
    }

    public static Timing getReceiveDataPacketTiming(BedrockPacket pk) {
        return TimingsManager.getTiming(DEFAULT_GROUP.name, "## Receive Packet: " + pk.getClass().getSimpleName(), playerNetworkReceiveTimer);
    }

    public static Timing getSendDataPacketTiming(BedrockPacket pk) {
        return TimingsManager.getTiming(DEFAULT_GROUP.name, "## Send Packet: " + pk.getClass().getSimpleName(), playerNetworkSendTimer);
    }

    public static void stopServer() {
        setTimingsEnabled(false);
        TimingsManager.recheckEnabled();
    }
}

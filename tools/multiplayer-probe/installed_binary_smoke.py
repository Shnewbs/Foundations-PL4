"""Isolated server-only smoke for a genuine installed Foundations PL4 JAR.

Do not include the production PL4 source in this dev workspace. This gate is
NOT two-client migration, installed-provider acceptance, or RC clearance.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import signal
import socket
import struct
import subprocess
import time
import zipfile

MODID = "foundations_pl4"
PROBE_MODID = "pl4_binary_probe"
JAVA_REL = "src/main/java/net/foundations/pl4probe/InstalledBinaryProbe.java"
TOML_REL = "src/main/resources/META-INF/neoforge.mods.toml"
MARKER = "PL4_INSTALLED_BINARY_READY:"

BUILD = """plugins {
    id 'java'
    id 'net.neoforged.moddev' version '2.0.147'
}
version = '1.0.0-probe'
group = 'net.foundations.pl4probe'
java { toolchain.languageVersion = JavaLanguageVersion.of(21) }
neoForge {
    enable {
        version = '21.1.250'
        disableRecompilation = true
    }
    mods { pl4_binary_probe { sourceSet sourceSets.main } }
    runs {
        server { server(); programArgument '--nogui'; jvmArguments.addAll('-Xms256m', '-Xmx768m') }
        configureEach { logLevel = org.slf4j.event.Level.INFO }
    }
}
"""

JAVA = r"""package net.foundations.pl4probe;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Independent test mod; NEVER contains or compiles PL4 implementation classes. */
@Mod("pl4_binary_probe")
public final class InstalledBinaryProbe {
    public InstalledBinaryProbe(IEventBus bus, ModContainer container) {
        NeoForge.EVENT_BUS.addListener(this::serverStarted);
        NeoForge.EVENT_BUS.addListener(this::serverTick);
    }


    // Only the installed PL4 JAR supplies these classes. Reflection prevents
    // compiling implementation classes into this independent probe workspace.
    private static final UUID PART_OWNER = UUID.fromString("3d83227f-3df1-4c66-a188-a2f271bb86b3");

    private static Object installedPart(ClassLoader loader, String kindName, Direction face)
            throws ReflectiveOperationException {
        Class<?> kind = Class.forName("net.foundations.pl4.Kind", true, loader);
        Class<?> part = Class.forName("net.foundations.pl4.Part", true, loader);
        return part.getConstructor(kind, Direction.class, UUID.class)
            .newInstance(kind.getField(kindName).get(null), face, PART_OWNER);
    }

    @SuppressWarnings("unchecked")
    private static String configuredMultipartProof(net.minecraft.world.level.block.entity.BlockEntity host,
            boolean seed) {
        if (!host.getClass().getName().equals("net.foundations.pl4.HostEntity")) {
            throw new IllegalStateException("World host is not installed PL4 HostEntity");
        }
        try {
            ClassLoader loader = host.getClass().getClassLoader();
            Class<?> partType = Class.forName("net.foundations.pl4.Part", true, loader);
            Class<?> hostBlock = Class.forName("net.foundations.pl4.HostBlock", true, loader);
            Map<Integer, Object> parts = (Map<Integer, Object>) host.getClass().getField("parts").get(host);
            if (seed) {
                if (!parts.isEmpty()) throw new IllegalStateException("Fresh PL4 host already contains parts");
                var canAdd = hostBlock.getMethod("canAdd", host.getClass(), partType);
                Object cable = installedPart(loader, "DATA_CABLE", Direction.DOWN);
                if (!Boolean.TRUE.equals(canAdd.invoke(null, host, cable)))
                    throw new IllegalStateException("Installed PL4 rejects native centre cable");
                parts.put((Integer) partType.getMethod("slot").invoke(cable), cable);
                Object reader = installedPart(loader, "INVENTORY_READER", Direction.EAST);
                if (!Boolean.TRUE.equals(canAdd.invoke(null, host, reader)))
                    throw new IllegalStateException("Installed PL4 rejects barrel-facing inventory reader");
                parts.put((Integer) partType.getMethod("slot").invoke(reader), reader);
                host.getClass().getMethod("changed").invoke(host);
            }
            // Reopen must load the same native saved Part objects; no fixture
            // reconstruction or new UUID generation occurs on restore.
            if (parts.size() != 2 || !parts.containsKey(5) || !parts.containsKey(6)) {
                throw new IllegalStateException("Native saved PL4 parts missing or slot map changed");
            }
            StringBuilder result = new StringBuilder("[");
            UUID firstIdentity = null;
            for (int slot : new int[]{5, 6}) {
                Object part = parts.get(slot);
                if (part == null || part.getClass() != partType
                        || !slotEquals(partType, part, slot)) {
                    throw new IllegalStateException("Installed PL4 part has invalid native slot");
                }
                String kind = ((Enum<?>) partType.getField("kind").get(part)).name();
                Direction face = (Direction) partType.getField("face").get(part);
                UUID owner = (UUID) partType.getField("owner").get(part);
                UUID identity = (UUID) partType.getField("identity").get(part);
                String expectedKind = slot == 5 ? "INVENTORY_READER" : "DATA_CABLE";
                Direction expectedFace = slot == 5 ? Direction.EAST : Direction.DOWN;
                if (!expectedKind.equals(kind) || face != expectedFace
                        || !PART_OWNER.equals(owner) || identity == null
                        || identity.equals(firstIdentity)) {
                    throw new IllegalStateException("Installed PL4 configured part identity changed");
                }
                firstIdentity = slot == 5 ? identity : firstIdentity;
                if (slot == 6) result.append(",");
                result.append("{\"slot\":").append(slot)
                    .append(",\"kind\":\"").append(kind)
                    .append("\",\"face\":\"").append(face.name())
                    .append("\",\"owner\":\"").append(owner)
                    .append("\",\"identity\":\"").append(identity).append("\"}");
            }
            return result.append("]").toString();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Installed PL4 reflection/part API unavailable", e);
        }
    }

    private static boolean slotEquals(Class<?> partType, Object part, int slot)
            throws ReflectiveOperationException {
        return ((Integer) partType.getMethod("slot").invoke(part)) == slot;
    }


    // Native installed-JAR transfer engine and vanilla capability: supplied
    // endpoint refs are explicit, so topology discovery and players are NOT tested.
    @SuppressWarnings("unchecked")
    private static String installedTransferProof(net.minecraft.server.MinecraftServer server,
            net.minecraft.server.level.ServerLevel level, Block hostBlock, boolean seed) {
        BlockPos[] hosts = {new BlockPos(0, 80, 2), new BlockPos(4, 80, 2)};
        if (seed) for (BlockPos pos : hosts) {
            if (!level.setBlock(pos, hostBlock.defaultBlockState(), 3)
                    || !level.setBlock(pos.east(), Blocks.BARREL.defaultBlockState(), 3))
                throw new IllegalStateException("Cannot place installed transfer host and barrel");
        }
        try {
            Object[] entities = {level.getBlockEntity(hosts[0]), level.getBlockEntity(hosts[1])};
            if (entities[0] == null || entities[1] == null)
                throw new IllegalStateException("Missing installed transfer hosts");
            ClassLoader loader = entities[0].getClass().getClassLoader();
            Class<?> host = Class.forName("net.foundations.pl4.HostEntity", true, loader);
            Class<?> part = Class.forName("net.foundations.pl4.Part", true, loader);
            Class<?> ref = Class.forName("net.foundations.pl4.NetworkEngine$Ref", true, loader);
            Class<?> engine = Class.forName("net.foundations.pl4.TransferEngine", true, loader);
            Class<?> block = Class.forName("net.foundations.pl4.HostBlock", true, loader);
            Object[] pieces = new Object[2];
            BarrelBlockEntity[] barrels = new BarrelBlockEntity[2];
            for (int i = 0; i < 2; i++) {
                if (!host.isInstance(entities[i])
                        || !(level.getBlockEntity(hosts[i].east()) instanceof BarrelBlockEntity))
                    throw new IllegalStateException("Native transfer host/barrel type mismatch");
                barrels[i] = (BarrelBlockEntity) level.getBlockEntity(hosts[i].east());
                Map<Integer, Object> parts = (Map<Integer, Object>) host.getField("parts").get(entities[i]);
                if (seed) {
                    if (!parts.isEmpty()) throw new IllegalStateException("Transfer host was not fresh");
                    Object node = installedPart(loader, "TRANSFER_NODE", Direction.EAST);
                    part.getField("transferMode").setInt(node, i == 0 ? 2 : 1);
                    part.getField("fluids").setBoolean(node, false);
                    part.getField("energy").setBoolean(node, false);
                    if (!Boolean.TRUE.equals(block.getMethod("canAdd", host, part).invoke(null, entities[i], node)))
                        throw new IllegalStateException("Installed TRANSFER_NODE placement rejected");
                    parts.put((Integer) part.getMethod("slot").invoke(node), node);
                    host.getMethod("changed").invoke(entities[i]);
                }
                if (parts.size() != 1 || !parts.containsKey(5))
                    throw new IllegalStateException("Native transfer node absent on reload");
                pieces[i] = parts.get(5);
                if (!"TRANSFER_NODE".equals(((Enum<?>)part.getField("kind").get(pieces[i])).name())
                        || part.getField("face").get(pieces[i]) != Direction.EAST
                        || !PART_OWNER.equals(part.getField("owner").get(pieces[i]))
                        || part.getField("transferMode").getInt(pieces[i]) != (i == 0 ? 2 : 1)
                        || !part.getField("items").getBoolean(pieces[i])
                        || part.getField("fluids").getBoolean(pieces[i])
                        || part.getField("energy").getBoolean(pieces[i]))
                    throw new IllegalStateException("Installed transfer direction/owner/flags changed");
            }
            if (seed) {
                if (barrels[0].getContainerSize() != 27 || barrels[1].getContainerSize() != 27)
                    throw new IllegalStateException("Unexpected native barrel capacity");
                for (int i = 0; i < 27; i++) barrels[0].setItem(i, new ItemStack(Items.DIAMOND, 64));
                barrels[0].setChanged();
            }
            int[] before = new int[2], after = new int[2];
            for (int i = 0; i < 2; i++) {
                before[i] = diamonds(barrels[i]);
            }
            if (before[0] <= 0 || before[0] + before[1] != 1728
                    || (seed && before[1] != 0) || (!seed && before[1] <= 0))
                throw new IllegalStateException("Native transfer pre-state or save conservation failed");
            Object source = ref.getConstructor(host, part).newInstance(entities[0], pieces[0]);
            Object sink = ref.getConstructor(host, part).newInstance(entities[1], pieces[1]);
            engine.getMethod("run", net.minecraft.server.MinecraftServer.class, java.util.List.class)
                .invoke(null, server, java.util.List.of(source, sink));
            for (int i = 0; i < 2; i++) {
                after[i] = diamonds(barrels[i]);
                barrels[i].setChanged();
                ((net.minecraft.world.level.block.entity.BlockEntity)entities[i]).setChanged();
                if (!((ItemStack)part.getField("pendingItem").get(pieces[i])).isEmpty())
                    throw new IllegalStateException("Pending item escrow after actual transfer");
            }
            int moved = before[0] - after[0];
            if (moved <= 0 || after[0] <= 0 || moved != after[1] - before[1]
                    || after[0] + after[1] != 1728)
                throw new IllegalStateException("Installed TransferEngine failed live delivery/conservation");
            String sourceId = part.getField("identity").get(pieces[0]).toString();
            String sinkId = part.getField("identity").get(pieces[1]).toString();
            if (sourceId.equals(sinkId)) throw new IllegalStateException("Duplicate native transfer node UUID");
            return "{\"engine\":\"net.foundations.pl4.TransferEngine\",\"source_before\":"
                + before[0] + ",\"source_after\":" + after[0] + ",\"sink_before\":"
                + before[1] + ",\"sink_after\":" + after[1] + ",\"delivered\":"
                + moved + ",\"total\":1728,\"source_identity\":\"" + sourceId
                + "\",\"sink_identity\":\"" + sinkId + "\",\"pending_escrow\":0}";
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Real installed PL4 transfer API unavailable", failure);
        }
    }

    /** Isolated physical DATA_CABLE graph; no manual Ref list or transfer-rate claim. */
    @SuppressWarnings("unchecked")
    private static String installedCableTopologyProof(net.minecraft.server.MinecraftServer server,
            net.minecraft.server.level.ServerLevel level, Block nativeHostBlock, boolean seed) {
        BlockPos[] cells = {new BlockPos(0, 80, 5), new BlockPos(0, 80, 6),
            new BlockPos(1, 80, 6), new BlockPos(2, 80, 6),
            new BlockPos(3, 80, 6), new BlockPos(4, 80, 6),
            new BlockPos(4, 80, 5)};
        try {
            ClassLoader loader = nativeHostBlock.getClass().getClassLoader();
            Class<?> host = Class.forName("net.foundations.pl4.HostEntity", true, loader);
            Class<?> part = Class.forName("net.foundations.pl4.Part", true, loader);
            Class<?> block = Class.forName("net.foundations.pl4.HostBlock", true, loader);
            Class<?> network = Class.forName("net.foundations.pl4.NetworkEngine", true, loader);
            Class<?> ref = Class.forName("net.foundations.pl4.NetworkEngine$Ref", true, loader);
            var changed = host.getMethod("changed");
            var canAdd = block.getMethod("canAdd", host, part);
            Object[] cableParts = new Object[7];
            Object[] endpointParts = new Object[2];
            for (int i = 0; i < cells.length; i++) {
                BlockPos pos = cells[i];
                if (seed) {
                    level.setBlock(pos.below(), Blocks.IRON_BLOCK.defaultBlockState(), 3);
                    if (!level.setBlock(pos, nativeHostBlock.defaultBlockState(), 3))
                        throw new IllegalStateException("Cannot place installed DATA_CABLE route");
                }
                Object entity = level.getBlockEntity(pos);
                if (!host.isInstance(entity)) throw new IllegalStateException("Installed cable host is missing");
                Map<Integer, Object> parts = (Map<Integer, Object>) host.getField("parts").get(entity);
                if (seed) {
                    Object cable = installedPart(loader, "DATA_CABLE", Direction.DOWN);
                    if (!Boolean.TRUE.equals(canAdd.invoke(null, entity, cable)))
                        throw new IllegalStateException("Native cable centre shape rejected");
                    parts.put(6, cable);
                    if (i == 0 || i == 6) {
                        Object endpoint = installedPart(loader, "TRANSFER_NODE", Direction.EAST);
                        part.getField("transferMode").setInt(endpoint, 0);
                        part.getField("items").setBoolean(endpoint, false);
                        part.getField("fluids").setBoolean(endpoint, false);
                        part.getField("energy").setBoolean(endpoint, false);
                        if (!Boolean.TRUE.equals(canAdd.invoke(null, entity, endpoint)))
                            throw new IllegalStateException("Native endpoint/cable multipart overlap");
                        parts.put(5, endpoint);
                    }
                    changed.invoke(entity);
                }
                if (parts.size() != ((i == 0 || i == 6) ? 2 : 1)
                        || parts.get(6) == null
                        || !"DATA_CABLE".equals(((Enum<?>) part.getField("kind").get(parts.get(6))).name())
                        || part.getField("blockedFaces").getInt(parts.get(6)) != 0)
                    throw new IllegalStateException("Installed cable route was not saved intact");
                cableParts[i] = parts.get(6);
                if (i == 0 || i == 6) {
                    Object endpoint = parts.get(5);
                    if (endpoint == null || !"TRANSFER_NODE".equals(
                            ((Enum<?>)part.getField("kind").get(endpoint)).name())
                            || part.getField("face").get(endpoint) != Direction.EAST
                            || part.getField("transferMode").getInt(endpoint) != 0
                            || !PART_OWNER.equals(part.getField("owner").get(endpoint)))
                        throw new IllegalStateException("Installed passive topology endpoint changed");
                    endpointParts[i == 0 ? 0 : 1] = endpoint;
                }
            }
            var discover = network.getMethod("storageNodes", net.minecraft.server.MinecraftServer.class, part);
            java.util.List<?> linked = (java.util.List<?>) discover.invoke(null, server, endpointParts[0]);
            if (linked.size() != 2) {
                var observedNetwork = (java.util.List<?>) network.getMethod("all",
                    net.minecraft.server.MinecraftServer.class).invoke(null, server);
                StringBuilder detail = new StringBuilder();
                for (Object reference : observedNetwork) {
                    Object candidate = ref.getMethod("part").invoke(reference);
                    Object nativeHost = ref.getMethod("host").invoke(reference);
                    detail.append(" [").append(
                        ((net.minecraft.world.level.block.entity.BlockEntity)nativeHost).getBlockPos())
                        .append(",").append(((Enum<?>)part.getField("kind").get(candidate)).name())
                        .append(",").append(part.getMethod("slot").invoke(candidate)).append("]");
                }
                throw new IllegalStateException("Installed NetworkEngine did not connect both "
                    + "seven-centre cable endpoints: discovered=" + linked.size()
                    + " all_loaded_parts=" + observedNetwork.size() + detail);
            }
            java.util.Set<Object> observed = new java.util.HashSet<>();
            for (Object row : linked) {
                if (!ref.isInstance(row)) throw new IllegalStateException("Foreign topology reference");
                observed.add(ref.getMethod("part").invoke(row));
            }
            if (observed.size() != 2 || !observed.contains(endpointParts[0])
                    || !observed.contains(endpointParts[1]))
                throw new IllegalStateException("Native network graph includes missing/foreign endpoints");
            Object midHost = level.getBlockEntity(cells[3]);
            int eastBit = 1 << Direction.EAST.ordinal();
            part.getField("blockedFaces").setInt(cableParts[3], eastBit);
            changed.invoke(midHost);
            java.util.List<?> severed = (java.util.List<?>) discover.invoke(null, server, endpointParts[0]);
            if (severed.size() != 1 || ref.getMethod("part").invoke(severed.get(0)) != endpointParts[0])
                throw new IllegalStateException("Disabled cable arm did not isolate opposite endpoint");
            part.getField("blockedFaces").setInt(cableParts[3], 0);
            changed.invoke(midHost);
            java.util.List<?> restored = (java.util.List<?>) discover.invoke(null, server, endpointParts[0]);
            if (restored.size() != 2)
                throw new IllegalStateException("Reenabled cable arm did not restore physical network");
            String source = part.getField("identity").get(endpointParts[0]).toString();
            String sink = part.getField("identity").get(endpointParts[1]).toString();
            java.util.Set<String> allIds = new java.util.HashSet<>();
            allIds.add(source);
            allIds.add(sink);
            StringBuilder ids = new StringBuilder("[");
            for (int i = 0; i < cableParts.length; i++) {
                String identity = part.getField("identity").get(cableParts[i]).toString();
                if (!allIds.add(identity))
                    throw new IllegalStateException("Installed cable UUID reused");
                if (i > 0) ids.append(",");
                ids.append("\"").append(identity).append("\"");
            }
            ids.append("]");
            if (allIds.size() != 9 || source.equals(sink))
                throw new IllegalStateException("Native topology endpoint UUIDs are not unique");
            return "{\"network_engine\":\"net.foundations.pl4.NetworkEngine\","
                + "\"graph\":\"PHYSICAL_DATA_CABLE\","
                + "\"connected_endpoints\":2,\"severed_endpoints\":1,"
                + "\"restored_endpoints\":2,\"cable_count\":7,"
                + "\"source_identity\":\"" + source + "\",\"sink_identity\":\"" + sink
                + "\",\"cable_identities\":" + ids + ",\"automatic_tick_tested\":false}";
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Installed PL4 NetworkEngine topology API missing", failure);
        }
    }

    private static int diamonds(BarrelBlockEntity barrel) {
        int result = 0;
        for (int i = 0; i < barrel.getContainerSize(); i++) {
            ItemStack stack = barrel.getItem(i);
            if (!stack.isEmpty() && !stack.is(Items.DIAMOND))
                throw new IllegalStateException("Unexpected non-diamond in transfer barrel");
            result += stack.getCount();
        }
        return result;
    }

    private String pendingWorldPhase;
    private String pendingWorldSha;
    private String pendingParts;
    private String pendingTransfer;
    private Block pendingNativeBlock;
    private int nativeWaitTicks;

    /** Restore-side HostEntity.onLoad occurs on live chunk ticks, not ServerStartedEvent.
     * Do not force-register any PL4 host: wait for the actual installed runtime
     * to supply every loaded part before attesting its physical cable topology.
     */
    private void serverTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        if (pendingWorldPhase == null) return;
        var server = event.getServer();
        try {
            var nativeNetwork = Class.forName("net.foundations.pl4.NetworkEngine",
                true, pendingNativeBlock.getClass().getClassLoader());
            var loaded = (java.util.List<?>) nativeNetwork.getMethod("all",
                net.minecraft.server.MinecraftServer.class).invoke(null, server);
            int minimum = pendingWorldPhase.equals("seed") ? 4 : 13;
            if (loaded.size() < minimum) {
                if (++nativeWaitTicks <= 200) return;
                throw new IllegalStateException("Restored PL4 HostEntity.onLoad registration "
                    + "did not complete after 200 native ticks: loaded_parts=" + loaded.size()
                    + " expected_at_least=" + minimum);
            }
            String topology = installedCableTopologyProof(server, server.overworld(),
                pendingNativeBlock, pendingWorldPhase.equals("seed"));
            String destination = System.getenv("PL4_WORLD_PROOF");
            if (destination == null || destination.isEmpty())
                throw new IllegalStateException("Missing native world proof destination");
            Path proof = Path.of(destination);
            Files.createDirectories(proof.getParent());
            String worldJson = "{\"schema\":1,\"phase\":\"" + pendingWorldPhase
                + "\",\"expected_jar_sha256\":\"" + pendingWorldSha
                + "\",\"host\":\"foundations_pl4:multipart_host\""
                + ",\"host_entity\":\"foundations_pl4:host\""
                + ",\"barrel_diamonds\":128,\"server_started\":true,\"parts\":"
                + pendingParts + ",\"transfer\":" + pendingTransfer
                + ",\"topology\":" + topology + "}\n";
            Files.writeString(proof, worldJson, StandardCharsets.UTF_8);
            String phase = pendingWorldPhase, sha = pendingWorldSha;
            pendingWorldPhase = null;
            System.out.println("PL4_INSTALLED_WORLD_" + phase.toUpperCase() + ":" + sha);
            System.out.println("PL4_INSTALLED_BINARY_READY:" + sha);
        } catch (ReflectiveOperationException | java.io.IOException failure) {
            throw new IllegalStateException("Installed native graph lifecycle/proof unavailable", failure);
        }
    }

    private void serverStarted(ServerStartedEvent event) {
        if (!ModList.get().isLoaded("foundations_pl4")) {
            throw new IllegalStateException("Installed production PL4 mod was not loaded");
        }
        String sha = System.getenv("PL4_EXPECTED_JAR_SHA256");
        String output = System.getenv("PL4_INSTALLED_PROOF");
        if (sha == null || !sha.matches("[0-9a-f]{64}") || output == null) {
            throw new IllegalStateException("Missing installed-JAR attestation environment");
        }
        try {
            Path proof = Path.of(output);
            Files.createDirectories(proof.getParent());
            String json = "{\"schema\":1,\"loaded_mod_id\":\"foundations_pl4\","
                + "\"server_started\":true,\"expected_jar_sha256\":\"" + sha + "\"}\n";
            Files.writeString(proof, json, StandardCharsets.UTF_8);
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot preserve installed-server proof", failure);
        }
        // Optional real-world upgrade fixture: executes only with the installed
        // PL4 production JAR, never source-compiled PL4 implementation classes.
        String phase = System.getenv("PL4_WORLD_PHASE");
        if (phase != null) {
            if (!phase.equals("seed") && !phase.equals("restore")) {
                throw new IllegalStateException("Invalid installed-binary world phase");
            }
            ResourceLocation hostKey = ResourceLocation.fromNamespaceAndPath("foundations_pl4", "multipart_host");
            Block host = BuiltInRegistries.BLOCK.get(hostKey);
            if (host == Blocks.AIR || !BuiltInRegistries.BLOCK.getKey(host).equals(hostKey)) {
                throw new IllegalStateException("Installed PL4 multipart host registry is missing");
            }
            var level = event.getServer().overworld();
            BlockPos hostPos = new BlockPos(0, 80, 0);
            BlockPos barrelPos = hostPos.east();
            if (phase.equals("seed")) {
                for (int x = -4; x <= 4; x++) {
                    for (int z = -4; z <= 4; z++) {
                        level.setBlock(new BlockPos(x, 79, z), Blocks.IRON_BLOCK.defaultBlockState(), 3);
                    }
                }
                if (!level.setBlock(hostPos, host.defaultBlockState(), 3)
                    || !level.setBlock(barrelPos, Blocks.BARREL.defaultBlockState(), 3)) {
                    throw new IllegalStateException("Could not place native installed-PL4 world fixture");
                }
                if (!(level.getBlockEntity(barrelPos) instanceof BarrelBlockEntity)) {
                    throw new IllegalStateException("Native barrel was not created");
                }
                BarrelBlockEntity barrel = (BarrelBlockEntity) level.getBlockEntity(barrelPos);
                barrel.setItem(0, new ItemStack(Items.DIAMOND, 64));
                barrel.setItem(1, new ItemStack(Items.DIAMOND, 64));
                barrel.setChanged();
            }
            if (!level.getBlockState(hostPos).is(host)
                || level.getBlockEntity(hostPos) == null
                || !(level.getBlockEntity(barrelPos) instanceof BarrelBlockEntity)) {
                throw new IllegalStateException("Installed PL4 host/block entity lost during " + phase);
            }
            BarrelBlockEntity barrel = (BarrelBlockEntity) level.getBlockEntity(barrelPos);
            int remaining = 0;
            for (int slot = 0; slot < barrel.getContainerSize(); slot++) {
                ItemStack stack = barrel.getItem(slot);
                if (stack.is(Items.DIAMOND)) remaining += stack.getCount();
            }
            if (remaining != 128) {
                throw new IllegalStateException("Native barrel contents changed during " + phase + ": " + remaining);
            }
            String hostType = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(
                level.getBlockEntity(hostPos).getType()).toString();
            if (!hostType.equals("foundations_pl4:host")) {
                throw new IllegalStateException("Installed PL4 block entity lost its registered type: " + hostType);
            }
            String configuredParts = configuredMultipartProof(level.getBlockEntity(hostPos), phase.equals("seed"));
            String transferProof = installedTransferProof(event.getServer(), level, host, phase.equals("seed"));
            pendingWorldPhase = phase;
            pendingWorldSha = sha;
            pendingNativeBlock = host;
            pendingParts = configuredParts;
            pendingTransfer = transferProof;
            // World and startup markers are emitted ONLY when the native
            // ServerTickEvent sees restored HostEntity.onLoad registrations.
            return;

        }
        System.out.println("PL4_INSTALLED_BINARY_READY:" + sha);
    }
}
"""

TOML = """modLoader="javafml"
loaderVersion="[4,)"
license="MIT"
[[mods]]
modId="pl4_binary_probe"
version="1.0.0-probe"
displayName="Isolated PL4 installed-JAR smoke"
[[dependencies.pl4_binary_probe]]
modId="neoforge"
type="required"
versionRange="[21.1.250,21.2)"
ordering="NONE"
side="BOTH"
[[dependencies.pl4_binary_probe]]
modId="minecraft"
type="required"
versionRange="[1.21.1]"
ordering="NONE"
side="BOTH"
[[dependencies.pl4_binary_probe]]
modId="foundations_pl4"
type="required"
versionRange="[0,)"
ordering="AFTER"
side="BOTH"
"""


def digest(path):
    h = hashlib.sha256()
    with Path(path).open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def validate_binary(path, expected):
    path = Path(path)
    if not re.fullmatch(r"[0-9a-f]{64}", expected):
        raise ValueError("Expected a pinned 64-character SHA256")
    if not path.is_file() or path.is_symlink() or digest(path) != expected:
        raise ValueError("Installed JAR is absent, linked or not byte-identical")
    with zipfile.ZipFile(path) as jar:
        if jar.testzip() is not None:
            raise ValueError("Installed JAR ZIP integrity failure")
        names = jar.namelist()
        if len(names) != len(set(names)) or "META-INF/neoforge.mods.toml" not in names:
            raise ValueError("Installed JAR metadata missing/ambiguous")
        if "net/foundations/pl4/FoundationsPL4.class" not in names:
            raise ValueError("Installed JAR lacks actual PL4 production classes")
        meta = jar.read("META-INF/neoforge.mods.toml").decode("utf-8")
        if 'modId="foundations_pl4"' not in meta or 'versionRange="[1.21.1]"' not in meta:
            raise ValueError("Installed JAR has wrong mod or Minecraft target")


def prepare(checkout, jar, output, expected):
    checkout, jar, output = Path(checkout), Path(jar), Path(output)
    validate_binary(jar, expected)
    if output.exists():
        raise FileExistsError("Refusing to overwrite an existing isolated runtime: " + str(output))
    wrapper = checkout / "gradle" / "wrapper"
    for required in [checkout / "gradlew", wrapper / "gradle-wrapper.jar", wrapper / "gradle-wrapper.properties"]:
        if not required.is_file():
            raise FileNotFoundError("Missing pinned Gradle wrapper: " + str(required))
    output.mkdir(parents=True)
    entries = {
        "build.gradle": BUILD,
        "settings.gradle": "pluginManagement { repositories { gradlePluginPortal(); maven { url='https://maven.neoforged.net/releases' } } }\nrootProject.name = 'PL4-Installed-Binary-Probe'\n",
        "gradle.properties": "org.gradle.jvmargs=-Xmx768m\norg.gradle.daemon=false\norg.gradle.workers.max=1\n",
        JAVA_REL: JAVA,
        TOML_REL: TOML,
        "run/eula.txt": "eula=true\n",
        "run/server.properties": ("server-ip=127.0.0.1\nserver-port=25578\nonline-mode=false\nmax-players=1\nlevel-type=minecraft:flat\nview-distance=3\nsimulation-distance=3\n"
                                  "enable-rcon=true\nrcon.port=25579\nrcon.password=" + secrets.token_urlsafe(32) + "\n"),
    }
    for name, value in entries.items():
        target = output / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(value)
    shutil.copy2(checkout / "gradlew", output / "gradlew")
    shutil.copytree(wrapper, output / "gradle" / "wrapper")
    installed = output / "run" / "mods" / jar.name
    installed.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(jar, installed)
    if digest(installed) != expected:
        raise ValueError("Installed copy does not match pinned input bytes")
    # Vanilla rewrites server.properties/eula.txt during startup. They are
    # runtime evidence, never immutable source inputs; check safety keys later.
    protected = ["gradlew", "gradle/wrapper/gradle-wrapper.jar",
                 "gradle/wrapper/gradle-wrapper.properties",
                 *(name for name in entries if not name.startswith("run/"))]
    files = {name: digest(output / name) for name in protected}
    manifest = {"schema": 1, "expected_jar_sha256": expected, "installed_name": jar.name,
                "isolation": "Only standalone probe sources; PL4 production bytecode exclusively in run/mods",
                "files": files, "runtime_server_executed": False, "rc_publication_cleared": False}
    (output / "prepared-installation.json").write_text(json.dumps(manifest, indent=2, sort_keys=True) + "\n")
    return manifest


def verify(output, log):
    output, log = Path(output), Path(log)
    manifest = json.loads((output / "prepared-installation.json").read_text())
    if manifest.get("schema") != 1 or manifest.get("rc_publication_cleared") is not False:
        raise ValueError("Invalid installation manifest or unsafe acceptance status")
    expected = manifest["expected_jar_sha256"]
    files = manifest["files"]
    if not files or any(not (output / n).is_file() or digest(output / n) != sha for n, sha in files.items()):
        raise ValueError("Isolated probe project changed after installation preparation")
    server_props = {}
    for line in (output / "run/server.properties").read_text().splitlines():
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            server_props[key.strip()] = value.strip()
    if any(server_props.get(key) != value for key, value in {
            "server-ip": "127.0.0.1", "server-port": "25578",
            "online-mode": "false", "max-players": "1",
            "enable-rcon": "true", "rcon.port": "25579"}.items()):
        raise ValueError("Native runtime escaped isolated loopback/offline server configuration")
    if not re.fullmatch(r"[A-Za-z0-9_-]{32,}", server_props.get("rcon.password", "")):
        raise ValueError("Native runtime missing ephemeral authenticated local shutdown control")
    if "eula=true" not in (output / "run/eula.txt").read_text().splitlines():
        raise ValueError("Native runtime lacks explicit disposable-server EULA")
    sources = sorted(p.relative_to(output).as_posix() for p in (output / "src/main/java").rglob("*.java"))
    if sources != [JAVA_REL]:
        raise ValueError("Runtime contains compiled-from-source PL4 classes or unexpected Java sources")
    mods = sorted((output / "run/mods").glob("*.jar"))
    if len(mods) != 1 or mods[0].name != manifest["installed_name"]:
        raise ValueError("Runtime does not have exactly the pinned installed production JAR")
    validate_binary(mods[0], expected)
    if not log.is_file():
        raise ValueError("Missing actual native NeoForge server log")
    occurrences = [line.strip() for line in log.read_text(errors="replace").splitlines()
                   if MARKER in line]
    if len(occurrences) != 1 or not occurrences[0].endswith(MARKER + expected):
        raise ValueError("Real server never emitted the exact installed-mod startup attestation")
    proof = json.loads((output / "installed-server-proof.json").read_text())
    if (proof != {"schema": 1, "loaded_mod_id": MODID,
                  "server_started": True, "expected_jar_sha256": expected}):
        raise ValueError("Real ServerStartedEvent proof missing or mismatched")
    receipt = {"schema": 1, "installed_jar_sha256": expected,
               "installed_jar_name": mods[0].name, "loaded_mod_id": MODID,
               "server_started": True, "isolation_manifest_sha256": digest(output / "prepared-installation.json"),
               "server_log_sha256": digest(log), "probe_proof_sha256": digest(output / "installed-server-proof.json"),
               "installed_binary_server_smoke": "PASS",
               "installed_binary_client_executed": False,
               "released_to_candidate_world_upgrade_accepted": False,
               "multiplayer_accepted": False, "rc_publication_cleared": False}
    (output / "installed-server-receipt.json").write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n")
    return receipt


def _receive_exact(sock, count):
    chunks = []
    while count:
        chunk = sock.recv(count)
        if not chunk:
            raise ConnectionError("RCON response ended before a complete packet")
        chunks.append(chunk)
        count -= len(chunk)
    return b"".join(chunks)


def _rcon_send(sock, request_id, packet_type, message):
    data = struct.pack("<ii", request_id, packet_type) + message.encode("utf-8") + b"\0\0"
    sock.sendall(struct.pack("<i", len(data)) + data)


def _rcon_receive(sock):
    (size,) = struct.unpack("<i", _receive_exact(sock, 4))
    if size < 10 or size > 4096:
        raise ValueError("Invalid bounded local RCON reply frame")
    payload = _receive_exact(sock, size)
    if payload[-2:] != b"\0\0":
        raise ValueError("Malformed local RCON reply terminator")
    return struct.unpack("<ii", payload[:8])


def stop_server_rcon(properties):
    """Authenticated loopback /stop; Gradle runServer does not forward stdin."""
    config = {}
    for line in Path(properties).read_text().splitlines():
        if "=" in line and not line.startswith("#"):
            key, value = line.split("=", 1)
            config[key.strip()] = value.strip()
    if (config.get("server-ip") != "127.0.0.1"
            or config.get("enable-rcon") != "true"
            or config.get("rcon.port") != "25579"
            or not re.fullmatch(r"[A-Za-z0-9_-]{32,}", config.get("rcon.password", ""))):
        raise ValueError("Local authenticated RCON shutdown configuration was changed")
    with socket.create_connection(("127.0.0.1", 25579), timeout=5) as sock:
        sock.settimeout(5)
        _rcon_send(sock, 194, 3, config["rcon.password"])
        # Minecraft often sends an empty type-0 frame before type-2 auth.
        authenticated = False
        for _ in range(3):
            response_id, response_type = _rcon_receive(sock)
            if response_id == -1:
                raise PermissionError("Local RCON authentication rejected")
            if response_type == 2:
                if response_id != 194:
                    raise PermissionError("Local RCON authentication identity mismatch")
                authenticated = True
                break
            if response_id != 194 or response_type != 0:
                raise ValueError("Unexpected local RCON authentication frame")
        if not authenticated:
            raise PermissionError("Local RCON authentication never completed")
        _rcon_send(sock, 195, 2, "stop")


def _stop_with_retry(properties):
    failure = None
    for attempt in range(3):
        try:
            stop_server_rcon(properties)
            return
        except (OSError, ConnectionError) as exc:
            failure = exc
            if attempt < 2:
                time.sleep(2)
    raise RuntimeError("Authenticated loopback RCON stop unavailable") from failure


def run(output, seconds, world_phase=None):
    output = Path(output).resolve()
    manifest = json.loads((output / "prepared-installation.json").read_text())
    expected = manifest["expected_jar_sha256"]
    log_path = output / "installed-server.log"
    proof = output / "installed-server-proof.json"
    env = dict(os.environ, PL4_EXPECTED_JAR_SHA256=expected, PL4_INSTALLED_PROOF=str(proof))
    if world_phase is not None:
        if world_phase not in ("seed", "restore"):
            raise ValueError("Only explicit installed-JAR seed and restore world phases are supported")
        env.update(PL4_WORLD_PHASE=world_phase, PL4_WORLD_PROOF=str(output / "installed-world-proof.json"))
    with log_path.open("w") as log:
        process = subprocess.Popen(["bash", "gradlew", "--no-daemon", "--console=plain", "runServer"],
                                   cwd=output, env=env, stdin=subprocess.DEVNULL, stdout=log,
                                   stderr=subprocess.STDOUT, text=True, start_new_session=True)
        try:
            deadline = time.monotonic() + seconds
            ready = False
            while time.monotonic() < deadline:
                if process.poll() is not None:
                    break
                if log_path.exists() and MARKER + expected in log_path.read_text(errors="replace"):
                    ready = True
                    break
                time.sleep(3)
            if not ready:
                raise TimeoutError("Installed production-JAR NeoForge server did not attest startup; inspect retained log")
            # Gradle stdin is not the Minecraft console. Send an authenticated
            # loopback /stop, then require the native task to exit cleanly.
            _stop_with_retry(output / "run/server.properties")
            try:
                process.wait(timeout=90)
            except subprocess.TimeoutExpired as exc:
                raise RuntimeError("Native server did not exit after authenticated RCON stop") from exc
            if process.returncode != 0:
                raise RuntimeError("Native installed-JAR runner exited unsuccessfully after RCON stop")
        finally:
            if process.poll() is None:
                # Terminate the complete isolated Gradle/server process group;
                # otherwise the wrapper can leave an orphan game JVM.
                os.killpg(process.pid, signal.SIGTERM)
                try:
                    process.wait(timeout=20)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL)
                    process.wait()
    return verify(output, log_path)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    sub = p.add_subparsers(dest="action", required=True)
    a = sub.add_parser("prepare")
    a.add_argument("--checkout", required=True)
    a.add_argument("--jar", required=True)
    a.add_argument("--output", required=True)
    a.add_argument("--sha256", required=True)
    b = sub.add_parser("run")
    b.add_argument("--output", required=True)
    b.add_argument("--timeout-seconds", type=int, default=1500)
    b.add_argument("--world-phase", choices=("seed", "restore"))
    c = sub.add_parser("verify")
    c.add_argument("--output", required=True)
    c.add_argument("--log", required=True)
    args = p.parse_args()
    if args.action == "prepare":
        print(json.dumps(prepare(args.checkout, args.jar, args.output, args.sha256), sort_keys=True))
    elif args.action == "run":
        if not 60 <= args.timeout_seconds <= 1800:
            raise ValueError("Native execution timeout outside bounded interval")
        print(json.dumps(run(args.output, args.timeout_seconds, args.world_phase), sort_keys=True))
    else:
        print(json.dumps(verify(args.output, args.log), sort_keys=True))


if __name__ == "__main__":
    main()

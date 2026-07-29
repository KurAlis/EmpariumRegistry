package net.tommybutz.emparium_registry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class EmpireSavedData extends SavedData {

    private static final String DATA_NAME = "emparium_empires";

    private final List<EmpireData> empires = new ArrayList<>();


    private static final SavedData.Factory<EmpireSavedData> FACTORY =
            new SavedData.Factory<>(
                    EmpireSavedData::new,
                    EmpireSavedData::load,
                    null
            );

    public static EmpireSavedData get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(FACTORY, DATA_NAME);
    }

    // — NBT save/load —

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag empireList = new ListTag();
        for (EmpireData empire : empires) {
            empireList.add(empire.save());
        }
        tag.put("Empires", empireList);
        return tag;
    }

    public static EmpireSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        EmpireSavedData data = new EmpireSavedData();
        ListTag empireList = tag.getList("Empires", Tag.TAG_COMPOUND);
        for (int i = 0; i < empireList.size(); i++) {
            data.empires.add(EmpireData.load(empireList.getCompound(i)));
        }
        return data;
    }

    // — Empire management —

    public boolean createEmpire(EmpireData empire) {
        if (getEmpireByName(empire.getName()) != null) return false;
        if (getEmpireByPlayer(empire.getEmperorUUID()) != null) return false;
        empires.add(empire);
        setDirty();
        return true;
    }

    public boolean deleteEmpire(String name) {
        boolean removed = empires.removeIf(e -> e.getName().equalsIgnoreCase(name));
        if (removed) setDirty();
        return removed;
    }

    public boolean joinEmpire(String empireName, UUID playerUUID, String playerName) {
        if (getEmpireByPlayer(playerUUID) != null) return false;
        EmpireData empire = getEmpireByName(empireName);
        if (empire == null) return false;
        boolean added = empire.addMember(playerUUID, playerName);
        if (added) setDirty();
        return added;
    }

    public enum JoinResult {
        JOINED, REQUEST_SENT, ALREADY_IN_EMPIRE, ALREADY_REQUESTED, EMPIRE_FULL, EMPIRE_NOT_FOUND
    }

    public JoinResult requestJoin(String empireName, UUID playerUUID, String playerName) {
        if (getEmpireByPlayer(playerUUID) != null) return JoinResult.ALREADY_IN_EMPIRE;

        EmpireData empire = getEmpireByName(empireName);
        if (empire == null) return JoinResult.EMPIRE_NOT_FOUND;
        if (empire.isFull()) return JoinResult.EMPIRE_FULL;

        if (empire.isPublic()) {
            empire.addMember(playerUUID, playerName);
            setDirty();
            return JoinResult.JOINED;
        }

        if (empire.isPendingRequest(playerUUID)) return JoinResult.ALREADY_REQUESTED;
        empire.addPendingRequest(playerUUID, playerName);
        setDirty();
        return JoinResult.REQUEST_SENT;
    }

    public boolean acceptJoinRequest(String empireName, UUID targetUUID, ServerPlayer actingPlayer) {
        EmpireData empire = getEmpireByName(empireName);
        if (empire == null || !canManage(empire, actingPlayer)) return false;
        if (!empire.isPendingRequest(targetUUID)) return false;

        if (getEmpireByPlayer(targetUUID) != null || empire.isFull()) {
            empire.removePendingRequest(targetUUID);
            setDirty();
            return false;
        }

        String name = empire.getPendingRequestName(targetUUID);
        empire.removePendingRequest(targetUUID);
        boolean added = empire.addMember(targetUUID, name);
        if (added) setDirty();
        return added;
    }

    public boolean denyJoinRequest(String empireName, UUID targetUUID, ServerPlayer actingPlayer) {
        EmpireData empire = getEmpireByName(empireName);
        if (empire == null || !canManage(empire, actingPlayer)) return false;
        boolean removed = empire.removePendingRequest(targetUUID);
        if (removed) setDirty();
        return removed;
    }

    public boolean leaveEmpire(UUID playerUUID) {
        EmpireData empire = getEmpireByPlayer(playerUUID);
        if (empire == null) return false;
        if (empire.isEmperor(playerUUID)) return false;
        boolean removed = empire.removeMember(playerUUID);
        if (removed) setDirty();
        return removed;
    }

    public boolean kickMember(String empireName, UUID targetUUID, ServerPlayer actingPlayer) {
        EmpireData empire = getEmpireByName(empireName);
        if (empire == null || !canManage(empire, actingPlayer)) return false;
        if (empire.isEmperor(targetUUID)) return false;
        boolean removed = empire.removeMember(targetUUID);
        if (removed) setDirty();
        return removed;
    }

    // — Queries —

    public List<EmpireData> getAllEmpires() {
        return Collections.unmodifiableList(empires);
    }

    public EmpireData getEmpireByName(String name) {
        return empires.stream()
                .filter(e -> e.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public EmpireData getEmpireByPlayer(UUID uuid) {
        return empires.stream()
                .filter(e -> e.isMember(uuid))
                .findFirst()
                .orElse(null);
    }

    public boolean canManage(EmpireData empire, ServerPlayer player) {
        return empire.isEmperor(player.getUUID()) || player.hasPermissions(2);
    }

    public boolean deleteEmpireIfAuthorized(String name, ServerPlayer player) {
        EmpireData empire = getEmpireByName(name);
        if (empire == null || !canManage(empire, player)) return false;
        empires.removeIf(e -> e.getName().equalsIgnoreCase(name));
        setDirty();
        return true;
    }

    public boolean updateEmpireIfAuthorized(String originalName, String newName, String ideology,
                                            String capitalName, String flagUrl, String description,
                                            List<String> colonies, String flagRatio,boolean isPublic, ServerPlayer player) {
        EmpireData empire = getEmpireByName(originalName);
        if (empire == null || !canManage(empire, player)) return false;
        if (!originalName.equalsIgnoreCase(newName) && isNameTaken(newName)) return false;

        empire.setName(newName);
        empire.setIdeology(ideology);
        empire.setCapitalName(capitalName);
        empire.setFlagUrl(flagUrl);
        empire.setDescription(description);
        empire.setColonies(colonies);
        empire.setFlagRatio(EmpireData.FlagRatio.valueOf(flagRatio));
        empire.setPublic(isPublic);
        setDirty();
        return true;
    }

    public boolean isNameTaken(String name) {
        return getEmpireByName(name) != null;
    }
}
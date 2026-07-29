package net.tommybutz.emparium_registry.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EmpireData {

    private String name;
    private String ideology;
    private String description;
    private String capitalName;
    private String flagUrl;
    private List<String> screenshotUrls;
    private List<String> colonies;
    private UUID emperorUUID;
    private UUID empireId;
    private String emperorName;
    private List<UUID> memberUUIDs;
    private List<String> memberNames;
    private long registeredAtTick;
    private long foundedEpochMillis;
    private int claimCount;
    private boolean rewardClaimed;
    private boolean isPublic = true;
    private List<UUID> pendingRequestUUIDs = new ArrayList<>();
    private List<String> pendingRequestNames = new ArrayList<>();
    private List<Long> pendingRequestTimestamps = new ArrayList<>();
    private FlagRatio flagRatio = FlagRatio.SIXTEEN_NINE;


    private transient long totalPlaytimeTicks = 0L;

    public EmpireData(String name, String ideology, String capitalName,
                      UUID emperorUUID, String emperorName, long registeredAtTick) {
        this.name = name;
        this.ideology = ideology;
        this.description = "";
        this.capitalName = capitalName;
        this.flagUrl = "";
        this.screenshotUrls = new ArrayList<>();
        this.colonies = new ArrayList<>();
        this.emperorUUID = emperorUUID;
        this.emperorName = emperorName;
        this.memberUUIDs = new ArrayList<>();
        this.memberNames = new ArrayList<>();
        this.registeredAtTick = registeredAtTick;
        this.foundedEpochMillis = System.currentTimeMillis();
        this.claimCount = 0;
        this.rewardClaimed = false;
        this.empireId = UUID.randomUUID();
    }

    // — Serialization to NBT (saving) —

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putString("Name", name);
        tag.putString("Ideology", ideology);
        tag.putString("Description", description);
        tag.putString("CapitalName", capitalName);
        tag.putString("FlagUrl", flagUrl);
        tag.putString("EmperorUUID", emperorUUID.toString());
        tag.putString("EmperorName", emperorName);
        tag.putLong("RegisteredAt", registeredAtTick);
        tag.putLong("FoundedEpochMillis", foundedEpochMillis);
        tag.putInt("ClaimCount", claimCount);
        tag.putBoolean("RewardClaimed", rewardClaimed);
        tag.putBoolean("IsPublic", isPublic);
        tag.putString("EmpireId", empireId.toString());
        tag.putString("FlagRatio", flagRatio.name());

        long[] timestampArray = new long[pendingRequestTimestamps.size()];
        for (int i = 0; i < timestampArray.length; i++) timestampArray[i] = pendingRequestTimestamps.get(i);
        tag.putLongArray("PendingRequestTimestamps", timestampArray);

        ListTag pendingUUIDTag = new ListTag();
        for (UUID uuid : pendingRequestUUIDs) {
            pendingUUIDTag.add(StringTag.valueOf(uuid.toString()));
        }
        tag.put("PendingRequestUUIDs", pendingUUIDTag);

        ListTag pendingNameTag = new ListTag();
        for (String name : pendingRequestNames) {
            pendingNameTag.add(StringTag.valueOf(name));
        }
        tag.put("PendingRequestNames", pendingNameTag);

        ListTag screenshots = new ListTag();
        for (String url : screenshotUrls) {
            screenshots.add(StringTag.valueOf(url));
        }
        tag.put("Screenshots", screenshots);

        ListTag coloniesTag = new ListTag();
        for (String colony : colonies) {
            coloniesTag.add(StringTag.valueOf(colony));
        }
        tag.put("Colonies", coloniesTag);

        ListTag memberUUIDTag = new ListTag();
        for (UUID uuid : memberUUIDs) {
            memberUUIDTag.add(StringTag.valueOf(uuid.toString()));
        }
        tag.put("MemberUUIDs", memberUUIDTag);

        ListTag memberNameTag = new ListTag();
        for (String name : memberNames) {
            memberNameTag.add(StringTag.valueOf(name));
        }
        tag.put("MemberNames", memberNameTag);

        return tag;
    }

    public enum FlagRatio {
        SIXTEEN_NINE(16f / 9f),
        ONE_ONE(1f),
        TWO_ONE(2f);

        public final float widthToHeight;

        FlagRatio(float widthToHeight) {
            this.widthToHeight = widthToHeight;
        }
    }

    // — Deserialization from NBT (loading) —

    public static EmpireData load(CompoundTag tag) {
        EmpireData data = new EmpireData(
                tag.getString("Name"),
                tag.getString("Ideology"),
                tag.getString("CapitalName"),
                UUID.fromString(tag.getString("EmperorUUID")),
                tag.getString("EmperorName"),
                tag.getLong("RegisteredAt")
        );

        data.description = tag.getString("Description");
        data.flagUrl = tag.getString("FlagUrl");
        data.claimCount = tag.getInt("ClaimCount");
        data.rewardClaimed = tag.getBoolean("RewardClaimed");
        data.foundedEpochMillis = tag.contains("FoundedEpochMillis")
                ? tag.getLong("FoundedEpochMillis")
                : System.currentTimeMillis();
        data.isPublic = !tag.contains("IsPublic") || tag.getBoolean("IsPublic");
        data.empireId = tag.contains("EmpireId")
                ? UUID.fromString(tag.getString("EmpireId"))
                : UUID.randomUUID();
        data.flagRatio = tag.contains("FlagRatio")
                ? FlagRatio.valueOf(tag.getString("FlagRatio"))
                : FlagRatio.SIXTEEN_NINE;

        if (tag.contains("PendingRequestTimestamps")) {
            for (long t : tag.getLongArray("PendingRequestTimestamps")) {
                data.pendingRequestTimestamps.add(t);
            }
        } else {
            for (int i = 0; i < data.pendingRequestUUIDs.size(); i++) {
                data.pendingRequestTimestamps.add(System.currentTimeMillis());
            }
        }

        ListTag pendingUUIDTag = tag.getList("PendingRequestUUIDs", Tag.TAG_STRING);
        for (int i = 0; i < pendingUUIDTag.size(); i++) {
            data.pendingRequestUUIDs.add(UUID.fromString(pendingUUIDTag.getString(i)));
        }

        ListTag pendingNameTag = tag.getList("PendingRequestNames", Tag.TAG_STRING);
        for (int i = 0; i < pendingNameTag.size(); i++) {
            data.pendingRequestNames.add(pendingNameTag.getString(i));
        }

        ListTag screenshots = tag.getList("Screenshots", Tag.TAG_STRING);
        for (int i = 0; i < screenshots.size(); i++) {
            data.screenshotUrls.add(screenshots.getString(i));
        }

        ListTag coloniesTag = tag.getList("Colonies", Tag.TAG_STRING);
        for (int i = 0; i < coloniesTag.size(); i++) {
            data.colonies.add(coloniesTag.getString(i));
        }

        ListTag memberUUIDTag = tag.getList("MemberUUIDs", Tag.TAG_STRING);
        for (int i = 0; i < memberUUIDTag.size(); i++) {
            data.memberUUIDs.add(UUID.fromString(memberUUIDTag.getString(i)));
        }

        ListTag memberNameTag = tag.getList("MemberNames", Tag.TAG_STRING);
        for (int i = 0; i < memberNameTag.size(); i++) {
            data.memberNames.add(memberNameTag.getString(i));
        }

        return data;
    }

    // — Getters —

    public String getName() { return name; }
    public String getIdeology() { return ideology; }
    public String getDescription() { return description; }
    public String getCapitalName() { return capitalName; }
    public String getFlagUrl() { return flagUrl; }
    public List<String> getScreenshotUrls() { return screenshotUrls; }
    public List<String> getColonies() { return colonies; }
    public UUID getEmperorUUID() { return emperorUUID; }
    public String getEmperorName() { return emperorName; }
    public List<UUID> getMemberUUIDs() { return memberUUIDs; }
    public List<String> getMemberNames() { return memberNames; }
    public long getRegisteredAtTick() { return registeredAtTick; }
    public long getFoundedEpochMillis() { return foundedEpochMillis; }
    public int getClaimCount() { return claimCount; }
    public boolean isRewardClaimed() { return rewardClaimed; }
    public int getMemberCount() { return memberUUIDs.size() + 1; }
    public long getTotalPlaytimeTicks() { return totalPlaytimeTicks; }
    public boolean isPublic() { return isPublic; }
    public List<UUID> getPendingRequestUUIDs() { return pendingRequestUUIDs; }
    public List<String> getPendingRequestNames() { return pendingRequestNames; }
    public UUID getEmpireId() { return empireId; }
    public FlagRatio getFlagRatio() { return flagRatio; }

    // — Setters —

    public void setName(String name) { this.name = name; }
    public void setIdeology(String ideology) { this.ideology = ideology; }
    public void setDescription(String description) { this.description = description; }
    public void setCapitalName(String capitalName) { this.capitalName = capitalName; }
    public void setFlagUrl(String flagUrl) { this.flagUrl = flagUrl; }
    public void setClaimCount(int claimCount) { this.claimCount = claimCount; }
    public void setRewardClaimed(boolean rewardClaimed) { this.rewardClaimed = rewardClaimed; }
    public void setColonies(List<String> colonies) { this.colonies = new ArrayList<>(colonies); }
    public void setTotalPlaytimeTicks(long ticks) { this.totalPlaytimeTicks = ticks; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    public void setFlagRatio(FlagRatio flagRatio) { this.flagRatio = flagRatio; }

    // — Member management —

    public boolean addMember(UUID uuid, String name) {
        if (memberUUIDs.size() >= 4) return false;
        if (memberUUIDs.contains(uuid)) return false;
        memberUUIDs.add(uuid);
        memberNames.add(name);
        return true;
    }

    public boolean removeMember(UUID uuid) {
        int index = memberUUIDs.indexOf(uuid);
        if (index == -1) return false;
        memberUUIDs.remove(index);
        memberNames.remove(index);
        return true;
    }

    public boolean isMember(UUID uuid) {
        return memberUUIDs.contains(uuid) || emperorUUID.equals(uuid);
    }

    public boolean isEmperor(UUID uuid) {
        return emperorUUID.equals(uuid);
    }

    public boolean isFull() {
        return memberUUIDs.size() >= 4;
    }

    public boolean isPendingRequest(UUID uuid) {
        return pendingRequestUUIDs.contains(uuid);
    }

    public void addPendingRequest(UUID uuid, String name) {
        if (!pendingRequestUUIDs.contains(uuid)) {
            pendingRequestUUIDs.add(uuid);
            pendingRequestNames.add(name);
            pendingRequestTimestamps.add(System.currentTimeMillis());
        }
    }

    public boolean removePendingRequest(UUID uuid) {
        int index = pendingRequestUUIDs.indexOf(uuid);
        if (index == -1) return false;
        pendingRequestUUIDs.remove(index);
        pendingRequestNames.remove(index);
        pendingRequestTimestamps.remove(index);
        return true;
    }

    public String getPendingRequestName(UUID uuid) {
        int index = pendingRequestUUIDs.indexOf(uuid);
        return index == -1 ? null : pendingRequestNames.get(index);
    }

    public List<Long> getPendingRequestTimestamps() { return pendingRequestTimestamps; }

    public long getPendingRequestTimestamp(UUID uuid) {
        int index = pendingRequestUUIDs.indexOf(uuid);
        return index == -1 ? -1L : pendingRequestTimestamps.get(index);
    }

    public void addScreenshot(String url) {
        if (screenshotUrls.size() < 5) screenshotUrls.add(url);
    }

    public void removeScreenshot(int index) {
        if (index >= 0 && index < screenshotUrls.size()) {
            screenshotUrls.remove(index);
        }
    }
}
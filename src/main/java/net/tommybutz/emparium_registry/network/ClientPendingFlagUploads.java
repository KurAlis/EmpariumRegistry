package net.tommybutz.emparium_registry.network;

import net.tommybutz.emparium_registry.client.gui.FormAndEditEmpireScreen;
import java.util.HashMap;
import java.util.Map;

public class ClientPendingFlagUploads {

    private static final Map<String, byte[]> pending = new HashMap<>();

    public static void queue(String empireName, byte[] pngBytes) {
        pending.put(empireName.toLowerCase(), pngBytes);
    }


    public static void checkAndFire(net.tommybutz.emparium_registry.data.EmpireData empire, java.util.UUID localPlayerUUID) {
        String key = empire.getName().toLowerCase();
        byte[] bytes = pending.get(key);
        if (bytes == null) return;
        if (!empire.isEmperor(localPlayerUUID)) return;

        pending.remove(key);
        FormAndEditEmpireScreen.sendFlagChunks(empire.getName(), bytes);
    }
}
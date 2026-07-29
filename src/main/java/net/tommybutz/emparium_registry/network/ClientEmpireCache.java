package net.tommybutz.emparium_registry.network;

import net.tommybutz.emparium_registry.data.EmpireData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ClientEmpireCache {

    private static List<EmpireData> empires = new ArrayList<>();

    public static void setEmpires(List<EmpireData> received) {
        empires = new ArrayList<>(received);
    }

    public static List<EmpireData> getEmpires() {
        return Collections.unmodifiableList(empires);
    }

    public static EmpireData getEmpireByPlayer(UUID uuid) {
        return empires.stream()
                .filter(e -> e.isMember(uuid))
                .findFirst()
                .orElse(null);
    }

    public static EmpireData getEmpireByName(String name) {
        return empires.stream()
                .filter(e -> e.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public static void clear() {
        empires.clear();
    }
}
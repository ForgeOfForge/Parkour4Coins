package forge.Parkour4Coins;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.util.BlockPos;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ParkourData {
    private List<String> parkourTimesDB = new ArrayList<>();
    private List<String> parkourRecordsDB = new ArrayList<>();
    private int selectedSound = 1;

    public List<String> getParkourTimesDB() {
        return parkourTimesDB;
    }

    public List<String> getParkourRecordsDB() {
        return parkourRecordsDB;
    }

    // Serialize the data to JSON
    public void saveToFile(String filename) {
        try (Writer writer = new FileWriter(filename)) {
            Gson gson = new GsonBuilder().create();
            gson.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Deserialize the data from JSON
    public static ParkourData loadFromFile(String filename) {
        try (Reader reader = new FileReader(filename)) {
            Gson gson = new GsonBuilder().create();
            return gson.fromJson(reader, ParkourData.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ParkourData(); // Return a new instance if loading fails
    }

    // Deserialize the selectedSound from JSON
    public static int loadSelectedSoundFromFile(String filename) {
        try (Reader reader = new FileReader(filename)) {
            Gson gson = new GsonBuilder().create();
            return gson.fromJson(reader, Integer.class);
        } catch (IOException e) {
            // If the file doesn't exist, create it with a default value
            saveSelectedSoundToFile(filename, 1); // Set the default value (e.g., 1)
            return 1; // Return the default value
        }
    }

    public static void saveSelectedSoundToFile(String filename, int selectedSound) {
        try (Writer writer = new FileWriter(filename)) {
            Gson gson = new GsonBuilder().create();
            gson.toJson(selectedSound, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Static inner class for GhostDataEntry
    public static class GhostDataEntry {
        private BlockPos position;
        private long timestamp;

        public GhostDataEntry(BlockPos position, long timestamp) {
            this.position = position;
            this.timestamp = timestamp;
        }

        public BlockPos getPosition() {
            return position;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    public void saveGhostPositionsToFile(String courseName, List<GhostDataEntry> ghostDataEntries, String filename) {
        try (Writer writer = new FileWriter(filename)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            JsonObject jsonCourseData = new JsonObject();
            jsonCourseData.addProperty("courseName", courseName);

            JsonArray jsonGhostData = new JsonArray();
            for (GhostDataEntry entry : ghostDataEntries) {
                JsonObject jsonDataEntry = new JsonObject();
                jsonDataEntry.addProperty("x", entry.getPosition().getX());
                jsonDataEntry.addProperty("y", entry.getPosition().getY());
                jsonDataEntry.addProperty("z", entry.getPosition().getZ());
                jsonDataEntry.addProperty("timestamp", entry.getTimestamp());

                jsonGhostData.add(jsonDataEntry);
            }

            jsonCourseData.add("ghostData", jsonGhostData);

            gson.toJson(jsonCourseData, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Add this method to load ghost data
    public List<GhostDataEntry> loadGhostDataFromFile(String parkourName, String filename) {
        try (Reader reader = new FileReader(filename)) {
            Gson gson = new GsonBuilder().create();
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);

            // Check if the JSON object contains the specified courseName
            if (jsonObject.has("courseName") && jsonObject.get("courseName").getAsString().equals(parkourName)) {
                JsonArray jsonGhostData = jsonObject.getAsJsonArray("ghostData");
                List<GhostDataEntry> ghostData = new ArrayList<>();

                // Deserialize the ghost data entries
                for (int i = 0; i < jsonGhostData.size(); i++) {
                    JsonObject jsonDataEntry = jsonGhostData.get(i).getAsJsonObject();
                    int x = jsonDataEntry.get("x").getAsInt();
                    int y = jsonDataEntry.get("y").getAsInt();
                    int z = jsonDataEntry.get("z").getAsInt();
                    long timestamp = jsonDataEntry.get("timestamp").getAsLong();

                    ghostData.add(new GhostDataEntry(new BlockPos(x, y, z), timestamp));
                }

                return ghostData;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>(); // Return an empty list if loading fails or courseName doesn't match
    }
}
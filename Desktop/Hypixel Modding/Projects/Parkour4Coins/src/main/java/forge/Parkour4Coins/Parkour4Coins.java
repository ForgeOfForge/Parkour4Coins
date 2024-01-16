package forge.Parkour4Coins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.apache.commons.lang3.time.DurationFormatUtils;

import forge.Parkour4Coins.ParkourData.GhostDataEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mod(
   modid = Parkour4Coins.MODID,
   version = Parkour4Coins.VERSION,
   acceptedMinecraftVersions = "[1.8.9]"
)
public class Parkour4Coins {
   public static final String MODID = "Parkour4Coins";
   public static final String VERSION = "1.0";

   private long startTime;
   private long finishTime;
   private long timeElapsed;
   private int checkpointCounter;
   private int messageFlag;
   private int recordFound;
   private int speedMeasureDelay = 0;
   private String parkourName = "";
   private String islandOwner = "";
   
   private boolean ghostRecord = false;
   private boolean ghostPlayback = false;
   private int ghostMeasureDelay = 0;
   private List<BlockPos> ghostPositions = new ArrayList<>();
   private List<Long> ghostTimestamps = new ArrayList<>();
   private List<GhostDataEntry> bestGhostData = new ArrayList<>();
   List<GhostDataEntry> ghostDataEntries = new ArrayList<>();
   private int ghostPlaybackIndex = 0; // Add this variable to keep track of the current playback index
   private int ghostPlaybackDelay = 0;




   private StringBuilder activeCourse = new StringBuilder();
   private List<String> parkourTimesDB = new ArrayList<String>();
   private List<String> parkourRecordsDB = new ArrayList<String>();

   private ParkourData parkourData;

   private int startedParkourCounter = 0;
   private int selectedSound = 1;

   private long timeDifference = 0;
   private double playerSpeed = 0;
   private double playerSpeedPercentage = 0.0;
   List<Double> speedMeasurements = new ArrayList<>();
   private CustomSidebarElement customSidebarElement;
   
   @EventHandler
   public void init(FMLInitializationEvent event) {
      MinecraftForge.EVENT_BUS.register(this);
      customSidebarElement = new CustomSidebarElement();

      // Load data from a file at the start of the game
      parkourData = parkourData.loadFromFile("mods/Parkour4Coins/parkour_data.json");
      selectedSound = parkourData.loadSelectedSoundFromFile("mods/Parkour4Coins/selected_sound.json");
      System.out.println("Loaded selectedSound: " + selectedSound);
   }

   // Save data to the file when needed
   private void saveDataToFile() {
       parkourData.saveToFile("mods/Parkour4Coins/parkour_data.json");
   }

   @SubscribeEvent
   public void onChat(ClientChatReceivedEvent event) {
       String msg = event.message.getUnformattedText();

       // Show parkourData in chat
       for (String entry : parkourData.getParkourTimesDB()) {
           //Minecraft.getMinecraft().thePlayer.addChatComponentMessage(new ChatComponentText(entry));
       }

       if (msg.contains("Started parkour") && !msg.contains("]") || msg.contains("Reset time for parkour") && !msg.contains("]")) {
    	   // Start ghost recording in case run is a personal best
    	   List<BlockPos> ghostPositions = new ArrayList<>();
    	   List<BlockPos> ghostTimestamps = new ArrayList<>();
    	   ghostPlayback = false;
    	   ghostRecord = true;
    	   
           EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
           String islandOwnerBossbar = BossStatus.bossName;
           // Check if the boss bar name is not empty or null
           if (islandOwnerBossbar != null && !islandOwnerBossbar.isEmpty()) {
               String[] islandOwnerBossbarSplit = islandOwnerBossbar.split(" ");
               String[] filteredWords = Arrays.stream(islandOwnerBossbarSplit)
                       .filter(word -> word.contains("'"))
                       .toArray(String[]::new);
               islandOwner = String.join(" ", filteredWords);
               islandOwner = islandOwner.replace("'s", "");
               
               // Now you can use the 'islandOwner' variable as needed
           } else {
        	   islandOwner = "?";
           }

           event.setCanceled(true);
           resetActiveCourse();
           playCheckpointSound(selectedSound);
           startTime = System.currentTimeMillis();
           checkpointCounter = 0;

           String parkourNameSpace = msg.replace("Started parkour", "").replace("Reset time for parkour", "");
           String parkourNameNoContext = parkourNameSpace.replace(" ", "");
           parkourName = islandOwner + "-" + parkourNameNoContext;
           activeCourse.append(parkourName).append(", ");
           displayMessage(EnumChatFormatting.GOLD + "Starting " + parkourName);
           // Display best time details at start if available
           for (String record : parkourData.getParkourRecordsDB()) {
               String[] parts = record.split(",");
               String recordCourseName = parts[0].trim();
               if (recordCourseName.equals(parkourName)){
            	   // If record found, display best splits and start ghost
                   String[] recordParts = record.split(", ");
                   int counter = 1;
                   StringBuilder result = new StringBuilder();
                   for (int i = 1; i < recordParts.length; i++) {
                       String entry = recordParts[i];
                       String renamedEntry = "CP" + counter + ": ";
                       result.append(renamedEntry).append(entry).append(", ");
                       counter++;
                   }
                   String lastEntry = recordParts[recordParts.length - 1];
                   result.replace(result.lastIndexOf("CP"), result.length(), "Finish: " + lastEntry);

                   displayMessage(EnumChatFormatting.GOLD + "Personal Best: " + result.toString());
                   
                   // If record found, load ghost data
                   bestGhostData = parkourData.loadGhostDataFromFile(parkourName, "mods/Parkour4Coins/ghost_positions.json");
            	   ghostPlayback = true;
            	   displayMessage(EnumChatFormatting.GRAY + "" + EnumChatFormatting.ITALIC + "Ghost playback started");
               }
           }
           startedParkourCounter++;
           if (startedParkourCounter >= 10){
               selectedSound++;
               startedParkourCounter = 0;
               parkourData.saveSelectedSoundToFile("mods/Parkour4Coins/selected_sound.json", selectedSound);
           }

           if (selectedSound > 3) {
               selectedSound = 1;
           }
       }

       if (msg.contains("Cancelled parkour!") && !msg.contains("]")) {
    	   ghostRecord = false;
    	   ghostPositions.clear();
    	   ghostTimestamps.clear();
    	   ghostDataEntries.clear();
           event.setCanceled(true);
           displayMessage(EnumChatFormatting.RED + "Parkour cancelled - Don't fly or use item abilities");
           startedParkourCounter = 0;
       }

       if (msg.contains("Reached checkpoint #") && !msg.contains("]")) {
           event.setCanceled(true);
           startedParkourCounter = 0;
           messageFlag = 0;
           playCheckpointSound(selectedSound);
           checkpointCounter++;
           finishTime = System.currentTimeMillis();
           timeElapsed = finishTime - startTime;
           String formattedTime = DurationFormatUtils.formatDuration(timeElapsed, "mm:ss.SSS");
           activeCourse.append(formattedTime).append(", ");

           for (String line : parkourData.getParkourRecordsDB()) {
               String[] parkourTimesDBComponents = line.split(",");
               String courseNameString = activeCourse.toString();
               String[] courseNameParts = courseNameString.split(",");

               if (parkourTimesDBComponents[0].contains(courseNameParts[0])) {
                   String[] activeTime = formattedTime.split("[:.]");
                   String activeTimeCompare = String.join("", activeTime);
                   String trimmedActiveTimeCompare = activeTimeCompare.trim();
                   int activeTimeCompareInt = Integer.parseInt(trimmedActiveTimeCompare);

                   String[] recordTime = parkourTimesDBComponents[checkpointCounter].split("[:.]");
                   String recordTimeCompare = String.join("", recordTime);
                   String trimmedRecordTimeCompare = recordTimeCompare.trim();
                   int recordTimeCompareInt = Integer.parseInt(trimmedRecordTimeCompare);

                   if (activeTimeCompareInt < recordTimeCompareInt) {
                       timeDifference = activeTimeCompareInt - recordTimeCompareInt;
                       displayMessage(EnumChatFormatting.GOLD + " > CP " + checkpointCounter + " || " + formattedTime + " (" + timeDifference + ")");
                       messageFlag = 1;
                   }
                   if (activeTimeCompareInt >= recordTimeCompareInt) {
                       timeDifference = activeTimeCompareInt - recordTimeCompareInt;
                       displayMessage(EnumChatFormatting.GREEN + " > CP " + checkpointCounter + " || " + formattedTime + " (+" + timeDifference + ")");
                       messageFlag = 1;
                   }
                   if (messageFlag == 0) {
                       displayMessage(EnumChatFormatting.GREEN + " > CP " + checkpointCounter + " || " + formattedTime + "(+0)");
                   }
               }
           }
       }

       if (msg.contains("Finished parkour") && !msg.contains("]")) {
    	   ghostRecord = false;
    	   
           messageFlag = 0;
           recordFound = 0;
           playCheckpointSound(selectedSound);
           finishTime = System.currentTimeMillis();
           timeElapsed = finishTime - startTime;
           String formattedTime = DurationFormatUtils.formatDuration(timeElapsed, "mm:ss.SSS");
           activeCourse.append(formattedTime);
           parkourData.getParkourTimesDB().add(activeCourse.toString());

           for (String line : parkourData.getParkourRecordsDB()) {
               String[] parkourTimesDBComponents = line.split(",");
               String courseNameString = activeCourse.toString();
               String[] courseNameParts = courseNameString.split(",");

               if (parkourTimesDBComponents[0].contains(courseNameParts[0])) {
                   recordFound = 1;
                   String[] activeTime = formattedTime.split("[:.]");
                   String activeTimeCompare = String.join("", activeTime);
                   String trimmedActiveTimeCompare = activeTimeCompare.trim();
                   int activeTimeCompareInt = Integer.parseInt(trimmedActiveTimeCompare);

                   String[] recordTime = parkourTimesDBComponents[parkourTimesDBComponents.length - 1].split("[:.]");
                   String recordTimeCompare = String.join("", recordTime);
                   String trimmedRecordTimeCompare = recordTimeCompare.trim();
                   int recordTimeCompareInt = Integer.parseInt(trimmedRecordTimeCompare);
            	   timeDifference = activeTimeCompareInt - recordTimeCompareInt;

                   if (activeTimeCompareInt < recordTimeCompareInt) {
                       playCompleteSound(selectedSound);
                       displayMessage(EnumChatFormatting.GOLD + "Personal Best! " + formattedTime + " (" + timeDifference + ")");
                       Minecraft.getMinecraft().ingameGUI.displayTitle(EnumChatFormatting.GOLD + "Personal Best!", formattedTime, 20, 60, 20);
                       messageFlag = 1;
                       parkourData.getParkourRecordsDB().remove(line);
                       parkourData.getParkourRecordsDB().add(activeCourse.toString());
                       // Save ghost data
                       for (int i = 0; i < ghostPositions.size(); i++) {
                           BlockPos position = ghostPositions.get(i);
                           long timestamp = ghostTimestamps.get(i);
                           GhostDataEntry ghostDataEntry = new GhostDataEntry(position, timestamp);
                           ghostDataEntries.add(ghostDataEntry);
                       }

                       // Call the method with the correct arguments
                       parkourData.saveGhostPositionsToFile(parkourName, ghostDataEntries, "mods/Parkour4Coins/ghost_positions.json");
                	   // Clear for next write
                       ghostPositions.clear();
                	   ghostTimestamps.clear();
                	   ghostDataEntries.clear();
                   }
               }
           }

           if (messageFlag == 0) {
               displayMessage(EnumChatFormatting.GREEN + "Course completed in (realtime): " + formattedTime);
           }

           if (recordFound == 0) {
               playCompleteSound(selectedSound);
               displayMessage(EnumChatFormatting.GOLD + "Time to beat: " + formattedTime);
               parkourData.getParkourRecordsDB().add(activeCourse.toString());
               // Save ghost data
               for (int i = 0; i < ghostPositions.size(); i++) {
                   BlockPos position = ghostPositions.get(i);
                   long timestamp = ghostTimestamps.get(i);
                   GhostDataEntry ghostDataEntry = new GhostDataEntry(position, timestamp);
                   ghostDataEntries.add(ghostDataEntry);
               }

               // Call the method with the correct arguments
               parkourData.saveGhostPositionsToFile(parkourName, ghostDataEntries, "mods/Parkour4Coins/ghost_positions.json");
        	   // Clear for next write
               ghostPositions.clear();
        	   ghostTimestamps.clear();
        	   ghostDataEntries.clear();
           }

           // Save data to file after updating
           saveDataToFile();
       }
   }

   @SubscribeEvent
   public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
       if (event.type == RenderGameOverlayEvent.ElementType.ALL) {
           customSidebarElement.render();
       }
   }

   private class CustomSidebarElement {
       @SideOnly(Side.CLIENT)
       public void render() {
           Minecraft mc = Minecraft.getMinecraft();
           ScaledResolution scaledResolution = new ScaledResolution(mc);
           int width = scaledResolution.getScaledWidth();
           int height = scaledResolution.getScaledHeight();
           int yOffset = 0; // Adjust this value to set the Y position of your sidebar
           Gui.drawRect(width - 50, yOffset, width, yOffset + 21, 0x99000000); // Sidebar background color

           String timeDifferenceString = (timeDifference >= 0) ? "+" + timeDifference : String.valueOf(timeDifference);
           String speedString = String.format("%.0f%%", Math.ceil(playerSpeedPercentage));

           mc.fontRendererObj.drawStringWithShadow(EnumChatFormatting.GOLD + timeDifferenceString, width - 50, yOffset + 2, 0xFFFFFF);
           if (timeDifferenceString.contains("+")){
               mc.fontRendererObj.drawStringWithShadow(EnumChatFormatting.RED + timeDifferenceString, width - 50, yOffset + 2, 0xFFFFFF);
           }
           mc.fontRendererObj.drawStringWithShadow(EnumChatFormatting.GRAY + speedString, width - 50, yOffset + 12, 0xFFFFFF);
       }
   }

   private void resetActiveCourse() {
      activeCourse.setLength(0);
   }

   private void playCheckpointSound(int selectedSound) {
        switch (selectedSound) {
            case 1:
                Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation(MODID, "vanilla_checkpoint"), 1.0F));
                break;
            case 2:
                Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation(MODID, "sonic_checkpoint"), 1.0F));
                break;
            case 3:
                Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation(MODID, "mario_checkpoint"), 1.0F));
                break;
            default:
                Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation(MODID, "vanilla_checkpoint"), 1.0F));
                break;
        }
    }

   private void playCompleteSound(int selectedSound) {
        switch (selectedSound) {
        case 1:
            Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation(MODID, "vanilla_complete"), 1.0F));
            break;
        case 2:
            Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation(MODID, "sonic_complete"), 1.0F));
            break;
        default:
        	Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation(MODID, "vanilla_complete"), 1.0F));
            break;
    }	}

   private void displayMessage(String message) {
      Minecraft.getMinecraft().thePlayer.addChatComponentMessage(new ChatComponentText(message));
   }

   public static class TimeList {
      public static List<String> stringList = new ArrayList<String>();
   }
   
   // Calculate and display player speed
   private BlockPos startPos;
   private long startPosTime;

   public void startMeasurement() {
	    // Record the player's position and time at the start of measurement
	    this.startPos = Minecraft.getMinecraft().thePlayer.getPosition();
	    this.startPosTime = System.nanoTime(); // Use System.nanoTime() for more precise timing
	}

	public double stopMeasurement() {
	    // Record the player's position and time at the end of measurement
	    BlockPos endPos = Minecraft.getMinecraft().thePlayer.getPosition();
	    long endTime = System.nanoTime(); // Use System.nanoTime() for more precise timing

	    // Calculate the time elapsed in seconds (using nanoseconds)
	    double nanosecondsElapsed = endTime - startPosTime;
	    double secondsElapsed = nanosecondsElapsed / 1e9; // Convert nanoseconds to seconds

	    // Calculate the distance between positions
	    double deltaX = endPos.getX() - startPos.getX();
	    double deltaY = endPos.getY() - startPos.getY();
	    double deltaZ = endPos.getZ() - startPos.getZ();

	    double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

	    // Calculate the player's movement speed in blocks per second
	    double playerSpeed = distance / secondsElapsed;

	    return playerSpeed;
	}
   @SubscribeEvent
   public void onClientTick(TickEvent.ClientTickEvent event) {
	   // Find player speed for updating the GUI
	   EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
	   if (player != null) {
		   speedMeasureDelay++;
		   if (speedMeasureDelay == 1){
	    	   startMeasurement();
		   }
		   if (speedMeasureDelay > 20){
			   speedMeasureDelay = 0;
			   playerSpeed = stopMeasurement();
			   speedMeasurements.add(playerSpeed);
			   if (speedMeasurements.size() >= 2) {
				   double sum = 0;
				   for (Double measurement : speedMeasurements) {
				       sum += measurement;
				       String doubleAsString = String.format("%.2f", measurement);
				   }
			        
				   double averageSpeed = sum / speedMeasurements.size();
				   playerSpeedPercentage = (averageSpeed / 4.317) * 100;
				   speedMeasurements = new ArrayList<>();
			   }
		   }
	   }
	   // Save player position data every half second for best time ghost
	   if (player != null) {
		    ghostMeasureDelay++;
		    if (ghostMeasureDelay >= 10 && ghostRecord) {
		        ghostMeasureDelay = 0;
		        BlockPos ghostCurrentPosition = player.getPosition();
		        ghostPositions.add(ghostCurrentPosition);
		        long currentTime = System.currentTimeMillis();
		        ghostTimestamps.add(currentTime); // Move this line inside the if block
		    }
	   }
	   // Playback ghost at parkour start
	   if (player != null) {
	       if (ghostPlayback && !bestGhostData.isEmpty() && ghostPlaybackIndex < bestGhostData.size()) {
	    	   ghostPlaybackDelay = 0;
	           ParkourData.GhostDataEntry ghostDataEntry = bestGhostData.get(ghostPlaybackIndex);
	           long currentTime = System.currentTimeMillis();

	           if (ghostTimestamps.size() > ghostPlaybackIndex + 1) {
	               long timeDifference = ghostTimestamps.get(ghostPlaybackIndex + 1) - ghostTimestamps.get(ghostPlaybackIndex);
	               	
	               displayMessage(Long.toString(timeDifference));
	               if (currentTime >= ghostTimestamps.get(ghostPlaybackIndex) && currentTime < ghostTimestamps.get(ghostPlaybackIndex + 1)) {
	                   double progress = (double) (currentTime - ghostTimestamps.get(ghostPlaybackIndex)) / (double) timeDifference;

	                   double x = ghostDataEntry.getPosition().getX();
	                   double y = ghostDataEntry.getPosition().getY();
	                   double z = ghostDataEntry.getPosition().getZ();

	                   // Spawn the particle at the interpolated position
	                   spawnParticlesAtPosition(x, y, z);
	                   displayMessage("Spawning Particle at: " + x + " | " + y + " | " + z);

	                   ghostPlaybackIndex++;
	               } else if (currentTime >= ghostTimestamps.get(ghostPlaybackIndex + 1)) {
	                   // Handle delay between particles
	                   long delay = ghostTimestamps.get(ghostPlaybackIndex + 1) - ghostTimestamps.get(ghostPlaybackIndex);
	                   ghostPlaybackDelay += delay / 10; // Add delay between particles
	                   ghostPlaybackIndex++; // Move to the next ghost data entry
	               }
	           }
	       }
	   }
   }
   
   private void spawnParticlesAtPosition(double x, double y, double z) {
	    displayMessage("Rendering particles");
	    World world = Minecraft.getMinecraft().theWorld;
	    EffectRenderer particleManager = Minecraft.getMinecraft().effectRenderer;

	    EnumParticleTypes particleType = EnumParticleTypes.CRIT; // Change this to the desired particle type
	    int numParticles = 10; // Number of particles to spawn
	    double yOffset = 1.0; // Offset above the target position

	    for (int i = 0; i < numParticles; i++) {
	        double offsetX = world.rand.nextDouble() * 0.6 - 0.3; // Random X offset
	        double offsetY = world.rand.nextDouble() * 0.6 - 0.3; // Random Y offset
	        double offsetZ = world.rand.nextDouble() * 0.6 - 0.3; // Random Z offset

	        double motionX = 0.0;
	        double motionY = 0.0; // You can adjust this value to control particle vertical motion
	        double motionZ = 0.0;

	        // Calculate the final spawn position with the offset
	        double spawnX = x + offsetX;
	        double spawnY = y + yOffset + offsetY;
	        double spawnZ = z + offsetZ;

	        EntityFX particle = particleManager.spawnEffectParticle(
	            particleType.getParticleID(),
	            spawnX, spawnY, spawnZ,
	            motionX, motionY, motionZ
	        );

	        if (particle != null) {
	            // Customize particle properties if needed
	        }
	    }
	}

}

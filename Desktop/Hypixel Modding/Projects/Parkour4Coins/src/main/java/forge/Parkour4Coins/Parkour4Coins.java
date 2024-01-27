package forge.Parkour4Coins;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.apache.commons.lang3.time.DurationFormatUtils;

import forge.Parkour4Coins.ParkourData.GhostDataEntry;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


@Mod(
   modid = Parkour4Coins.MODID,
   version = Parkour4Coins.VERSION,
   acceptedMinecraftVersions = "[1.8.9]"
)
public class Parkour4Coins {
   public static final String MODID = "Parkour4Coins";
   public static final String VERSION = "1.0";

   private long startTime;
   StringBuilder bestRunSplits;
   private long finishTime;
   private long timeElapsed;
   private int checkpointCounter;
   private int messageFlag;
   private int recordFound;
   private int speedMeasureDelay = 0;
   private String parkourName = "";
   private String parkourNameMark = "";
   private String activePlayerUsername = Minecraft.getMinecraft().getSession().getUsername();
   private String islandOwner = "";
   
   private boolean ghostRecord = false;
   private boolean ghostPlayback = false;
   private boolean ghostPlaybackTrigger = false;
   private int ghostMeasureDelay = 0;
   private int ghostMeasureTotal = 0;
   private List<BlockPos> ghostPositions = new ArrayList<>();
   private List<Long> ghostTimestamps = new ArrayList<>();
   private List<GhostDataEntry> bestGhostData = new ArrayList<>();
   List<GhostDataEntry> ghostDataEntries = new ArrayList<>();
   private int ghostPlaybackIndex = 0;

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
   
   // Commands
   boolean parkourSpawn = false;
   int parkourSpawnDelay = 0;
   boolean parkourGhost = true;
   boolean parkourSplits = false;
   boolean parkourHud = true;
   boolean parkourStart = false;
   boolean parkourEnd = false;
   private static BlockPos startPosition = null;
   private static BlockPos endPosition = null;
   long segmentStartTime;
   long segmentEndTime;
   boolean distanceToEndTold;
   
   
   @EventHandler
   public void init(FMLInitializationEvent event) {
      MinecraftForge.EVENT_BUS.register(this);
      customSidebarElement = new CustomSidebarElement();
      createDirectory();

      // Load data from a file at the start of the game
      parkourData = parkourData.loadFromFile("mods/Parkour4Coins/parkour_data.json");
      selectedSound = parkourData.loadSelectedSoundFromFile("mods/Parkour4Coins/selected_sound.json");
      System.out.println("Loaded selectedSound: " + selectedSound);
      
      }
   
   private void createDirectory(){
	   // Create a File object for the directory
	   String directoryPathRoot = "mods/Parkour4Coins";
	   File directoryRoot = new File(directoryPathRoot);
	   
	   if (!directoryRoot.exists()) {
	       boolean created = directoryRoot.mkdir(); // This method creates a single directory
	       if (created) {
	    	   String directoryPathGhosts = "mods/Parkour4Coins/Ghosts";
	    	   File directoryGhosts = new File(directoryPathGhosts);
		       directoryGhosts.mkdir(); // This method creates a single directory
	       }
	   }
   }

   // Save data to the file when needed
   private void saveDataToFile() {
       parkourData.saveToFile("mods/Parkour4Coins/parkour_data.json");
   }
   // Save ghost data
   private void saveGhostData(){
	   // Link to PB sharing Discord channel
       displayMessage(EnumChatFormatting.BOLD + "------");
       IChatComponent linkMessage = new ChatComponentText(EnumChatFormatting.BOLD + "Share your run here! " + EnumChatFormatting.RESET + "https://discord.gg/mR5YA9CrAn");
       ChatStyle chatStyle = new ChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://discord.gg/mR5YA9CrAn"));
       linkMessage.setChatStyle(chatStyle);
       Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(linkMessage);
       // Info on how to share ghosts
       displayMessage(EnumChatFormatting.BOLD + "Upload your ghost file so others can race against you!");
       displayMessage(EnumChatFormatting.GRAY + "It's in your mods/Parkour4Coins/Ghosts directory.");
       displayMessage(EnumChatFormatting.GRAY + "Copy the \"" + parkourName + "-GhostData.json\" file");
       displayMessage(EnumChatFormatting.GRAY + "and share it on Discord in #pb-alley!");
       displayMessage(EnumChatFormatting.BOLD + "------");
	   
	   try {
	       for (int i = 0; i < ghostPositions.size(); i++) {
	           BlockPos position = ghostPositions.get(i);
	           long timestamp = ghostTimestamps.get(i);
	           GhostDataEntry ghostDataEntry = new GhostDataEntry(position, timestamp);
	           ghostDataEntries.add(ghostDataEntry);
	       }
	   } catch (Exception e) {
	   }
       
	   parkourData.saveGhostPositionsToFile(parkourName, ghostDataEntries, "mods/Parkour4Coins/Ghosts/" + parkourName +  "-GhostData.json");
   }

   
   @SubscribeEvent
   public void onChat(ClientChatReceivedEvent event) {
       String msg = event.message.getUnformattedText();

       // Show parkourData in chat
       for (String entry : parkourData.getParkourTimesDB()) {
           //Minecraft.getMinecraft().thePlayer.addChatComponentMessage(new ChatComponentText(entry));
       }
       
       if (msg.contains("parkourhelp") && !msg.contains("]") || msg.contains("phelp") && !msg.contains("]")) {
           event.setCanceled(true);
           displayMessage(EnumChatFormatting.BOLD + "Available Parkour4Coins Commands");
           displayMessage(EnumChatFormatting.BOLD  + "/parkourspawn " + EnumChatFormatting.RESET + "- Warps away then /visits automatically to prevent errors from warping too quickly");
           displayMessage(EnumChatFormatting.BOLD  + "/parkoursound " + EnumChatFormatting.RESET + "- Change the checkpoint and completion sounds");
           displayMessage(EnumChatFormatting.BOLD  + "/parkourghost " + EnumChatFormatting.RESET + "- Toggle the fire trail best-time ghost");
           displayMessage(EnumChatFormatting.BOLD  + "/parkoursplits " + EnumChatFormatting.RESET + "- Toggle the list of splits when starting a course");
           displayMessage(EnumChatFormatting.BOLD  + "/parkourhud " + EnumChatFormatting.RESET + "- Toggle the hud in the top-right");
           displayMessage(EnumChatFormatting.BOLD  + "/parkourstart " + EnumChatFormatting.RESET + "- Set the starting positon for a segment test");
           displayMessage(EnumChatFormatting.BOLD  + "/parkourend " + EnumChatFormatting.RESET + "- Set the ending positon for a segment test");
       }
       
       if (msg.contains("parkourspawn") && !msg.contains("]") || msg.contains("pspawn") && !msg.contains("]")) {
           event.setCanceled(true);
           displayMessage("Attempting to warp to " + islandOwner + "'s island spawn");
           parkourSpawn = true;
           parkourSpawnDelay = 0;
       }
       
       if (msg.contains("parkoursound") && !msg.contains("]") || msg.contains("psound") && !msg.contains("]")) {
           event.setCanceled(true);
           selectedSound++;
           startedParkourCounter = 0;
           parkourData.saveSelectedSoundToFile("mods/Parkour4Coins/selected_sound.json", selectedSound);
           playCheckpointSound(selectedSound);

           if (selectedSound > 3) {
               selectedSound = 1;
           }
       }
       
       if (msg.contains("parkourghost") && !msg.contains("]") || msg.contains("pghost") && !msg.contains("]")) {
           event.setCanceled(true);
           parkourGhost = !parkourGhost;
           displayMessage("Ghost playback: " + parkourGhost);
       }
       
       if (msg.contains("parkoursplits") && !msg.contains("]") || msg.contains("psplits") && !msg.contains("]")) {
           event.setCanceled(true);
           parkourSplits = !parkourSplits;
           displayMessage("Parkour splits at start: " + parkourSplits);
       }
       
       if (msg.contains("parkourhud") && !msg.contains("]") || msg.contains("phud") && !msg.contains("]")) {
           event.setCanceled(true);
           parkourHud = !parkourHud;
           displayMessage("Parkour hud: " + parkourHud);
       }
       
       if (msg.contains("parkourstart") && !msg.contains("]") || msg.contains("pstart") && !msg.contains("]")) {
           event.setCanceled(true);
           parkourStart = true;
	       EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
           startPosition = player.getPosition(); // Store the player's position
           displayMessage("Start set: " + startPosition);
       }
       
       if (msg.contains("parkourend") && !msg.contains("]") || msg.contains("pend") && !msg.contains("]")) {
           event.setCanceled(true);
           parkourEnd = true;
	       EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
           endPosition = player.getPosition(); // Store the player's position
           displayMessage("End set: " + endPosition);
       }

       if (msg.contains("Started parkour") && !msg.contains("]") || msg.contains("Reset time for parkour") && !msg.contains("]")) {
    	   // Start ghost recording in case run is a personal best
	       ghostPositions.clear();
	       ghostTimestamps.clear();
    	   ghostDataEntries.clear();
    	   ghostPlaybackTrigger = false;
    	   ghostRecord = true;
    	   ghostMeasureTotal = 0;
    	   
           EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
           String islandOwnerBossbar = BossStatus.bossName;
           // Check if the boss bar name is not empty or null
           if (islandOwnerBossbar != null && !islandOwnerBossbar.isEmpty()) {
               String[] islandOwnerBossbarSplit = islandOwnerBossbar.split(" ");
               String[] filteredWords = Arrays.stream(islandOwnerBossbarSplit)
                       .filter(word -> word.contains("'"))
                       .toArray(String[]::new);
               islandOwner = String.join(" ", filteredWords);
               if (!islandOwner.contains("'s")){
            	   islandOwner = activePlayerUsername;
               }
               islandOwner = islandOwner.replace("'s", "");
           }

           event.setCanceled(true);
           resetActiveCourse();
           playCheckpointSound(selectedSound);
           startTime = System.currentTimeMillis();
           checkpointCounter = 0;

           String parkourNameSpace = msg.replace("Started parkour", "").replace("Reset time for parkour", "");
           String parkourNameNoContext = parkourNameSpace.replace(" ", "");
           parkourName = islandOwner + "-" + parkourNameNoContext;
           parkourName = parkourName.replace("§0","").replace("§1","").replace("§2","").replace("§3","").replace("§4","").replace("§5","").replace("§6","").replace("§7","").replace("§8","").replace("§9","").replace("§a","").replace("§b","").replace("§c","").replace("§d","").replace("§e","").replace("§f","");
           parkourName = parkourName.replace("!", "");
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
                   bestRunSplits = new StringBuilder();
                   for (int i = 1; i < recordParts.length; i++) {
                       String entry = recordParts[i];
                       String renamedEntry = "CP" + counter + ": ";
                       bestRunSplits.append(renamedEntry).append(entry).append(", ");
                       counter++;
                   }
                   String lastEntry = recordParts[recordParts.length - 1];
                   bestRunSplits.replace(bestRunSplits.lastIndexOf("CP"), bestRunSplits.length(), "Finish: " + lastEntry);

                   if (parkourSplits){
                       displayMessage(EnumChatFormatting.GOLD + "Your best run: " + bestRunSplits.toString());
                   }
                   
                   // If record found, load ghost data
    	           ghostPlaybackIndex = 0;
                   bestGhostData = parkourData.loadGhostDataFromFile(parkourName, "mods/Parkour4Coins/Ghosts/" + parkourName +  "-GhostData.json");
                   if (parkourGhost){
                       ghostPlaybackTrigger = true;
                   }
               }
           }
           startedParkourCounter++;
       }

       if (msg.contains("Cancelled parkour!") && !msg.contains("]")) {
    	   ghostRecord = false;
    	   ghostPlayback = false;
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
                   int minutes = Integer.parseInt(activeTime[0]);
                   int seconds = Integer.parseInt(activeTime[1]);
                   int milliseconds = Integer.parseInt(activeTime[2]);
                   // Calculate the total time in milliseconds
                   long activeTimeCompare = (minutes * 60 * 1000) + (seconds * 1000) + milliseconds;
                   
                   String[] recordTime = parkourTimesDBComponents[checkpointCounter].split("[:.]");
                   minutes = Integer.parseInt(recordTime[0].replace(" ",""));
                   seconds = Integer.parseInt(recordTime[1].replace(" ",""));
                   milliseconds = Integer.parseInt(recordTime[2].replace(" ",""));
                   // Calculate the total time in milliseconds
                   long recordTimeCompare = (minutes * 60 * 1000) + (seconds * 1000) + milliseconds;
                   timeDifference = activeTimeCompare - recordTimeCompare;
                   
                   if (activeTimeCompare < recordTimeCompare) {
                       displayMessage(EnumChatFormatting.GOLD + " > CP " + checkpointCounter + " || " + formattedTime + " (" + timeDifference + ")");
                       messageFlag = 1;
                   }
                   if (activeTimeCompare >= recordTimeCompare) {
                       displayMessage(EnumChatFormatting.GREEN + " > CP " + checkpointCounter + " || " + formattedTime + " (+" + timeDifference + ")");
                       messageFlag = 1;
                   }
               }
           }
           if (messageFlag == 0) {
               displayMessage(EnumChatFormatting.GREEN + " > CP " + checkpointCounter + " || " + formattedTime + " (+0)");
           }
       }
       
       String chatMessage = event.message.getUnformattedText();
       if (msg.contains("Finished parkour") && !msg.contains("]")) {
    	   ghostRecord = false;
           ghostPlayback = false;
    	   
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
                   int minutes = Integer.parseInt(activeTime[0]);
                   int seconds = Integer.parseInt(activeTime[1]);
                   int milliseconds = Integer.parseInt(activeTime[2]);
                   // Calculate the total time in milliseconds
                   long activeTimeCompare = (minutes * 60 * 1000) + (seconds * 1000) + milliseconds;
                   
                   String[] recordTime = parkourTimesDBComponents[checkpointCounter].split("[:.]");
                   minutes = Integer.parseInt(recordTime[0].replace(" ",""));
                   seconds = Integer.parseInt(recordTime[1].replace(" ",""));
                   milliseconds = Integer.parseInt(recordTime[2].replace(" ",""));
                   // Calculate the total time in milliseconds
                   long recordTimeCompare = (minutes * 60 * 1000) + (seconds * 1000) + milliseconds;

                   if (activeTimeCompare < recordTimeCompare) {
                       playCompleteSound(selectedSound);
                       displayMessage(EnumChatFormatting.GOLD + "Personal Best! " + formattedTime + " (" + timeDifference + ")");
                       //Minecraft.getMinecraft().ingameGUI.displayTitle(EnumChatFormatting.GOLD + "Personal Best!", "Your Time: 1:23.456", 20, 60, 20);
                       messageFlag = 1;
                       parkourData.getParkourRecordsDB().remove(line);
                       parkourData.getParkourRecordsDB().add(activeCourse.toString());

                       saveGhostData();
                       
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
               displayMessage(EnumChatFormatting.GOLD + "Personal Best! " + formattedTime + " (" + timeDifference + ")");
               //Minecraft.getMinecraft().ingameGUI.displayTitle(EnumChatFormatting.GOLD + "Personal Best!", formattedTime, 20, 60, 20);
               messageFlag = 1;
               parkourData.getParkourRecordsDB().add(activeCourse.toString());

               saveGhostData();
           }
           
           // Save data to file after updating
           saveDataToFile();
       }
   }

   @SubscribeEvent
   public void onClientChatClickEvent(GuiScreenEvent.ActionPerformedEvent event) {
       if (event.button != null && event.button.displayString.equals("[Click to copy your run information]")) {
           String copyText = "This is the text to be copied to the clipboard"; // Replace with your desired text
           setClipboard(copyText);
       }
   }

   private void setClipboard(String text) {
       StringSelection stringSelection = new StringSelection(text);
       Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
       clipboard.setContents(stringSelection, null);
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
    	   if (parkourHud){
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
	   // parkourspawn command
	   if (parkourSpawn){
	       EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
		   parkourSpawnDelay++;
		   if (parkourSpawnDelay == 1){
		       player.sendChatMessage("/is");
		   }
		   if (parkourSpawnDelay >= 110){
		       player.sendChatMessage("/visit " + islandOwner);
		       parkourSpawn = false;
		       parkourSpawnDelay = 0;
		   }
	   }
	   
	   // parkour start/end commands
	   if (parkourStart && parkourEnd){
           int startSoundDelay = 0;
		   // Measure if player is near the marked end
	       EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
           double distanceToStart = player.getDistance(startPosition.getX(), startPosition.getY(), startPosition.getZ());
           double distanceToEnd = player.getDistance(endPosition.getX(), endPosition.getY(), endPosition.getZ());
           // Display where start and end are
           spawnParticlesAtPosition(startPosition.getX(), startPosition.getY(), startPosition.getZ());
           spawnParticlesAtPosition(endPosition.getX(), endPosition.getY(), endPosition.getZ());
           if (distanceToStart < 1.0) {
               segmentStartTime = System.currentTimeMillis();
               distanceToEndTold = false;
               displayMessage("At start of segment");
           }
           if (distanceToEnd < 1.0){
        	   if (!distanceToEndTold){
            	   segmentEndTime = System.currentTimeMillis();
            	   long segmentFinishTime = segmentEndTime - segmentStartTime;
            	   displayMessage("Time to reach end: " + segmentFinishTime);
            	   distanceToEndTold = true;
        	   }
           }
	   }

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
	   // Save player position data every half second for best time ghost - stop at 10 minutes
	   if (player != null) {
		    ghostMeasureDelay++;
		    // Stop ghost recordings at a set time
		    if (ghostMeasureDelay >= 5 && ghostRecord) {
		    	int maxGhostMeasurements = 2400; // Every 8 measurements is ONE second
			    if (ghostMeasureTotal < maxGhostMeasurements){
				    ghostMeasureTotal++;
			        ghostMeasureDelay = 0;
			        BlockPos ghostCurrentPosition = player.getPosition();
			        ghostPositions.add(ghostCurrentPosition);
			        long currentTime = System.currentTimeMillis();
			        ghostTimestamps.add(currentTime);
			    }
			    if (ghostMeasureTotal == maxGhostMeasurements){
				    ghostMeasureTotal++;
				    displayMessage("Ghost recording stopped - max size reached (5 minutes)");
			    }
		    }
	   }
	   // Playback ghost at parkour start
	   if (player != null) {
		   if (ghostPlaybackTrigger){
               ghostPlaybackTrigger = false;
               ghostTimestamps.clear();
			   ghostPlayback = true;
		   }
		   
	       if (ghostPlayback && !bestGhostData.isEmpty() && ghostPlaybackIndex < bestGhostData.size()) {
	           ParkourData.GhostDataEntry ghostDataEntry = bestGhostData.get(ghostPlaybackIndex);
	           long currentTime = System.currentTimeMillis();
	           
	           if (ghostTimestamps.size() > ghostPlaybackIndex + 1) {	               	
	        	   if (currentTime >= ghostTimestamps.get(ghostPlaybackIndex) && currentTime <= ghostTimestamps.get(ghostPlaybackIndex + 1)) {
	                   
	                   double x = ghostDataEntry.getPosition().getX();
	                   double y = ghostDataEntry.getPosition().getY();
	                   double z = ghostDataEntry.getPosition().getZ();

	                   // Spawn the particle at the interpolated position
	                   spawnParticlesAtPosition(x, y, z);

	                   ghostPlaybackIndex++;
	               } else if (currentTime >= ghostTimestamps.get(ghostPlaybackIndex + 1)) {
	                   // Handle delay between particles
	                   long delay = ghostTimestamps.get(ghostPlaybackIndex + 1) - ghostTimestamps.get(ghostPlaybackIndex);
	                   ghostPlaybackIndex++; // Move to the next ghost data entry
	               }
	               if (ghostPlaybackIndex >= bestGhostData.size()) {
	                   // All data processed, set ghostPlayback to false
	                   ghostPlayback = false;
	                   ghostTimestamps.clear();
	               }
	           }
	       }
	   }
   }
   
   private void spawnParticlesAtPosition(double x, double y, double z) {
	    World world = Minecraft.getMinecraft().theWorld;
	    EffectRenderer particleManager = Minecraft.getMinecraft().effectRenderer;

	    EnumParticleTypes particleType = EnumParticleTypes.FLAME; // Change this to the desired particle type
	    int numParticles = 1; // Number of particles to spawn
	    double yOffset = 0.0; // Offset above the target position

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

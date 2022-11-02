package me.Parkour4Coins;

import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.time.StopWatch;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;

@Mod(modid = Parkour4Coins.MODID, version = Parkour4Coins.VERSION)
public class Parkour4Coins
{
    public static final String MODID = "Parkour4Coins";
    public static final String VERSION = "1.0";
    
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    private long start;
    private long finish;
    private long timeElapsed; //milliseconds
    private long secondsElapsed;
    private long minutesElapsed;
    private int checkpointCounter;
    
    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        String msg = event.message.getUnformattedText();   
        if (msg.contains("Started parkour") && !msg.contains(":")) {
            Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation(this.MODID, "ping"), 1.0F));
            //Start stopwatch
            start = System.currentTimeMillis();
            checkpointCounter = 0;
        }
        if (msg.contains("Reached checkpoint #") && !msg.contains("]")) {
        	Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation(this.MODID, "ping"), 1.0F));
        	//Count current checkpoint
        	checkpointCounter++;
        	//Calculate total time 
            finish = System.currentTimeMillis();
            timeElapsed = finish - start;
            
            String formatedTime = DurationFormatUtils.formatDuration(timeElapsed, "mm:ss.SSS");
            
            Minecraft.getMinecraft().thePlayer.addChatComponentMessage(new ChatComponentText(" > CP " + checkpointCounter + " || " + formatedTime));
        }
        if (msg.contains("Reset time for parkour") && !msg.contains(":")) {
        	Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation(this.MODID, "ping"), 1.0F));
        	//Reset stopwatch
            start = System.currentTimeMillis();
            checkpointCounter = 0;
        }
        if (msg.contains("Finished parkour") && !msg.contains("]")) {
        	Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation(this.MODID, "ping"), 1.0F));
        	//Stop stopwatch
            finish = System.currentTimeMillis();
            timeElapsed = finish - start;
            
            String formatedTime = DurationFormatUtils.formatDuration(timeElapsed, "mm:ss.SSS");
            Minecraft.getMinecraft().thePlayer.addChatComponentMessage(new ChatComponentText(" > Finish (realtime) " + " || " + formatedTime));
        }
    }
}

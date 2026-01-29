package com.TETOSOFT.tilegame;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

/**
 * SoundManager class handles background music and sound effects
 */
public class SoundManager {
    private Clip backgroundClip;
    private String soundPath;
    
    public SoundManager() {
        // Get the user's home directory and construct the sounds folder path
        String userHome = System.getProperty("user.home");
        this.soundPath = userHome + File.separator + "Desktop" + File.separator + 
                         "Super-Mario-Java-2D-Game-master" + File.separator + "sounds" + File.separator;
    }
    
    /**
     * Load and play background music in a loop
     */
    public void playBackgroundMusic(String filename) {
        try {
            String filepath = soundPath + filename;
            File soundFile = new File(filepath);
            
            System.out.println("========================================");
            System.out.println("Attempting to load sound from: " + filepath);
            System.out.println("File exists: " + soundFile.exists());
            System.out.println("File absolute path: " + soundFile.getAbsolutePath());
            System.out.println("Can read: " + soundFile.canRead());
            System.out.println("File size: " + soundFile.length() + " bytes");
            System.out.println("========================================");
            
            if (!soundFile.exists()) {
                System.err.println("ERROR: Sound file not found!");
                System.err.println("Expected file at: " + filepath);
                return;
            }
            
            try {
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
                backgroundClip = AudioSystem.getClip();
                backgroundClip.open(audioInputStream);
                backgroundClip.loop(Clip.LOOP_CONTINUOUSLY);
                backgroundClip.start();
                
                System.out.println("SUCCESS: Background music started: " + filename);
            } catch (UnsupportedAudioFileException uafe) {
                System.err.println("ERROR: Unsupported audio format!");
                System.err.println("This format may not be supported by Java.");
                System.err.println("Try converting to WAV format instead.");
                System.err.println("Details: " + uafe.getMessage());
                uafe.printStackTrace();
            }
        } catch (IOException | LineUnavailableException e) {
            System.err.println("ERROR loading sound: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Stop background music
     */
    public void stopBackgroundMusic() {
        if (backgroundClip != null && backgroundClip.isRunning()) {
            backgroundClip.stop();
            backgroundClip.close();
            System.out.println("Background music stopped");
        }
    }
    
    /**
     * Pause background music
     */
    public void pauseBackgroundMusic() {
        if (backgroundClip != null && backgroundClip.isRunning()) {
            backgroundClip.stop();
            System.out.println("Background music paused");
        }
    }
    
    /**
     * Resume background music
     */
    public void resumeBackgroundMusic() {
        if (backgroundClip != null && !backgroundClip.isRunning()) {
            backgroundClip.start();
            System.out.println("Background music resumed");
        }
    }
    
    /**
     * Check if background music is playing
     */
    public boolean isPlaying() {
        return backgroundClip != null && backgroundClip.isRunning();
    }
}

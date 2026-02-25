package com.TETOSOFT.tilegame;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Iterator;

import com.TETOSOFT.graphics.*;
import com.TETOSOFT.input.*;
import com.TETOSOFT.test.GameCore;
import com.TETOSOFT.tilegame.sprites.*;

/**
 * GameManager manages all parts of the game.
 */
public class GameEngine extends GameCore 
{
    
    public static void main(String[] args) 
    {
        new GameEngine().run();
    }
    
    public static final float GRAVITY = 0.002f;
    
    // Game states
    public static final int STATE_MENU = 0;
    public static final int STATE_PLAYING = 1;
    public static final int STATE_GAME_OVER = 2;
    public static final int STATE_WIN_GAME = 3;
    public static final int STATE_INPUT_NAME = 4;
    
    private Point pointCache = new Point();
    private TileMap map;
    private int gameState = STATE_MENU;
    private MapLoader mapLoader;
    private InputManager inputManager;
    private TileMapDrawer drawer;
    private SoundManager soundManager;
    
    private GameAction moveLeft;
    private GameAction moveRight;
    private GameAction jump;
    private GameAction exit;
    private GameAction startGame;
    private int collectedStars=0;
    private int numLives=6;
    private long gameStartTime=0;
    private long elapsedSeconds=0;
    private long finalGameTime=0;
    private long gameOverTime=0;
    private String playerName = "Player";
    private StringBuilder nameInput = new StringBuilder();
   
    public void init()
    {
        super.init();
        
        // set up input manager
        initInput();
        
        // start resource manager
        mapLoader = new MapLoader(screen.getFullScreenWindow().getGraphicsConfiguration());
        
        // load resources
        drawer = new TileMapDrawer();
        drawer.setBackground(mapLoader.loadImage("background.jpg"));
        
        // Initialize sound manager
        soundManager = new SoundManager();
        
        // Game starts at menu
        gameState = STATE_MENU;
    }
    
    
    /**
     * Closes any resurces used by the GameManager.
     */
    public void stop() {
        soundManager.stopBackgroundMusic();
        super.stop();
        
    }
    
    
    private void initInput() {
        moveLeft = new GameAction("moveLeft");
        moveRight = new GameAction("moveRight");
        jump = new GameAction("jump", GameAction.DETECT_INITAL_PRESS_ONLY);
        exit = new GameAction("exit",GameAction.DETECT_INITAL_PRESS_ONLY);
        startGame = new GameAction("startGame",GameAction.DETECT_INITAL_PRESS_ONLY);
        
        inputManager = new InputManager(screen.getFullScreenWindow());
        inputManager.setCursor(InputManager.INVISIBLE_CURSOR);
        
        inputManager.mapToKey(moveLeft, KeyEvent.VK_LEFT);
        inputManager.mapToKey(moveRight, KeyEvent.VK_RIGHT);
        inputManager.mapToKey(jump, KeyEvent.VK_UP);
        inputManager.mapToKey(startGame, KeyEvent.VK_ENTER);
        inputManager.mapToKey(exit, KeyEvent.VK_ESCAPE);
    }
    
    
    private void checkInput(long elapsedTime) 
    {
        if (exit.isPressed()) {
            stop();
        }
        
        if (gameState == STATE_INPUT_NAME) {
            char ch = inputManager.getLastTypedChar();
            if (ch != '\0') {
                if (ch == '\b' || ch == 127) { // backspace
                    if (nameInput.length() > 0) {
                        nameInput.deleteCharAt(nameInput.length() - 1);
                    }
                } else if (Character.isLetterOrDigit(ch) || ch == ' ') {
                    if (nameInput.length() < 20) {
                        nameInput.append(ch);
                    }
                }
            }
            if (startGame.isPressed()) {
                if (nameInput.length() > 0) {
                    playerName = nameInput.toString();
                } else {
                    playerName = "Player";
                }
                startNewGame();
            }
            return;
        }
        
        if (gameState == STATE_MENU) {
            soundManager.pauseBackgroundMusic();
            if (startGame.isPressed()) {
                gameState = STATE_INPUT_NAME;
                nameInput.setLength(0);
            }
            return;
        }
        
        if (gameState == STATE_WIN_GAME) {
            soundManager.pauseBackgroundMusic();
            if (startGame.isPressed()) {
                gameState = STATE_MENU;
            }
            return;
        }
        
        if (gameState == STATE_GAME_OVER) {
            soundManager.pauseBackgroundMusic();
            if (startGame.isPressed()) {
                gameState = STATE_MENU;
            }
            return;
        }
        
        Player player = (Player)map.getPlayer();
        if (player.isAlive()) 
        {
            float velocityX = 0;
            if (moveLeft.isPressed()) 
            {
                velocityX-=player.getMaxSpeed();
            }
            if (moveRight.isPressed()) {
                velocityX+=player.getMaxSpeed();
            }
            if (jump.isPressed()) {
                player.jump(false);
            }
            player.setVelocityX(velocityX);
        }
        
    }
    
    private void startNewGame() {
        collectedStars = 0;
        numLives = 6;
        mapLoader.currentMap = 0;
        map = mapLoader.loadNextMap();
        gameStartTime = System.currentTimeMillis();
        gameState = STATE_PLAYING;
        
        soundManager.playBackgroundMusic("background.wav");
    }
    
    public void draw(Graphics2D g) {
        if (gameState == STATE_MENU) {
            drawMenu(g);
        } else if (gameState == STATE_INPUT_NAME) {
            drawNameInputScreen(g);
        } else if (gameState == STATE_WIN_GAME) {
            drawVictoryScreen(g);
        } else if (gameState == STATE_GAME_OVER) {
            drawGameOverScreen(g);
        } else {
            drawer.draw(g, map, screen.getWidth(), screen.getHeight());
            g.setColor(Color.WHITE);
            g.drawString("Press ESC for EXIT.",10.0f,20.0f);
            g.setColor(Color.GREEN);
            g.drawString("Coins: "+collectedStars,300.0f,20.0f);
            g.setColor(Color.YELLOW);
            g.drawString("Lives: "+(numLives),500.0f,20.0f );
            g.setColor(Color.WHITE);
            g.drawString("Home: "+mapLoader.currentMap,700.0f,20.0f);
            
            elapsedSeconds = (System.currentTimeMillis() - gameStartTime) / 1000;
            int minutes = (int)(elapsedSeconds / 60);
            int seconds = (int)(elapsedSeconds % 60);
                g.setColor(Color.CYAN);
                g.drawString("Time: " + minutes + ":" + String.format("%02d", seconds), 10.0f, 50.0f);

                try {
                    Sprite playerSprite = map.getPlayer();
                    int mapWidth = TileMapDrawer.tilesToPixels(map.getWidth());

                    int offsetX = screen.getWidth() / 2 - Math.round(playerSprite.getX()) - 64;
                    offsetX = Math.min(offsetX, 0);
                    offsetX = Math.max(offsetX, screen.getWidth() - mapWidth);

                    int offsetY = screen.getHeight() - TileMapDrawer.tilesToPixels(map.getHeight());

                    int px = Math.round(playerSprite.getX()) + offsetX;
                    int py = Math.round(playerSprite.getY()) + offsetY;

                    g.setFont(new Font("Arial", Font.BOLD, 18));
                    g.setColor(Color.WHITE);
                    String nameLabel = playerName;
                    FontMetrics nfm = g.getFontMetrics();
                    int nameX = px + (playerSprite.getWidth() / 2) - (nfm.stringWidth(nameLabel) / 2);
                    int nameY = py - 8; 
                    g.drawString(nameLabel, nameX, nameY);
                } catch (Exception ex) {

                }
        }
    }
    
    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, screen.getWidth(), screen.getHeight());
        
        g.setFont(new Font("Arial", Font.BOLD, 72));
        g.setColor(Color.YELLOW);
        String title = "SUPER MARIO";
        FontMetrics fm = g.getFontMetrics();
        int titleX = (screen.getWidth() - fm.stringWidth(title)) / 2;
        g.drawString(title, titleX, 150);
        
        g.setFont(new Font("Arial", Font.BOLD, 48));
        g.setColor(Color.CYAN);
        String startText = "Press ENTER to START";
        fm = g.getFontMetrics();
        int startX = (screen.getWidth() - fm.stringWidth(startText)) / 2;
        g.drawString(startText, startX, 300);
        
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.setColor(Color.WHITE);
        String[] instructions = {
            "Arrow Keys - Move Left/Right",
            "Up Arrow - Jump",
            "ESC - Exit Game"
        };
        int instructionY = 400;
        for (String instruction : instructions) {
            fm = g.getFontMetrics();
            int instructionX = (screen.getWidth() - fm.stringWidth(instruction)) / 2;
            g.drawString(instruction, instructionX, instructionY);
            instructionY += 40;
        }
    }
    
    private void drawNameInputScreen(Graphics2D g) {
        // Draw background
        g.setColor(new Color(0, 0, 0, 220));
        g.fillRect(0, 0, screen.getWidth(), screen.getHeight());
        
        g.setFont(new Font("Arial", Font.BOLD, 64));
        g.setColor(Color.YELLOW);
        String title = "Enter Your Name";
        FontMetrics fm = g.getFontMetrics();
        int titleX = (screen.getWidth() - fm.stringWidth(title)) / 2;
        g.drawString(title, titleX, 150);
        
        g.setFont(new Font("Arial", Font.PLAIN, 48));
        g.setColor(Color.WHITE);
        String inputDisplay = nameInput.toString() + (System.currentTimeMillis() % 1000 < 500 ? "_" : "");
        fm = g.getFontMetrics();
        int inputX = (screen.getWidth() - fm.stringWidth(inputDisplay)) / 2;
        g.drawString(inputDisplay, inputX, 300);
        
        g.setFont(new Font("Arial", Font.PLAIN, 32));
        g.setColor(Color.CYAN);
        String instruction = "Press ENTER to Continue";
        fm = g.getFontMetrics();
        int instructionX = (screen.getWidth() - fm.stringWidth(instruction)) / 2;
        g.drawString(instruction, instructionX, 400);
    }
    
    private void drawGameOverScreen(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 220));
        g.fillRect(0, 0, screen.getWidth(), screen.getHeight());
        
        g.setFont(new Font("Arial", Font.BOLD, 56));
        g.setColor(new Color(255, 0, 0));
        String title = "GAME OVER";
        FontMetrics fm = g.getFontMetrics();
        int titleX = (screen.getWidth() - fm.stringWidth(title)) / 2;
        g.drawString(title, titleX, 120);
        
        g.setFont(new Font("Arial", Font.BOLD, 48));
        g.setColor(Color.WHITE);
        String nameDisplay = playerName;
        fm = g.getFontMetrics();
        int nameX = (screen.getWidth() - fm.stringWidth(nameDisplay)) / 2;
        g.drawString(nameDisplay, nameX, 180);
        
        g.setFont(new Font("Arial", Font.BOLD, 48));
        g.setColor(Color.CYAN);
        String message = "You Lost!";
        fm = g.getFontMetrics();
        int messageX = (screen.getWidth() - fm.stringWidth(message)) / 2;
        g.drawString(message, messageX, 270);
        
        g.setFont(new Font("Arial", Font.BOLD, 32));
        g.setColor(Color.WHITE);
        
        long totalElapsedSeconds = (gameOverTime - gameStartTime) / 1000;
        int minutes = (int)(totalElapsedSeconds / 60);
        int seconds = (int)(totalElapsedSeconds % 60);
        
        String[] stats = {
            "Total Coins: " + collectedStars,
            "Final Lives: 0",
            "Total Time: " + minutes + ":" + String.format("%02d", seconds)
        };
        
        int statsY = 370;
        for (String stat : stats) {
            fm = g.getFontMetrics();
            int statX = (screen.getWidth() - fm.stringWidth(stat)) / 2;
            g.drawString(stat, statX, statsY);
            statsY += 40;
        }
        
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.setColor(Color.YELLOW);
        String restartText = "Press ENTER to Return to Menu";
        fm = g.getFontMetrics();
        int restartX = (screen.getWidth() - fm.stringWidth(restartText)) / 2;
        g.drawString(restartText, restartX, 520);
    }
    
    private void drawVictoryScreen(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 220));
        g.fillRect(0, 0, screen.getWidth(), screen.getHeight());
        
        g.setFont(new Font("Arial", Font.BOLD, 56));
        g.setColor(Color.YELLOW);
        String title = "CONGRATULATIONS";
        FontMetrics fm = g.getFontMetrics();
        int titleX = (screen.getWidth() - fm.stringWidth(title)) / 2;
        g.drawString(title, titleX, 120);
        
        g.setFont(new Font("Arial", Font.BOLD, 48));
        g.setColor(Color.CYAN);
        String nameDisplay = playerName + "!";
        fm = g.getFontMetrics();
        int nameX = (screen.getWidth() - fm.stringWidth(nameDisplay)) / 2;
        g.drawString(nameDisplay, nameX, 180);
        
        g.setFont(new Font("Arial", Font.BOLD, 48));
        g.setColor(Color.CYAN);
        String message = "You Have Won!";
        fm = g.getFontMetrics();
        int messageX = (screen.getWidth() - fm.stringWidth(message)) / 2;
        g.drawString(message, messageX, 270);
        
        g.setFont(new Font("Arial", Font.BOLD, 32));
        g.setColor(Color.WHITE);
        
        long totalElapsedSeconds = (finalGameTime - gameStartTime) / 1000;
        int minutes = (int)(totalElapsedSeconds / 60);
        int seconds = (int)(totalElapsedSeconds % 60);
        
        String[] stats = {
            "Total Coins: " + collectedStars,
            "Final Lives: " + numLives,
            "Total Time: " + minutes + ":" + String.format("%02d", seconds)
        };
        
        int statsY = 370;
        for (String stat : stats) {
            fm = g.getFontMetrics();
            int statX = (screen.getWidth() - fm.stringWidth(stat)) / 2;
            g.drawString(stat, statX, statsY);
            statsY += 60;
        }
        
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.setColor(Color.YELLOW);
        String restartText = "Press ENTER to return to Menu or ESC to Exit";
        fm = g.getFontMetrics();
        int restartX = (screen.getWidth() - fm.stringWidth(restartText)) / 2;
        g.drawString(restartText, restartX, 600);
    }
    
    public TileMap getMap() {
        return map;
    }
    
    
    public Point getTileCollision(Sprite sprite, float newX, float newY) 
    {
        if (map == null) {
            return null;
        }
        
        float fromX = Math.min(sprite.getX(), newX);
        float fromY = Math.min(sprite.getY(), newY);
        float toX = Math.max(sprite.getX(), newX);
        float toY = Math.max(sprite.getY(), newY);
        
        int fromTileX = TileMapDrawer.pixelsToTiles(fromX);
        int fromTileY = TileMapDrawer.pixelsToTiles(fromY);
        int toTileX = TileMapDrawer.pixelsToTiles(
                toX + sprite.getWidth() - 1);
        int toTileY = TileMapDrawer.pixelsToTiles(
                toY + sprite.getHeight() - 1);
        
        for (int x=fromTileX; x<=toTileX; x++) {
            for (int y=fromTileY; y<=toTileY; y++) {
                if (x < 0 || x >= map.getWidth() ||
                        map.getTile(x, y) != null) {
                    pointCache.setLocation(x, y);
                    return pointCache;
                }
            }
        }
        
        return null;
    }
    
    
    /**
     * Checks if two Sprites collide with one another. Returns
     * false if the two Sprites are the same. Returns false if
     * one of the Sprites is a Creature that is not alive.
     */
    public boolean isCollision(Sprite s1, Sprite s2) {
        // if the Sprites are the same, return false
        if (s1 == s2) {
            return false;
        }
        
        // if one of the Sprites is a dead Creature, return false
        if (s1 instanceof Creature && !((Creature)s1).isAlive()) {
            return false;
        }
        if (s2 instanceof Creature && !((Creature)s2).isAlive()) {
            return false;
        }
        
        // get the pixel location of the Sprites
        int s1x = Math.round(s1.getX());
        int s1y = Math.round(s1.getY());
        int s2x = Math.round(s2.getX());
        int s2y = Math.round(s2.getY());
        
        // check if the two sprites' boundaries intersect
        return (s1x < s2x + s2.getWidth() &&
                s2x < s1x + s1.getWidth() &&
                s1y < s2y + s2.getHeight() &&
                s2y < s1y + s1.getHeight());
    }
    
    
    /**
     * Gets the Sprite that collides with the specified Sprite,
     * or null if no Sprite collides with the specified Sprite.
     */
    public Sprite getSpriteCollision(Sprite sprite) {
        if (map == null) {
            return null;
        }
        
        // run through the list of Sprites
        Iterator i = map.getSprites();
        while (i.hasNext()) {
            Sprite otherSprite = (Sprite)i.next();
            if (isCollision(sprite, otherSprite)) {
                // collision found, return the Sprite
                return otherSprite;
            }
        }
        
        // no collision found
        return null;
    }
    
    
    /**
     * Updates Animation, position, and velocity of all Sprites
     * in the current map.
     */
    public void update(long elapsedTime) {
        // get keyboard/mouse input (always check input, even in menu)
        checkInput(elapsedTime);
        
        if (gameState != STATE_PLAYING) {
            return;
        }
        
        // Check if map is null (game won)
        if (map == null) {
            finalGameTime = System.currentTimeMillis();
            gameState = STATE_WIN_GAME;
            return;
        }
        
        Creature player = (Creature)map.getPlayer();
        
        
        // player is dead! start map over
        if (player.getState() == Creature.STATE_DEAD) {
            map = mapLoader.reloadMap();
            return;
        }
        
        // update player
        updateCreature(player, elapsedTime);
        player.update(elapsedTime);
        
        // Check if game won after updating player
        if (map == null) {
            gameState = STATE_WIN_GAME;
            return;
        }
        
        // update other sprites
        Iterator i = map.getSprites();
        while (i.hasNext()) {
            Sprite sprite = (Sprite)i.next();
            if (sprite instanceof Creature) {
                Creature creature = (Creature)sprite;
                if (creature.getState() == Creature.STATE_DEAD) {
                    i.remove();
                } else {
                    updateCreature(creature, elapsedTime);
                }
            }
            // normal update
            sprite.update(elapsedTime);
        }
    }
    
    
    /**
     * Updates the creature, applying gravity for creatures that
     * aren't flying, and checks collisions.
     */
    private void updateCreature(Creature creature,
            long elapsedTime) {
        if (map == null) {
            return;
        }
        
        // apply gravity
        if (!creature.isFlying()) {
            creature.setVelocityY(creature.getVelocityY() +
                    GRAVITY * elapsedTime);
        }
        
        // change x
        float dx = creature.getVelocityX();
        float oldX = creature.getX();
        float newX = oldX + dx * elapsedTime;
        Point tile =
                getTileCollision(creature, newX, creature.getY());
        if (tile == null) {
            creature.setX(newX);
        } else {
            // line up with the tile boundary
            if (dx > 0) {
                creature.setX(
                        TileMapDrawer.tilesToPixels(tile.x) -
                        creature.getWidth());
            } else if (dx < 0) {
                creature.setX(
                        TileMapDrawer.tilesToPixels(tile.x + 1));
            }
            creature.collideHorizontal();
        }
        if (creature instanceof Player) {
            checkPlayerCollision((Player)creature, false);
        }
        
        // change y
        float dy = creature.getVelocityY();
        float oldY = creature.getY();
        float newY = oldY + dy * elapsedTime;
        tile = getTileCollision(creature, creature.getX(), newY);
        if (tile == null) {
            creature.setY(newY);
        } else {
            // line up with the tile boundary
            if (dy > 0) {
                creature.setY(
                        TileMapDrawer.tilesToPixels(tile.y) -
                        creature.getHeight());
            } else if (dy < 0) {
                creature.setY(
                        TileMapDrawer.tilesToPixels(tile.y + 1));
            }
            creature.collideVertical();
        }
        if (creature instanceof Player) {
            boolean canKill = (oldY < creature.getY());
            checkPlayerCollision((Player)creature, canKill);
        }
        
    }
    
    
    /**
     * Checks for Player collision with other Sprites. If
     * canKill is true, collisions with Creatures will kill
     * them.
     */
    public void checkPlayerCollision(Player player,
            boolean canKill) {
        if (!player.isAlive() || map == null) {
            return;
        }
        
        // check for player collision with other sprites
        Sprite collisionSprite = getSpriteCollision(player);
        if (collisionSprite instanceof PowerUp) {
            acquirePowerUp((PowerUp)collisionSprite);
        } else if (collisionSprite instanceof Creature) {
            Creature badguy = (Creature)collisionSprite;
            if (canKill) {
                // kill the badguy and make player bounce
                badguy.setState(Creature.STATE_DYING);
                player.setY(badguy.getY() - player.getHeight());
                player.jump(true);
            } else {
                // player dies!
                player.setState(Creature.STATE_DYING);
                numLives--;
                if(numLives==0) {
                    soundManager.pauseBackgroundMusic();
                    gameOverTime = System.currentTimeMillis();
                    gameState = STATE_GAME_OVER;
                }
            }
        }
    }
    
    
    /**
     * Gives the player the speicifed power up and removes it
     * from the map.
     */
    public void acquirePowerUp(PowerUp powerUp) {
        map.removeSprite(powerUp);
        
        if (powerUp instanceof PowerUp.Star) {
            collectedStars++;
            if(collectedStars==100) 
            {
                numLives++;
                collectedStars=0;
            }
            
        } else if (powerUp instanceof PowerUp.Music) {
            
        } else if (powerUp instanceof PowerUp.Goal) {
            numLives += 2;
            map = mapLoader.loadNextMap();
            
            if (map == null) {
                finalGameTime = System.currentTimeMillis();
                gameState = STATE_WIN_GAME;
            }
            
        }
    }   
}
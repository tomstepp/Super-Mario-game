package com.brackeen.javagamebook.tilegame;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Iterator;

import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.sampled.AudioFormat;

import com.brackeen.javagamebook.graphics.*;
import com.brackeen.javagamebook.sound.*;
import com.brackeen.javagamebook.input.*;
import com.brackeen.javagamebook.test.GameCore;
import com.brackeen.javagamebook.tilegame.sprites.*;

/**
    GameManager manages all parts of the game.
*/
public class GameManager extends GameCore {

    public static void main(String[] args) {
    	String gamefile = null;
    	if (args.length > 0){
        	gamefile = args[0];
        }    	
    	new GameManager().run(gamefile);        
    }

    // uncompressed, 44100Hz, 16-bit, mono, signed, little-endian
    private static final AudioFormat PLAYBACK_FORMAT =
        new AudioFormat(44100, 16, 1, true, false);

    private static final int DRUM_TRACK = 1;

    public static final float GRAVITY = 0.002f;

    private Point pointCache = new Point();
    private TileMap map;
    private MidiPlayer midiPlayer;
    private SoundManager soundManager;
    private ResourceManager resourceManager;
    private Sound prizeSound;
    private Sound boopSound;
    private InputManager inputManager;
    private TileMapRenderer renderer;

    private GameAction moveLeft;
    private GameAction moveRight;
    private GameAction jump;
    private GameAction exit;
    private GameAction shoot;
    private GameAction down;
    
    // My variables
    private float prevX = -1;
    private float deltaX = 0;
    private long deltaT = 0;
    private String file = null;
    private int score = 0;
    
    private float invX = 0;
    private float invT = 0;
    private boolean inv = false;
    private float invdT = 0;
    
    private float gasX = 0;
    private float gasT = 0;
    private boolean gas = false; 

    public void init(String gamefile) {
        super.init(gamefile);
        file = gamefile;
        // set up input manager
        initInput();

        // start resource manager
        resourceManager = new ResourceManager(
        screen.getFullScreenWindow().getGraphicsConfiguration());

        // load resources
        renderer = new TileMapRenderer();
        renderer.setBackground(
            resourceManager.loadImage("background.png"));

        // load first map
        if (file == null){
        	map = resourceManager.loadNextMap();
        }
        else{
        	map = resourceManager.loadFromFile(gamefile);
        }
        
        // load sounds
        soundManager = new SoundManager(PLAYBACK_FORMAT);
        prizeSound = soundManager.getSound("sounds/prize.wav");
        boopSound = soundManager.getSound("sounds/boop2.wav");

        // start music
        midiPlayer = new MidiPlayer();
        Sequence sequence =
            midiPlayer.getSequence("sounds/music.midi");
        midiPlayer.play(sequence, true);
        toggleDrumPlayback();
    }


    /**
        Closes any resources used by the GameManager.
    */
    public void stop() {
        super.stop();
        midiPlayer.close();
        soundManager.close();
    }


    private void initInput() {
        moveLeft = new GameAction("moveLeft");
        moveRight = new GameAction("moveRight");
        jump = new GameAction("jump",GameAction.DETECT_INITAL_PRESS_ONLY);
        exit = new GameAction("exit",GameAction.DETECT_INITAL_PRESS_ONLY);
        shoot = new GameAction("shoot");
        down = new GameAction("down", GameAction.DETECT_INITAL_PRESS_ONLY);

        inputManager = new InputManager(screen.getFullScreenWindow());
        inputManager.setCursor(InputManager.INVISIBLE_CURSOR);

        inputManager.mapToKey(moveLeft, KeyEvent.VK_LEFT);
        inputManager.mapToKey(moveRight, KeyEvent.VK_RIGHT);
        inputManager.mapToKey(jump, KeyEvent.VK_SPACE);
        inputManager.mapToKey(jump, KeyEvent.VK_UP);
        inputManager.mapToKey(exit, KeyEvent.VK_ESCAPE);
        inputManager.mapToKey(shoot, KeyEvent.VK_S);
        inputManager.mapToKey(down, KeyEvent.VK_DOWN);
    }


    private void checkInput(long elapsedTime) {

        if (exit.isPressed()) {
            stop();
        }

        Player player = (Player)map.getPlayer();
        if (player.isAlive()) {
            float velocityX = 0;
            if (moveLeft.isPressed()) {
                velocityX-=player.getMaxSpeed();
                player.direction = -1;
            }
            if (moveRight.isPressed()) {
                velocityX+=player.getMaxSpeed();
                player.direction = 1;
            }
            if (jump.isPressed()) {
                player.jump(false);
            }
            if (down.isPressed()){
            	down();
            }
            
            if (shoot.isPressed() && !gas) {
            	if (player.bullets >= 10){
            		player.canShoot = false;
            	}
            	
            	if (player.dT > 300 && player.canShoot == true){
	            	player.bullets += 1;
	            	playerShoot(player);
	            	player.dT = 0;
            	}
            	else if (player.dT >= 1000 && player.canShoot == false){
            		playerShoot(player);
            		player.canShoot = true;
            		player.dT = 0;
            		player.bullets = 0;
            	}

            }
            else{
            	player.bullets = 0;
            }
            player.dT += elapsedTime;
            
            player.setVelocityX(velocityX);
        }

    }
    
    public void down(){
    	//System.out.println("DOWN");
    	Creature sprite = (Creature)map.getPlayer();
    	float newX = sprite.getX();
    	float newY = sprite.getY();
    	
    	float fromX = Math.min(sprite.getX(), newX);
        float fromY = Math.min(sprite.getY(), newY);
        float toX = Math.max(sprite.getX(), newX);
        float toY = Math.max(sprite.getY(), newY);

        // get the tile locations
        int fromTileX = TileMapRenderer.pixelsToTiles(fromX);
        int fromTileY = TileMapRenderer.pixelsToTiles(fromY);
        int toTileX = TileMapRenderer.pixelsToTiles(
            toX + sprite.getWidth() - 1);
        int toTileY = TileMapRenderer.pixelsToTiles(
            toY + sprite.getHeight() - 0);

        // check each tile for a collision
        for (int x=fromTileX; x<=toTileX; x++) {
            for (int y=fromTileY; y<=toTileY; y++) {
                if (x < 0 || x >= map.getWidth() ||
                    map.getTile(x, y) != null)
                {
                    // collision found, return the tile
                	if (map.getTile(x, y+1) == null){
                		float h = TileMapRenderer.tilesToPixels(map.getHeight());
                		
                		if (newY + 64 + sprite.getHeight() < h){
                			sprite.setY(newY + 64 + sprite.getHeight());
                		}
                	}
                }
            }
        }
    }
    
    public void playerShoot(Player player){
    	Bullet bullet = (Bullet) resourceManager.bulletSprite.clone();
    	bullet.setX(player.getX() + 100*player.direction);
    	bullet.setY(player.getY());
    	bullet.direction = player.direction;
    	
    	int x = TileMapRenderer.pixelsToTiles(player.getX() + 100 * player.direction);
		int y = TileMapRenderer.pixelsToTiles(player.getY());
		resourceManager.addBullet(map, bullet, x, y, player.direction);
    	soundManager.play(boopSound);
    }


    public void draw(Graphics2D g) {
        renderer.draw(g, map,
            screen.getWidth(), screen.getHeight());
        g.drawString("Hello World!", screen.getWidth(), screen.getHeight());
    }


    /**
        Gets the current map.
    */
    public TileMap getMap() {
        return map;
    }


    /**
        Turns on/off drum playback in the midi music (track 1).
    */
    public void toggleDrumPlayback() {
        Sequencer sequencer = midiPlayer.getSequencer();
        if (sequencer != null) {
            sequencer.setTrackMute(DRUM_TRACK,
                !sequencer.getTrackMute(DRUM_TRACK));
        }
    }


    /**
        Gets the tile that a Sprites collides with. Only the
        Sprite's X or Y should be changed, not both. Returns null
        if no collision is detected.
    */
    public Point getTileCollision(Sprite sprite,
        float newX, float newY)
    {
        float fromX = Math.min(sprite.getX(), newX);
        float fromY = Math.min(sprite.getY(), newY);
        float toX = Math.max(sprite.getX(), newX);
        float toY = Math.max(sprite.getY(), newY);

        // get the tile locations
        int fromTileX = TileMapRenderer.pixelsToTiles(fromX);
        int fromTileY = TileMapRenderer.pixelsToTiles(fromY);
        int toTileX = TileMapRenderer.pixelsToTiles(
            toX + sprite.getWidth() - 1);
        int toTileY = TileMapRenderer.pixelsToTiles(
            toY + sprite.getHeight() - 1);

        // check each tile for a collision
        for (int x=fromTileX; x<=toTileX; x++) {
            for (int y=fromTileY; y<=toTileY; y++) {
                if (x < 0 || x >= map.getWidth() ||
                    map.getTile(x, y) != null)
                {
                    // collision found, return the tile
                    pointCache.setLocation(x, y);
                    return pointCache;
                }
            }
        }

        // no collision found
        return null;
    }


    /**
        Checks if two Sprites collide with one another. Returns
        false if the two Sprites are the same. Returns false if
        one of the Sprites is a Creature that is not alive.
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
        Gets the Sprite that collides with the specified Sprite,
        or null if no Sprite collides with the specified Sprite.
    */
    public Sprite getSpriteCollision(Sprite sprite) {

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
        Updates Animation, position, and velocity of all Sprites
        in the current map.
    */
    public void update(long elapsedTime) {
        //Creature player = (Creature)map.getPlayer();
        Player player = (Player) map.getPlayer();
        if (prevX == -1){
        	prevX = player.getX();
        }
        
        // --- start invincible check --------
        //if (player.invincible){
        if (inv){
        	invdT += elapsedTime;
        	if ((invdT > 5000) || (invX > 10)){
        		inv = false;
        		invX = 0;
        		invT = 0;
        		invdT = 0;
        	}
        }
        
        // --- end invincible check ---------
        
        // gas start
        if (gas){
            gasT += elapsedTime; 
            if ((gasT > 5000) || (gasX > 10)){ 
                gas = false; 
                gasX = 0; 
                gasT = 0; 
            } 
        }
        // gas end
        
        // player health
        player.checkHealth();
        deltaX = Math.abs(player.getX() - prevX);
        
        
        deltaT += elapsedTime;
        if (deltaX > player.getWidth()){
        	player.boostHealth(1);
        	prevX = player.getX();
        	deltaT = 0;
        	if (inv){
        		invX++;
        	}
        	if (gas){
                gasX++;
            } 
        }

        if (deltaT > 1000 && deltaX < player.getWidth()){
        	player.boostHealth(5);
        	deltaT = 0;
        }
     
        // player is dead! start map over
        if (player.getState() == Creature.STATE_DYING) {
            if (file == null){
            	map = resourceManager.reloadMap();
            }
            else{
            	map = resourceManager.loadFromFile(file);
            }
            return;
        }

        // get keyboard/mouse input
        checkInput(elapsedTime);

        // update player
        updateCreature(player, elapsedTime);
        player.update(elapsedTime);

        // update other sprites
        Iterator i = map.getSprites();
        while (i.hasNext()) {
            Sprite sprite = (Sprite)i.next();
            if (sprite instanceof Creature) {
                Creature creature = (Creature)sprite;
                if (creature.getState() == Creature.STATE_DEAD) {
                    i.remove();
                }
                else {
                    updateCreature(creature, elapsedTime);
                }
            }
            sprite.update(elapsedTime);
        }
        
        // Bullet updates
        Iterator j = map.getBullets();
        while (j.hasNext()){
        	Bullet b = (Bullet) j.next();
        	if (updateBullet(b, elapsedTime)){
        		j.remove();
        	}
        }
        
    }
    
    private boolean updateBullet(Bullet bullet, long elapsedTime){
        float dx = 0.8f * bullet.direction;
        float oldX = bullet.getX();
        float newX = oldX + dx * elapsedTime;
        bullet.travel += Math.abs(newX - oldX);
        Player player = (Player) map.getPlayer();
        if (bullet.travel > 5*player.getWidth()){
        	return true;
        }
        
        Point tile =getTileCollision(bullet, newX, bullet.getY());
        if (tile == null) {
            bullet.setX(newX);
            return false;
        }
        else {
        	return true;
        }
        
    }


    /**
        Updates the creature, applying gravity for creatures that
        aren't flying, and checks collisions.
    */
    private void updateCreature(Creature creature,
        long elapsedTime)
    {
    	//update direction
    	if (creature.getVelocityX() > 0){
    		creature.direction = 1;
    	}
    	else{
    		creature.direction = -1;
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
        }
        else {
            // line up with the tile boundary
            if (dx > 0) {
                creature.setX(
                    TileMapRenderer.tilesToPixels(tile.x) -
                    creature.getWidth());
            }
            else if (dx < 0) {
                creature.setX(
                    TileMapRenderer.tilesToPixels(tile.x + 1));
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
        }
        else {
            // line up with the tile boundary
            if (dy > 0) {
                creature.setY(
                    TileMapRenderer.tilesToPixels(tile.y) -
                    creature.getHeight());
            }
            else if (dy < 0) {
                creature.setY(
                    TileMapRenderer.tilesToPixels(tile.y + 1));
            }
            creature.collideVertical();
        }
        if (creature instanceof Player) {
            boolean canKill = (oldY < creature.getY());
            checkPlayerCollision((Player)creature, canKill);
        }
        if (creature instanceof Grub){
        	checkSpriteCollision(creature);
        	if (creature.getVelocityX() != 0 && deltaX != 0){
	        	if (creature.dT > 600){
	        		creatureShoot(creature);
	        		creature.dT = 0;
	        	}
	        	else{
	        		creature.dT += elapsedTime;
	        	}
        	}
        }
        if (creature instanceof Fly){
        	checkSpriteCollision(creature);
        	if (creature.getVelocityX() != 0 && deltaX != 0){
	        	if (creature.dT > 600){
	        		creatureShoot(creature);
	        		creature.dT = 0;
	        	}
	        	else{
	        		creature.dT += elapsedTime;
	        	}
        	}
        }

    }
    
    public void creatureShoot(Creature creature){
    	Bullet bullet = (Bullet) resourceManager.bulletSprite.clone();
    	bullet.setX(creature.getX() + 65 * creature.direction);
    	bullet.setY(creature.getY());
    	bullet.direction = creature.direction;
    	bullet.fromPlayer = false;

    	int x = TileMapRenderer.pixelsToTiles(creature.getX() + 65 * creature.direction);
		int y = TileMapRenderer.pixelsToTiles(creature.getY());
    	resourceManager.addBullet(map, bullet, x, y, creature.direction);
    }


    /**
        Checks for Player collision with other Sprites. If
        canKill is true, collisions with Creatures will kill
        them.
    */
    public void checkPlayerCollision(Player player,
        boolean canKill)
    {
        if (!player.isAlive()) {
            return;
        }

        // check for player collision with other sprites
        Sprite collisionSprite = getSpriteCollision(player);
        if (collisionSprite instanceof PowerUp) {
            acquirePowerUp((PowerUp)collisionSprite);
        }
        if (collisionSprite instanceof SpecialBlock) {
            triggerSpecialBlock((SpecialBlock)collisionSprite);
        }
        else if (collisionSprite instanceof Creature) {
            Creature badguy = (Creature)collisionSprite;
            soundManager.play(boopSound);
            badguy.setState(Creature.STATE_DYING);

            if (canKill) {
                player.jump(true);
                player.updateScore(5);
            }
            else {
            	if (!inv){
            		player.setState(Creature.STATE_DYING);
            	}
            }
        }
        
        Iterator i = map.getBullets();
        while (i.hasNext()) {
            Bullet b = (Bullet) i.next();
            if (isCollision(player, b)) {
            	i.remove();
            	if (!inv){
            		player.healthHurt(5);
            	}
            }
        }
    }
    
    /**
     	Checks for Sprite collision with Bullet
     */
    public void checkSpriteCollision(Creature creature){
    	//For sprite, check if it hit any bullet
    	boolean result = getBulletCollision(creature);
    	if (result){
    		soundManager.play(prizeSound,
                    new EchoFilter(2000, .7f), false);
            creature.setState(Creature.STATE_DYING);
            Player player = (Player) map.getPlayer();
            player.updateScore(5);
    	}
    }
    
    public boolean getBulletCollision(Creature creature) {

        // run through the list of Sprites
        Iterator i = map.getBullets();
        while (i.hasNext()) {
            Bullet b = (Bullet)i.next();
            if (isCollision(creature, b) && b.fromPlayer == true) {
        		i.remove();
        		Player player = (Player) map.getPlayer();
        		player.boostHealth(10);
        		return true;
            	
            }
        }

        // no collision found
        return false;
    }


    /**
        Gives the player the specified power up and removes it
        from the map.
    */
    public void acquirePowerUp(PowerUp powerUp) {
        // remove it from the map
        map.removeSprite(powerUp);
        
        if (powerUp instanceof PowerUp.Star) {
            //soundManager.play(prizeSound);
        	Player player = (Player) map.getPlayer();
        	inv = true;
            invT = System.currentTimeMillis();
        }
        else if (powerUp instanceof PowerUp.Mushroom){
        	soundManager.play(prizeSound);
        	Player player = (Player) map.getPlayer();
        	player.boostHealth(5);
        }
        else if (powerUp instanceof PowerUp.Music) {
            soundManager.play(prizeSound);
            toggleDrumPlayback();
        }
        else if (powerUp instanceof PowerUp.Goal) {
            // advance to next map
        	Player player = (Player) map.getPlayer();
        	score = player.getScore();
        	
            soundManager.play(prizeSound,new EchoFilter(2000, .7f), false);
            map = resourceManager.loadNextMap();
            
            Player newplayer = (Player) map.getPlayer();
            newplayer.updateScore(score);
        }
    }
    public void triggerSpecialBlock(SpecialBlock specialBlock) {
        if (specialBlock instanceof SpecialBlock.Explode){
        	//soundManager.play(prizeSound);
            map.removeSprite(specialBlock);
        	Player player = (Player) map.getPlayer();
        	player.healthHurt(10);
        }       
        if (specialBlock instanceof SpecialBlock.Gas){
        	map.removeSprite(specialBlock);
        	gas = true;
        }    
        
    }
    
}

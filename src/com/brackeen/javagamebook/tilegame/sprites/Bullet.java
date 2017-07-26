package com.brackeen.javagamebook.tilegame.sprites;
import java.lang.reflect.Constructor;

import com.brackeen.javagamebook.graphics.Animation;
import com.brackeen.javagamebook.graphics.Sprite;

public class Bullet extends Sprite{

	private Animation left;
    private Animation right;
    private Animation deadLeft;
    private Animation deadRight;
    //private int direction;
    public float direction = 1;
    public boolean fromPlayer = true;
    public boolean canShoot = true;
    public float shotTime;
    public float travel = 0;
    
	public Bullet (Animation left, Animation right,Animation deadLeft, Animation deadRight){
		super(right);
        this.left = left;
        this.right = right;
        this.deadLeft = deadLeft;
        this.deadRight = deadRight;
	    }
	
	public Object clone() {
        // use reflection to create the correct subclass
        Constructor constructor = getClass().getConstructors()[0];
        try {
            return constructor.newInstance(new Object[] {
                (Animation)left.clone(),
                (Animation)right.clone(),
                (Animation)deadLeft.clone(),
                (Animation)deadRight.clone()
            });
        }
        catch (Exception ex) {
            // should never happen
            ex.printStackTrace();
            return null;
        }
    }
}
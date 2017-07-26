package com.brackeen.javagamebook.tilegame.sprites;
import java.lang.reflect.Constructor;
import com.brackeen.javagamebook.graphics.*;


public abstract class SpecialBlock extends Sprite {
    public SpecialBlock(Animation anim) {
        super(anim);
    }
    public Object clone() {
        // use reflection to create the correct subclass
        Constructor constructor = getClass().getConstructors()[0];
        try {
            return constructor.newInstance(
                new Object[] {(Animation)anim.clone()});
        }
        catch (Exception ex) {
            // should never happen
            ex.printStackTrace();
            return null;
        }
    }
    /**
    An Explode SpecialBlock. Decreases health.
    */
    public static class Explode extends SpecialBlock {
    	public Explode(Animation anim) {
    		super(anim);
    	}
    }
    /**
    A Gas SpecialBlock. Decreases health.
    */
    public static class Gas extends SpecialBlock {
    	public Gas(Animation anim) {
    		super(anim);
    	}
    }    
}

package com.if3games.chessonline;

import com.if3games.chessonline.activities.Preferences;
import com.if3games.chessonline.data.ConstantsData;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;

public class MainActivity extends Activity {

	public final static int REQUEST_CODE_MINIGAME = 1;
	
	SharedPreferences settings;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_new);
		
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        settings.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                
            }
        });
        
        Button play = (Button) findViewById(R.id.play);
        play.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
				Intent intent = new Intent();
		    	intent.setClassName(ConstantsData.PACKAGE_NAME , ConstantsData.PACKAGE_NAME + ".DroidFish");
		    	intent.putExtra("gms", 0);
		    	startActivity(intent);    
            }
        });
        
        play.setOnTouchListener(new OnTouchListener()
        {
			@Override
			public boolean onTouch(View view, MotionEvent motion) {
			    switch ( motion.getAction() ) {
			    	case MotionEvent.ACTION_DOWN:
			    		view.setBackgroundResource(R.drawable.button_play_down);
			    		view.startAnimation(buttonUpDownAnimate(true));			    		
			    		break;
			    	case MotionEvent.ACTION_UP: 
			    		view.setBackgroundResource(R.drawable.button_play_new);
			    		view.startAnimation(buttonUpDownAnimate(false));
			    		break;	
			    	case MotionEvent.ACTION_CANCEL:
			    		view.setBackgroundResource(R.drawable.button_play_new);
			    		view.startAnimation(buttonUpDownAnimate(false));
			    		break;
			    }
			    return false;
			}
        });
        
        Button multiplayer = (Button) findViewById(R.id.multiplayer_btn);
        multiplayer.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
				Intent intent = new Intent();
		    	intent.setClassName(ConstantsData.PACKAGE_NAME , ConstantsData.PACKAGE_NAME + ".DroidFish");
		    	intent.putExtra("gms", 1);
		    	startActivity(intent);    
            }
        });
        
        multiplayer.setOnTouchListener(new OnTouchListener()
        {
			@Override
			public boolean onTouch(View view, MotionEvent motion) {
			    switch ( motion.getAction() ) {
			    	case MotionEvent.ACTION_DOWN:
			    		view.setBackgroundResource(R.drawable.button_play_down);
			    		view.startAnimation(buttonUpDownAnimate(true));			    		
			    		break;
			    	case MotionEvent.ACTION_UP: 
			    		view.setBackgroundResource(R.drawable.button_play_new);
			    		view.startAnimation(buttonUpDownAnimate(false));
			    		break;	
			    	case MotionEvent.ACTION_CANCEL:
			    		view.setBackgroundResource(R.drawable.button_play_new);
			    		view.startAnimation(buttonUpDownAnimate(false));
			    		break;
			    }
			    return false;
			}
        });

        Button options = (Button) findViewById(R.id.about);
        options.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, Preferences.class);
                startActivity(i);
            }
        });        
        
        options.setOnTouchListener(new OnTouchListener()
        {
			@Override
			public boolean onTouch(View view, MotionEvent motion) {
			    switch ( motion.getAction() ) {
			    	case MotionEvent.ACTION_DOWN:
			    		view.setBackgroundResource(R.drawable.button_options_down);
			    		view.startAnimation(buttonUpDownAnimate(true));
			    		break;
			    	case MotionEvent.ACTION_UP: 
			    		view.setBackgroundResource(R.drawable.button_options_new);
			    		view.startAnimation(buttonUpDownAnimate(false));
			    		break;	
			    	case MotionEvent.ACTION_CANCEL:
			    		view.setBackgroundResource(R.drawable.button_options_new);
			    		view.startAnimation(buttonUpDownAnimate(false));
			    		break;
			    }
			    return false;
			}
        });
        
        // Initialize button animations      
        play.startAnimation(stretch(0));
        multiplayer.startAnimation(stretch(1));
        options.startAnimation(stretch(2));
    }
    
    private Animation buttonUpDownAnimate(boolean move) { 
    	Animation animation = null;
	    float y0 = 0.0f;
	    float y1 = ConstantsData.AnimTransDownBtn;;
	    if (move) {
	    	animation = new TranslateAnimation(0.0f, 0.0f, y0, y1);		
	    } else {	
	    	animation = new TranslateAnimation(0.0f, 0.0f, y0, -y1);			
		}
		animation.setDuration(50);
		animation.setFillAfter(true);		
		return animation;
    }
    
	@Override
	public void onStart() {
		super.onStart();
	}
	
	@Override
	public void onBackPressed() {
	    MainActivity.this.finish();
	}
	
	synchronized private void exitMain() { 
	}
	
	private final AnimationListener mTapPlayListener = new AnimationListener() {

		    public void onAnimationEnd(Animation animation) {
		      MainActivity.this.exitMain();
		    }

		    public void onAnimationRepeat(Animation animation) {
		    }

		    public void onAnimationStart(Animation animation) {
		    }
	};
	
	// Show and Hide Animation Play Button
	private Animation showPlayAnim(boolean show) {
		    float x0 = 0.0f;
		    float x1 = -1.0f;
		    Animation slideUp;
		    if (show) {
		      slideUp = new TranslateAnimation(Animation.RELATIVE_TO_SELF, x1,
		          Animation.RELATIVE_TO_SELF, x0, Animation.RELATIVE_TO_SELF, 0.0f,
		          Animation.RELATIVE_TO_SELF, 0.0f);

		      slideUp.setInterpolator(new DecelerateInterpolator());
		    } else {
		      slideUp = new TranslateAnimation(Animation.RELATIVE_TO_SELF, x0,
		          Animation.RELATIVE_TO_SELF, x1, Animation.RELATIVE_TO_SELF, 0.0f,
		          Animation.RELATIVE_TO_SELF, 0.0f);

		      slideUp.setInterpolator(new AccelerateInterpolator());
		    }
		    slideUp.setDuration(250);
		    // make the element maintain its orientation even after the animation
		    // finishes.
		    slideUp.setFillAfter(true);
		    slideUp.setAnimationListener(mTapPlayListener);
		    return slideUp;
	}
	
	
	private AnimationSet stretch(int order) {
		    final int SCALE_UP_DURATION = 400;
		    final int SCALE_DOWN_DURATION = 200;
		    final int SCALE_NORMAL_DURATION = 100;
		    final float SCALE_START = 0.0f;
		    final float SCALE_MAX = 1.20f;
		    final float SCALE_MIN = 0.8f;
		    final float SCALE_NORMAL = 1.0f;

		    AnimationSet set = new AnimationSet(true);
		    set.setInterpolator(new DecelerateInterpolator());

		    ScaleAnimation scaleUp = new ScaleAnimation(SCALE_START, SCALE_MAX,
		        SCALE_START, SCALE_MAX, Animation.RELATIVE_TO_SELF, 0.5f,
		        Animation.RELATIVE_TO_SELF, 0.5f);
		    scaleUp.setDuration(SCALE_UP_DURATION);
		    ScaleAnimation scaleDown = new ScaleAnimation(SCALE_MAX, SCALE_MIN,
		        SCALE_MAX, SCALE_MIN, Animation.RELATIVE_TO_SELF, 0.5f,
		        Animation.RELATIVE_TO_SELF, 0.5f);
		    scaleDown.setDuration(SCALE_DOWN_DURATION);
		    scaleDown.setStartOffset(SCALE_UP_DURATION);

		    ScaleAnimation scaleNormal = new ScaleAnimation(SCALE_MIN, SCALE_NORMAL,
		        SCALE_MIN, SCALE_NORMAL, Animation.RELATIVE_TO_SELF, 0.5f,
		        Animation.RELATIVE_TO_SELF, 0.5f);
		    scaleNormal.setDuration(SCALE_NORMAL_DURATION);
		    scaleNormal.setStartOffset(SCALE_UP_DURATION + SCALE_DOWN_DURATION);

		    set.addAnimation(scaleUp);
		    set.addAnimation(scaleDown);
		    set.addAnimation(scaleNormal);

		    set.setStartOffset(order * 200);

		    return set;
		  }
    
}

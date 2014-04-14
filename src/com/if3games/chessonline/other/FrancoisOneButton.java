package com.if3games.chessonline.other;

import java.util.Locale;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.Button;

public class FrancoisOneButton extends Button {

  public FrancoisOneButton(Context context) {
    super(context);
    setCustomFont();
  }

  public FrancoisOneButton(Context context, AttributeSet attrs) {
    super(context, attrs);
    setCustomFont();
  }

  /*
  public FrancoisOneButton(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    setCustomFont();
  }
  */

  /*
   * Helper function to set the custom typeface of this view.
   */
  private void setCustomFont()
  {
    // Wrap in isEditMode so that xml previewer doesn't break.
    if (!this.isInEditMode()) {
      Typeface typeface = Typeface.createFromAsset(this.getContext()
          .getAssets(), "fonts/OpenSans-Bold.ttf");
      setTypeface(typeface);
    }
    
    // Set buttons to always use upper case
    this.setText(this.getText().toString().toUpperCase(Locale.getDefault()));
  }
}

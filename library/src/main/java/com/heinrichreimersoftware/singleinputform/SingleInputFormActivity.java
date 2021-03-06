/*
 * MIT License
 *
 * Copyright (c) 2017 Jan Heinrich Reimer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.heinrichreimersoftware.singleinputform;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Property;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewAnimator;
import android.widget.ViewSwitcher;

import com.heinrichreimersoftware.singleinputform.steps.Step;

import java.util.ArrayList;
import java.util.List;

public abstract class SingleInputFormActivity extends AppCompatActivity {

	private static final String KEY_DATA = "key_data";
	private static final String KEY_STEP_INDEX = "key_step_index";

	private static final Property<ProgressBar, Integer> PB_PROGRESS_PROPERTY =
			new Property<ProgressBar, Integer>(Integer.class, "PB_PROGRESS_PROPERTY"){

				@Override
				public void set(ProgressBar pb, Integer value){
					pb.setProgress(value);
				}

				@Override
				public Integer get(ProgressBar pb){
					return pb.getProgress();
				}
			};

    private List<Step> steps = new ArrayList<>();
    private Bundle setupData = new Bundle();
	private int stepIndex = 0;
	private boolean error;

    private FrameLayout container;
    private ScrollView containerScrollView;
    private LinearLayout innerContainer;
    private TextSwitcher titleSwitcher;
	private TextSwitcher errorSwitcher;
	private TextSwitcher detailsSwitcher;
	private CardView textField;
	private ViewAnimator inputSwitcher;
	private ImageButton nextButton;
	private ProgressBar progress;
	private TextView stepText;

	private View.OnClickListener nextButtonClickListener = new View.OnClickListener(){
		@Override
		public void onClick(View v){
			nextStep();
		}
	};

	private Drawable buttonNextIcon;
	private Drawable buttonFinishIcon;

	private int textFieldBackgroundColor = -1;
	private int progressBackgroundColor = -1;

	private int titleTextColor = -1;
	private int detailsTextColor = -1;
	private int errorTextColor = -1;

	@Override
	public void onBackPressed(){
		if(stepIndex == 0){
			finish();
		}
		else{
			previousStep();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_single_input_form);

		loadTheme();

        findViews();

        steps = onCreateSteps();

		if(savedInstanceState != null){
			setupData = savedInstanceState.getBundle(KEY_DATA);
			stepIndex = savedInstanceState.getInt(KEY_STEP_INDEX, 0);
		}

		setupTitle();
		setupInput();
		setupError();
        setupDetails();

		nextButton.setOnClickListener(nextButtonClickListener);
		errorSwitcher.setText("");
        updateStep();
	}

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
	public void onRestoreInstanceState(Bundle savedInstanceState){
		super.onSaveInstanceState(savedInstanceState);
		if(savedInstanceState != null){
			setupData = savedInstanceState.getBundle(KEY_DATA);
			stepIndex = savedInstanceState.getInt(KEY_STEP_INDEX, 0);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		setupData = getCurrentStep().save(setupData);
		outState.putBundle(KEY_DATA, setupData);
		outState.putInt(KEY_STEP_INDEX, stepIndex);
	}

	@Override
	protected void onPause() {
		hideSoftInput();
		super.onPause();
	}

	protected abstract List<Step> onCreateSteps();

	private void findViews(){
        container = (FrameLayout) findViewById(R.id.container);
        containerScrollView = (ScrollView) findViewById(R.id.containerScrollView);
        innerContainer = (LinearLayout) findViewById(R.id.innerContainer);
        titleSwitcher = (TextSwitcher) findViewById(R.id.titleSwitcher);
		errorSwitcher = (TextSwitcher) findViewById(R.id.errorSwitcher);
		detailsSwitcher = (TextSwitcher) findViewById(R.id.detailsSwitcher);
        textField = (CardView) findViewById(R.id.textField);
		inputSwitcher = (ViewAnimator) findViewById(R.id.inputSwitcher);
		nextButton = (ImageButton) findViewById(R.id.nextButton);
		progress = (ProgressBar) findViewById(R.id.progress);
		stepText = (TextView) findViewById(R.id.stepText);
		setProgressDrawable();
	}

	protected Step getCurrentStep(){
		return getStep(stepIndex);
	}

	protected Step getStep(int position){
		return steps.get(position);
	}

    @SuppressWarnings("ResourceType")
    private void loadTheme() {
        /* Default values */
        buttonNextIcon = ContextCompat.getDrawable(this, R.drawable.ic_arrow_forward);
        buttonFinishIcon = ContextCompat.getDrawable(this, R.drawable.ic_done);


		/* Custom values */
		int[] attrs = {R.attr.colorPrimary, R.attr.colorPrimaryDark, android.R.attr.textColorPrimary, android.R.attr.textColorSecondary, R.attr.sifNextIcon, R.attr.sifFinishIcon};
		TypedArray array = obtainStyledAttributes(attrs);

        textFieldBackgroundColor = array.getColor(0, 0);
        progressBackgroundColor = array.getColor(1, 0);
        titleTextColor = errorTextColor = array.getColor(2, 0);
        detailsTextColor = array.getColor(3, 0);

        Drawable buttonNextIcon = array.getDrawable(4);
        if(buttonNextIcon != null){
            this.buttonNextIcon = buttonNextIcon;
        }

        Drawable buttonFinishIcon = array.getDrawable(5);
        if(buttonFinishIcon != null){
            this.buttonFinishIcon = buttonFinishIcon;
        }

        array.recycle();
	}

    private Animation getAnimation(int animationResId, boolean isInAnimation){
        final Interpolator interpolator;

        if(isInAnimation){
            interpolator = new DecelerateInterpolator(1.0f);
        }
        else{
            interpolator = new AccelerateInterpolator(1.0f);
        }

        Animation animation = AnimationUtils.loadAnimation(this, animationResId);
        animation.setInterpolator(interpolator);

        return animation;
    }

	private void setupTitle(){
        titleSwitcher.setInAnimation(getAnimation(R.anim.slide_in_to_bottom, true));
        titleSwitcher.setOutAnimation(getAnimation(R.anim.slide_out_to_top, false));

		titleSwitcher.setFactory(new ViewSwitcher.ViewFactory() {

            @Override
            public View makeView() {
                TextView view = (TextView) getLayoutInflater().inflate(R.layout.view_title, titleSwitcher, false);
                if (view != null) {
                    view.setTextColor(titleTextColor);
                }
                return view;
            }
        });

		titleSwitcher.setText("");
	}

	private void setupInput(){
		inputSwitcher.setInAnimation(getAnimation(R.anim.alpha_in, true));
		inputSwitcher.setOutAnimation(getAnimation(R.anim.alpha_out, false));

		inputSwitcher.removeAllViews();
		for(int i = 0; i < steps.size(); i++){
			inputSwitcher.addView(getStep(i).getView());
		}
	}

	private void setupError(){
        errorSwitcher.setInAnimation(getAnimation(android.R.anim.slide_in_left, true));
        errorSwitcher.setOutAnimation(getAnimation(android.R.anim.slide_out_right, false));

		errorSwitcher.setFactory(new ViewSwitcher.ViewFactory() {

            @Override
            public View makeView() {
                TextView view = (TextView) getLayoutInflater().inflate(R.layout.view_error, titleSwitcher, false);
                if (view != null && errorTextColor != -1) {
                    view.setTextColor(errorTextColor);
                }
                return view;
            }
        });

		errorSwitcher.setText("");
	}

	private void setupDetails(){
        detailsSwitcher.setInAnimation(getAnimation(R.anim.alpha_in, true));
        detailsSwitcher.setOutAnimation(getAnimation(R.anim.alpha_out, false));

		detailsSwitcher.setFactory(new ViewSwitcher.ViewFactory() {

            @Override
            public View makeView() {
                TextView view = (TextView) getLayoutInflater().inflate(R.layout.view_details, titleSwitcher, false);
                if (view != null && detailsTextColor != -1) {
                    view.setTextColor(detailsTextColor);
                }
                return view;
            }
        });

		detailsSwitcher.setText("");
	}

	private void updateStep(){
		if(stepIndex >= steps.size()){
			hideSoftInput();

            View finishedView = onCreateFinishedView(getLayoutInflater(), container);
            if(finishedView != null){
				finishedView.setAlpha(0);
				finishedView.setVisibility(View.VISIBLE);
				container.addView(finishedView);
				finishedView.animate()
						.alpha(1)
						.setDuration(getResources().getInteger(
								android.R.integer.config_mediumAnimTime));
			}

			onFormFinished(setupData);
			return;
		}
		updateViews();
		containerScrollView.smoothScrollTo(0, 0);
	}

	private void hideSoftInput(){
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

		View v = getCurrentFocus();
		if(v == null) return;

		imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
	}

    protected View onCreateFinishedView(LayoutInflater inflater, ViewGroup parent){
        return null;
    }

    protected abstract void onFormFinished(Bundle data);

	private void updateViews(){
		Step step = getCurrentStep();

		if(stepIndex + 1 >= steps.size()){
			nextButton.setImageDrawable(buttonFinishIcon);
            nextButton.setContentDescription(getString(R.string.finish));
			step.updateView(true);
		}
		else{
			nextButton.setImageDrawable(buttonNextIcon);
            nextButton.setContentDescription(getString(R.string.next_step));
			step.updateView(false);
		}

		step.restore(setupData);

		setTextFieldBackgroundDrawable();

		inputSwitcher.setDisplayedChild(stepIndex);
		errorSwitcher.setText("");
		detailsSwitcher.setText(step.getDetails(this));
		titleSwitcher.setText(step.getTitle(this));
		stepText.setText(getString(R.string.page_number, stepIndex + 1, steps.size()));

        stepText.setTextColor(detailsTextColor);

		updateProgressbar();
	}

	private void setTextFieldBackgroundDrawable(){
        if(textFieldBackgroundColor != -1) {
            textField.setCardBackgroundColor(textFieldBackgroundColor);
        }
	}

	private void setProgressDrawable(){
        if(progressBackgroundColor != -1) {
            Drawable progressDrawable = progress.getProgressDrawable();
            if (progressDrawable != null) {
                progressDrawable.setColorFilter(progressBackgroundColor, PorterDuff.Mode.SRC_IN);
            }
        }
	}

	private void updateProgressbar(){
		progress.setMax(steps.size() * 100);
		ObjectAnimator.ofInt(progress, PB_PROGRESS_PROPERTY, stepIndex * 100).start();
	}

	protected void previousStep(){
		setupData = getCurrentStep().save(setupData);
		stepIndex--;
		updateStep();
	}

	protected void nextStep(){
		Step step = getCurrentStep();
		boolean checkStep = checkStep();
		if(!checkStep){
			if(!error){
				error = true;
				errorSwitcher.setText(step.getError(this));
			}
		}
		else{
			error = false;
		}
		if(error){
			return;
		}
		setupData = step.save(setupData);

		stepIndex++;
		updateStep();
	}

	private boolean checkStep(){
		return getCurrentStep().validate();
	}

    public void setInputGravity(int gravity) {
		ScrollView.LayoutParams layoutParams = (ScrollView.LayoutParams) innerContainer.getLayoutParams();
		layoutParams.gravity = gravity;
		innerContainer.setLayoutParams(layoutParams);
	}

    public int getInputGravity() {
		ScrollView.LayoutParams layoutParams = (ScrollView.LayoutParams) innerContainer.getLayoutParams();
		return layoutParams.gravity;
	}
}
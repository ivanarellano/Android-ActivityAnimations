/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.support.android.designlibdemo;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

public class CheeseDetailActivity extends AppCompatActivity {

    public static final String EXTRA_NAME = "cheese_name";
    public static final String VIEW_INFO_EXTRA = "VIEW_INFO_EXTRA";
    public static final String BACKDROP_ID = "BACKDROP_ID";

    private static final int DEFAULT_DURATION = 350;
    private static final FastOutLinearInInterpolator DEFAULT_INTERPOLATOR = new FastOutLinearInInterpolator();

    @DrawableRes private int backdropId;
    private int leftDelta, topDelta;
    private float widthScale, heightScale;

    private ViewInfoExtras viewInfoExtras;
    private ImageView tempSharedElementView;
    private ImageView destinationView;
    private View scrollView;
    private View fab;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_detail);

        scrollView = findViewById(R.id.ll_list);
        tempSharedElementView = (ImageView) findViewById(R.id.temp_backdrop);
        destinationView = (ImageView)findViewById(R.id.circle_backdrop);
        fab = findViewById(R.id.fab);

        backdropId = getIntent().getIntExtra(BACKDROP_ID, android.R.drawable.ic_delete);

        Bundle b = getIntent().getBundleExtra(VIEW_INFO_EXTRA);
        if (null != b) {
            viewInfoExtras = new ViewInfoExtras();
            viewInfoExtras.parseBundle(b);
            onUiReady();
        }

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(getIntent().getStringExtra(EXTRA_NAME));

        loadBackdrop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sample_actions, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        runExitAnimation();
    }

    public void onUiReady() {
        tempSharedElementView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                tempSharedElementView.getViewTreeObserver().removeOnPreDrawListener(this);
                destinationView.setVisibility(View.INVISIBLE);
                prepareScene();
                runEnterAnimation();
                return true;
            }
        });
    }

    private void prepareScene() {
        tempSharedElementView.setPivotX(0);
        tempSharedElementView.setPivotY(0);

        // Position the destination view where the original view was
        int[] screenLocation = new int[2];
        tempSharedElementView.getLocationOnScreen(screenLocation);

        leftDelta = viewInfoExtras.left - screenLocation[0];
        topDelta = viewInfoExtras.top - screenLocation[1];
        tempSharedElementView.setTranslationX(leftDelta);
        tempSharedElementView.setTranslationY(topDelta);

        // Scale the destination view to be the same size as the original view
        int height = viewInfoExtras.height;
        int width = viewInfoExtras.width;
        widthScale = (float) width / destinationView.getWidth();
        heightScale = (float) height / destinationView.getHeight();
        tempSharedElementView.setScaleX(widthScale);
        tempSharedElementView.setScaleY(heightScale);
    }

    private void runEnterAnimation() {
        int[] screenLocation = new int[2];
        destinationView.getLocationOnScreen(screenLocation);

        destinationView.setVisibility(View.INVISIBLE);
        // Now simply animate to the default positions
        tempSharedElementView.animate()
                .setDuration(DEFAULT_DURATION)
                .setInterpolator(DEFAULT_INTERPOLATOR)
                .scaleX(1)
                .scaleY(1)
                .translationX(screenLocation[0])
                .translationY(screenLocation[1])
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing()) {
                            tempSharedElementView.setVisibility(View.GONE);
                            destinationView.setVisibility(View.VISIBLE);

                            fab.animate().withLayer().setInterpolator(DEFAULT_INTERPOLATOR).alpha(1).start();
                            scrollView.animate().withLayer().setInterpolator(DEFAULT_INTERPOLATOR).alpha(1).start();
                        }
                    }
                })
                .start();
    }

    private void runExitAnimation() {
        destinationView.setVisibility(View.GONE);
        tempSharedElementView.setVisibility(View.VISIBLE);

        fab.animate().withLayer().setDuration(DEFAULT_DURATION).setInterpolator(DEFAULT_INTERPOLATOR).alpha(0).start();
        scrollView.animate().withLayer().setDuration(DEFAULT_DURATION).setInterpolator(DEFAULT_INTERPOLATOR).alpha(0).start();

        tempSharedElementView.animate()
                .setDuration(DEFAULT_DURATION)
                .setInterpolator(DEFAULT_INTERPOLATOR)
                .scaleX(widthScale)
                .scaleY(heightScale)
                .translationX(leftDelta)
                .translationY(topDelta)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                        overridePendingTransition(0, 0);
                    }
                })
                .start();
    }

    private void loadBackdrop() {
        Glide.with(this)
             .load(backdropId)
             .asBitmap()
             .centerCrop()
             .into(new BitmapImageViewTarget(destinationView) {
                 @Override
                 public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                     super.onResourceReady(resource, glideAnimation);
                     destinationView.setVisibility(View.INVISIBLE);
                 }
             });

        Glide.with(this)
            .load(backdropId)
            .centerCrop()
            .into(tempSharedElementView);
    }

    final static class ViewInfoExtras {
        public static String WIDTH = "width";
        public static String HEIGHT = "height";
        public static String LEFT = "left";
        public static String TOP = "top";

        private int width, height, left, top;

        public static Bundle createBundle(int width, int height, int left, int top) {
            Bundle b = new Bundle();
            b.putInt(LEFT, left);
            b.putInt(TOP, top);
            b.putInt(WIDTH, width);
            b.putInt(HEIGHT, height);
            return b;
        }

        public void parseBundle(Bundle b) {
            left = b.getInt(LEFT);
            top = b.getInt(TOP);
            width = b.getInt(WIDTH);
            height = b.getInt(HEIGHT);
        }
    }
}

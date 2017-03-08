/*
 * Copyright 2017, The Android Open Source Project
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

package com.example.android.flatfoot.codelab.flatfootcodelab.step3_solution;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.TextView;

import com.android.support.lifecycle.LifecycleActivity;
import com.android.support.lifecycle.Observer;
import com.android.support.lifecycle.ViewModelStore;
import com.example.android.flatfoot.codelab.flatfootcodelab.R;

public class ChronoActivity3 extends LifecycleActivity {

    private LiveDataTimerViewModel chronometerViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.chrono_activity_3);

        chronometerViewModel
                = ViewModelStore.get(this, "ChronometerViewModel", LiveDataTimerViewModel.class);

        subscribe();
    }

    private void subscribe() {
        final Observer<Long> elapsedTimeObserver = new Observer<Long>() {
            @Override
            public void onChanged(@Nullable final Long aLong) {
                String newText = ChronoActivity3.this.getResources().getString(R.string.seconds, aLong);
                ((TextView) findViewById(R.id.timer_textview)).setText(newText);
                Log.d("ChronoActivity3", "Updating timer");
            }
        };

        chronometerViewModel.getElapsedTime().observe(this, elapsedTimeObserver);
    }
}

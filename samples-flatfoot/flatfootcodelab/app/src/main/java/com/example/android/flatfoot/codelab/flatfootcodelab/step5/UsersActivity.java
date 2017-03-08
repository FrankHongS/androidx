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

package com.example.android.flatfoot.codelab.flatfootcodelab.step5;

import android.os.Bundle;
import android.widget.TextView;

import com.android.support.lifecycle.LifecycleActivity;
import com.example.android.flatfoot.codelab.flatfootcodelab.R;
import com.example.android.flatfoot.codelab.flatfootcodelab.orm_db.AppDatabase;
import com.example.android.flatfoot.codelab.flatfootcodelab.orm_db.User;
import com.example.android.flatfoot.codelab.flatfootcodelab.orm_db.utils.DatabaseInitializer;

import java.util.List;
import java.util.Locale;

public class UsersActivity extends LifecycleActivity {

    private AppDatabase mDb;

    private TextView mYoungUsersTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.db_activity1);

        mYoungUsersTextView = (TextView) findViewById(R.id.young_users_tv);

        mDb = AppDatabase.getInMemoryDatabase(getApplicationContext());

        populateDb();

        fetchData();
    }

    private void populateDb() {
        DatabaseInitializer.populateSync(mDb);
    }

    private void fetchData() {
        // Note: this kind of logic should not be in an activity.
        StringBuilder sb = new StringBuilder();
        List<User> youngUsers = mDb.userModel().findYoungerThan(35);
        for (User youngUser : youngUsers) {
            sb.append(String.format(Locale.US,
                    "%s, %s (%d)\n", youngUser.lastName, youngUser.name, youngUser.age));
        }
        mYoungUsersTextView.setText(sb);
    }
}

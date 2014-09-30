/*
 * Copyright 2014 Google Inc. All rights reserved.
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
package org.votingsystem.android.ui.debug.actions;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import org.votingsystem.android.ui.NfcBadgeActivity;
import org.votingsystem.android.ui.debug.DebugAction;


/**
 * Simulates a badge scan. For debug/testing purposes.
 */
public class SimulateBadgeScannedAction implements DebugAction {


    @Override
    public void run(final Context context, final Callback callback) {
        final String url = null;
        context.startActivity(new Intent(NfcBadgeActivity.ACTION_SIMULATE, Uri.parse(url),
        context, NfcBadgeActivity.class));
        Toast.makeText(context, "Simulating badge scan: " + url, Toast.LENGTH_LONG).show();
    }

    @Override
    public String getLabel() {
        return "simulate NFC badge scan";
    }
}

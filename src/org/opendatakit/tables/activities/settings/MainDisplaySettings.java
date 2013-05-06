/*
 * Copyright (C) 2012 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.tables.activities.settings;

import org.opendatakit.tables.R;
import org.opendatakit.tables.data.Preferences;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

/**
 * An activity for setting display options.
 * 
 * @author hkworden
 */
public class MainDisplaySettings extends Activity {
    
    public static final String TABLE_ID_INTENT_KEY = "tableId";
    
    private static final String[] spinnerTexts = { "Table", "List", "Line Graph" };
    
    private Preferences prefs;
    private String tableId;
    private Spinner viewTypeSpinner;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Preferences(this);
        tableId = getIntent().getStringExtra(TABLE_ID_INTENT_KEY);
        setContentView(R.layout.settings_maindisplay);
        viewTypeSpinner = (Spinner) findViewById(
                R.id.settings_maindisplay_viewtype);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, spinnerTexts);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        viewTypeSpinner.setAdapter(adapter);
        viewTypeSpinner.setSelection(prefs.getPreferredViewType(tableId));
        Button saveButton = (Button) findViewById(
                R.id.settings_maindisplay_save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
                finish();
            }
        });
        Button cancelButton = (Button) findViewById(
                R.id.settings_maindisplay_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    
    private void saveSettings() {
        prefs.setPreferredViewType(tableId,
                viewTypeSpinner.getSelectedItemPosition());
    }
}
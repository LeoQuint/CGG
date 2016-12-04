/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.leoquintgames.capitalguess;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;

import com.google.example.games.basegameutils.BaseGameUtils;
import java.lang.Math;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;


import android.os.Bundle;
import android.os.Debug;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.*;
import android.view.KeyEvent;
import android.widget.TextView;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;


public class CapitalGuessActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    private static final String DEBUGTAG = "CapitalGuessDebug";
    private GoogleMap m_Map;
    private static LatLng m_MapCenter = new LatLng(43.6761, -79.4105);

    //region UI references
    private FrameLayout m_MainMenu;
    private FrameLayout m_GameOptionsMenu;
    private LinearLayout m_SettingsMenu;
    private RelativeLayout m_GameView;
    private LinearLayout m_EndGameOverlay;
    private LinearLayout m_GameClueOverlay;
    private LinearLayout m_GameBottomBarOverlay;

    //Clue text display refrences
    private TextView m_Clue_One_Display;
    private TextView m_Clue_Two_Display;
    private TextView m_Clue_Three_Display;
    private TextView m_Clue_Four_Display;
    //radio buttons
    private RadioButton m_RadioDiff_0;
    private RadioButton m_RadioDiff_1;
    private RadioButton m_RadioDiff_2;
    private RadioButton m_RadioDiff_3;

    //endregion

    //region Game Variables
    private int m_NumberOfRounds = 10;
    private int m_CurrentRound = 0;
    private int m_Zoom = 16;
    private int m_CurrentGuess = 0;
    private boolean m_HasClues = true;
    private boolean m_HasMapInfo = true;
    private int m_NumberOfCountries = 50;
    private ArrayList<Integer> m_PreviouslySelectedCountries = new ArrayList<Integer>();
    private ArrayList<JSONObject> m_Data = new ArrayList<JSONObject>();
    private int currentRandomIndex = 0;
    //endregion

    //region Google play services
    private static int RC_SIGN_IN = 9001;

    private boolean mResolvingConnectionFailure = false;
    private boolean mAutoStartSignInflow = true;
    private boolean mSignInClicked = false;
    private GoogleApiClient mGoogleApiClient;
    boolean mExplicitSignOut = false;
    boolean mInSignInFlow = false;
    private int mGoodGuessesInRow =0;

    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create the Google Api Client with access to the Play Games services
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                // add other APIs and scopes here as needed
                .build();


        setContentView(R.layout.capital_guess);

        //findViewById(R.id.sign_in_button).setOnClickListener((View.OnClickListener) this);
        //findViewById(R.id.sign_out_button).setOnClickListener((View.OnClickListener) this);

        MapFragment mapFragment =
                (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //region References to UI
        //Referencing menues
        m_MainMenu = (FrameLayout) findViewById(R.id.Main_Menu);//Layer->0
        m_GameOptionsMenu = (FrameLayout) findViewById(R.id.Game_Options_Menu);//Layer->1
        m_SettingsMenu = (LinearLayout) findViewById(R.id.Settings_Menu);//Layer->2
        m_GameView = (RelativeLayout) findViewById(R.id.Map_Screen);//Layer->3
        m_EndGameOverlay = (LinearLayout) findViewById(R.id.EndScreen);//Layer->4
        m_GameClueOverlay = (LinearLayout) findViewById(R.id.ClueLayout);
        m_GameBottomBarOverlay = (LinearLayout) findViewById(R.id.KeyboardLayout);
        //Setting the menues to default state on start
        changeMenu(0);
        //Referencing the Clue fields
        m_Clue_One_Display = (TextView) findViewById((R.id.clue1));
        m_Clue_Two_Display = (TextView) findViewById((R.id.clue2));
        m_Clue_Three_Display = (TextView) findViewById((R.id.clue3));
        m_Clue_Four_Display = (TextView) findViewById((R.id.clue4));
        //referencing the radio buttons
        m_RadioDiff_0 = (RadioButton) findViewById(R.id.diffselect0);
        m_RadioDiff_1 = (RadioButton) findViewById(R.id.diffselect1);
        m_RadioDiff_2 = (RadioButton) findViewById(R.id.diffselect2);
        m_RadioDiff_3 = (RadioButton) findViewById(R.id.diffselect3);
        //endregion


        final Button btn_Settings = (Button) findViewById(R.id.btn_Settings);
        btn_Settings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                changeMenu(2);
            }
        });
        final Button btn_Capitals = (Button) findViewById(R.id.btn_Capital_Mode);
        btn_Capitals.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                changeMenu(1);
            }
        });
        final Button btn_Country = (Button) findViewById(R.id.btn_Country_Mode);
        btn_Country.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                changeMenu(1);
            }
        });
        final Button btn_BackToMainMenuFromSettings = (Button) findViewById(R.id.btn_BackToMainMenuFromSettings);
        btn_BackToMainMenuFromSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                changeMenu(0);
            }
        });
        final Button btn_BackToMainMenuFromEndScreen = (Button) findViewById(R.id.btn_back_from_end_game_menu);
        btn_BackToMainMenuFromEndScreen.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                changeMenu(0);
            }
        });
        final Button btn_StartGame = (Button) findViewById(R.id.btn_Start);
        btn_StartGame.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                changeMenu(3);
            }
        });
        final Button btn_BackToMainMenuFromGameOptions = (Button) findViewById(R.id.btn_BackToMainMenuFromGameOptions);
        btn_BackToMainMenuFromGameOptions.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                changeMenu(0);
            }
        });
        final EditText inputPlayer = (EditText) findViewById(R.id.input_player);
        inputPlayer.setOnKeyListener(new EditText.OnKeyListener()
                                     {
                                         public boolean onKey(View v, int keyCode, KeyEvent event)
                                         {
                                             if(keyCode == 66 && event.getAction() == 1) //66 = enter/return, 1 = key release
                                             {
                                                 try {
                                                     submitGuess(inputPlayer.getText().toString());
                                                 } catch (JSONException e) {
                                                     e.printStackTrace();
                                                 }
                                                 return true;
                                             }
                                             return false;
                                         }
                                     }
        );
    }

    @Override
    public void onMapReady(GoogleMap map) {

        m_Map = map;

        MapStyleOptions mapStyles;
        m_Map.setIndoorEnabled(false);
        m_Map.setBuildingsEnabled(false);

        mapStyles = MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyles_cgg_none);

        m_Map.setMapStyle(mapStyles);
        m_Map.setContentDescription("The game");
        try {
            loadCountryList(0);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            setNewLocation();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //region Navigation
    private void changeMenu(int layer){
        switch(layer){
            case 0: //MainMenu Active
                m_MainMenu.setVisibility(View.VISIBLE);
                m_GameOptionsMenu.setVisibility(View.GONE);
                m_SettingsMenu.setVisibility(View.GONE);
                m_GameView.setVisibility(View.GONE);
                m_EndGameOverlay.setVisibility(View.GONE);
                m_GameClueOverlay.setVisibility(View.GONE);
                m_GameBottomBarOverlay.setVisibility(View.GONE);
                break;
            case 1: //Game Options Active
                m_MainMenu.setVisibility(View.GONE);
                m_GameOptionsMenu.setVisibility(View.VISIBLE);
                m_SettingsMenu.setVisibility(View.GONE);
                m_GameView.setVisibility(View.GONE);
                m_EndGameOverlay.setVisibility(View.GONE);
                m_GameClueOverlay.setVisibility(View.GONE);
                m_GameBottomBarOverlay.setVisibility(View.GONE);
                break;
            case 2: //Settings Active
                m_MainMenu.setVisibility(View.GONE);
                m_GameOptionsMenu.setVisibility(View.GONE);
                m_SettingsMenu.setVisibility(View.VISIBLE);
                m_GameView.setVisibility(View.GONE);
                m_EndGameOverlay.setVisibility(View.GONE);
                m_GameClueOverlay.setVisibility(View.GONE);
                m_GameBottomBarOverlay.setVisibility(View.GONE);
                break;
            case 3: //Gameplay Active
                m_MainMenu.setVisibility(View.GONE);
                m_GameOptionsMenu.setVisibility(View.GONE);
                m_SettingsMenu.setVisibility(View.GONE);
                m_GameView.setVisibility(View.VISIBLE);
                m_EndGameOverlay.setVisibility(View.GONE);
                m_GameClueOverlay.setVisibility(View.VISIBLE);
                m_GameBottomBarOverlay.setVisibility(View.VISIBLE);
                break;
            case 4: //End Game Active
                m_EndGameOverlay.setVisibility(View.VISIBLE);
                m_GameClueOverlay.setVisibility(View.GONE);
                m_GameBottomBarOverlay.setVisibility(View.GONE);
        }
    }
    //endregion

    //region Core Gameplay Functions
    public String loadJSONFromAsset(String filename) throws IOException {
        Log.v(DEBUGTAG, "Loading JSON");
        InputStream is = getResources().openRawResource(R.raw.country_list_50);
        Writer writer = new StringWriter();
        char[] buffer = new char[1024];
        try {
            Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        } finally {
            is.close();
        }

        String jsonString = writer.toString();
        return jsonString;
    }

    private void setNewLocation() throws JSONException {
        m_Zoom = 16;
        m_CurrentGuess = 0;
        m_CurrentRound++;
        Log.v(DEBUGTAG, "Round #" + m_CurrentRound + " of " + m_NumberOfRounds);
        if(m_CurrentRound > m_NumberOfRounds){
            endGame();
            return;
        }
        boolean previouslyDone = false;
        do{

            currentRandomIndex = (int)Math.floor( Math.random() * (double)m_NumberOfCountries);
            previouslyDone = false;
            for (int i = 0; i < m_PreviouslySelectedCountries.size(); ++i)
            {
                if (m_PreviouslySelectedCountries.get(i) == currentRandomIndex)
                {
                    previouslyDone = true;
                    break;
                }

            }
        }
        while (previouslyDone);

        m_PreviouslySelectedCountries.add(currentRandomIndex);

        double lat, lng;
        Log.v(DEBUGTAG, "Max number of countries " + m_NumberOfCountries);
        Log.v(DEBUGTAG, "Max number of countries from json" + m_Data.size());
        lat = (double)m_Data.get(currentRandomIndex).getJSONArray("latlng").get(0);
        lng = (double)m_Data.get(currentRandomIndex).getJSONArray("latlng").get(1);
        m_MapCenter =  new LatLng(lat, lng);

        m_Map.moveCamera(CameraUpdateFactory.newLatLngZoom(m_MapCenter, m_Zoom));
        try {
            displayClues();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void zoomOutCamera(int zoomLevel)
    {
        switch(zoomLevel)
        {
            case 0:
                m_Zoom = 16;
                break;
            case 1:
                m_Zoom = 12;
                break;
            case 2:
                m_Zoom = 8;
                break;
            case 3:
                m_Zoom = 4;
                break;
        }
        m_Map.moveCamera(CameraUpdateFactory.newLatLngZoom(m_MapCenter, m_Zoom));
    }
    //Submitting a new guess
    public void submitGuess(String input) throws JSONException {
        unlockAchievement(getString(R.string.achievement_thanks_for_trying_it_out));
        EditText editField = (EditText) findViewById(R.id.input_player);
        View view = this.getCurrentFocus();
        if(input.equalsIgnoreCase(""))
        {
            if (view != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            return;
        }
        String commonName = m_Data.get(currentRandomIndex).getJSONObject("name").getString("common");
        String officialName = m_Data.get(currentRandomIndex).getJSONObject("name").getString("official");
        Log.v(DEBUGTAG, input.toUpperCase());
        Log.v(DEBUGTAG, commonName.toUpperCase());

        if(input.equalsIgnoreCase("sprouts") && commonName.equalsIgnoreCase("belgium")){
            unlockAchievement(getString(R.string.achievement_eat_them_all_or_no_dessert));
        }
        if(input.equalsIgnoreCase("hilton") && commonName.equalsIgnoreCase("france")){
            unlockAchievement(getString(R.string.achievement_really));
        }
        if(input.equalsIgnoreCase(commonName) || input.equalsIgnoreCase(officialName))
        {
            goodGuess();
            editField.setText("");
            if (view != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            return;
        }
        badGuess();
        editField.setText("");
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void goodGuess() throws JSONException {
        mGoodGuessesInRow++;
        if(mGoodGuessesInRow >= 5){
            unlockAchievement(getString(R.string.achievement_5_in_a_row));
        }

        setNewLocation();

        try { displayClues(); } catch (JSONException e) { e.printStackTrace(); }
        return;
    }

    public void badGuess() throws JSONException {
        mGoodGuessesInRow = 0;
        m_CurrentGuess++;
        if(m_CurrentGuess >= 4){
            Log.v(DEBUGTAG, "Current guess " + m_CurrentGuess);
            setNewLocation();
        }
        try { displayClues(); } catch (JSONException e) { e.printStackTrace(); }
        zoomOutCamera(m_CurrentGuess);
        return;
    }
    public void endGame(){
        changeMenu(4);
        Log.v(DEBUGTAG, "End of game!");
        incrementAchievement(getString(R.string.achievement_just_one_more_game____), 1);
    }

    public void displayClues() throws JSONException {
        if(!m_HasClues){
            m_Clue_One_Display.setVisibility(View.GONE);
            m_Clue_Two_Display.setVisibility(View.GONE);
            m_Clue_Three_Display.setVisibility(View.GONE);
            m_Clue_Four_Display.setVisibility(View.GONE);
            return;
        }
        switch(m_CurrentGuess){
            case 0:
                //set visibility
                m_Clue_One_Display.setVisibility(View.VISIBLE);
                m_Clue_Two_Display.setVisibility(View.GONE);
                m_Clue_Three_Display.setVisibility(View.GONE);
                m_Clue_Four_Display.setVisibility(View.GONE);
                //set string
                String newClue1 = "Capital: " +  m_Data.get(currentRandomIndex).getString("capital");
                m_Clue_One_Display.setText(newClue1);
                break;
            case 1:
                //set visibility
                m_Clue_One_Display.setVisibility(View.VISIBLE);
                m_Clue_Two_Display.setVisibility(View.VISIBLE);
                m_Clue_Three_Display.setVisibility(View.GONE);
                m_Clue_Four_Display.setVisibility(View.GONE);
                //set string
                String newClue2 = "Region: " +  m_Data.get(currentRandomIndex).getString("region");
                m_Clue_Two_Display.setText(newClue2);
                break;
            case 2:
                //set visibility
                m_Clue_One_Display.setVisibility(View.VISIBLE);
                m_Clue_Two_Display.setVisibility(View.VISIBLE);
                m_Clue_Three_Display.setVisibility(View.VISIBLE);
                m_Clue_Four_Display.setVisibility(View.GONE);
                //set string
                String newClue3 = "Official Languages: ";
                for(int i = 0; i < m_Data.get(currentRandomIndex).getJSONArray("languages").length(); i++){
                    newClue3 += m_Data.get(currentRandomIndex).getJSONArray("languages").get(i);
                    if(i < m_Data.get(currentRandomIndex).getJSONArray("languages").length() -1){
                        newClue3 += ", ";
                    }
                }
                m_Clue_Three_Display.setText(newClue3);
                break;
            case 3:
                //set visibility
                m_Clue_One_Display.setVisibility(View.VISIBLE);
                m_Clue_Two_Display.setVisibility(View.VISIBLE);
                m_Clue_Three_Display.setVisibility(View.VISIBLE);
                m_Clue_Four_Display.setVisibility(View.VISIBLE);
                //set string
                String newClue4 = "Inhabitants: "+ m_Data.get(currentRandomIndex).getString("demonym");
                m_Clue_Four_Display.setText(newClue4);
                break;
        }
    }

    //Loads the json file based on selected difficulty/number of countries.
    public void loadCountryList(int difficulty) throws JSONException, IOException {
        String loadedFilename;
        switch(difficulty){
            case 0:
                loadedFilename = "country_list_50.json";
                m_NumberOfCountries = 50;
                break;
            case 1:
                loadedFilename = "country_list_100.json";
                m_NumberOfCountries = 100;
                break;
            case 2:
                loadedFilename = "country_list_185.json";
                m_NumberOfCountries = 185;
                break;
            case 3:
                loadedFilename = "country_list_238.json";
                m_NumberOfCountries = 238;
                break;
            default:
                loadedFilename = "country_list_50.json";
                m_NumberOfCountries = 50;
                break;
        }
        Log.v(DEBUGTAG, "Loading " + loadedFilename);
        JSONArray jArray = new JSONArray();

        String s = loadJSONFromAsset(loadedFilename);
        jArray = new JSONArray(s);

        for(int i = 0; i < jArray.length(); ++i){
            m_Data.add(new JSONObject(String.valueOf(jArray.get(i))));
        }

    }
    //endregion

    //region Getters and setters
    public void setNumberOfRounds(int num){
        m_NumberOfRounds = num;
    }
    public int getNumberOfRounds(){
        return m_NumberOfRounds;
    }
    public void setClues(boolean val){
        m_HasClues = val;
    }
    public boolean getClues(){
        return m_HasClues;
    }
    public void setMapInfo(boolean val){
        m_HasMapInfo = val;
    }
    public boolean getMapInfo(){
        return m_HasMapInfo;
    }
    //endregion

    public void onRadioButtonClicked(View view) throws IOException, JSONException {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.diffselect0:
                if (checked){
                    m_RadioDiff_1.setBackground(getDrawable(R.drawable.star_empty));
                    m_RadioDiff_2.setBackground(getDrawable(R.drawable.star_empty));
                    m_RadioDiff_3.setBackground(getDrawable(R.drawable.star_empty));
                    loadCountryList(0);
                }
                    break;
            case R.id.diffselect1:
                if (checked){
                    m_RadioDiff_1.setBackground(getDrawable(R.drawable.start_full));
                    m_RadioDiff_2.setBackground(getDrawable(R.drawable.star_empty));
                    m_RadioDiff_3.setBackground(getDrawable(R.drawable.star_empty));
                    loadCountryList(1);
                }
                    break;
            case R.id.diffselect2:
                if (checked){
                    m_RadioDiff_1.setBackground(getDrawable(R.drawable.start_full));
                    m_RadioDiff_2.setBackground(getDrawable(R.drawable.start_full));
                    m_RadioDiff_3.setBackground(getDrawable(R.drawable.star_empty));
                    loadCountryList(2);
                }
                    break;
            case R.id.diffselect3:
                if (checked){
                    m_RadioDiff_1.setBackground(getDrawable(R.drawable.start_full));
                    m_RadioDiff_2.setBackground(getDrawable(R.drawable.start_full));
                    m_RadioDiff_3.setBackground(getDrawable(R.drawable.start_full));
                    loadCountryList(3);
                }
                    break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mInSignInFlow && !mExplicitSignOut) {
            // auto sign in
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.sign_in_button) {
            // start the asynchronous sign in flow
            mSignInClicked = true;
            mGoogleApiClient.connect();
        }
        else if (view.getId() == R.id.sign_out_button) {
            // user explicitly signed out, so turn off auto sign in
            mExplicitSignOut = true;
            if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                Games.signOut(mGoogleApiClient);
                mGoogleApiClient.disconnect();
                // sign out.
                mSignInClicked = false;

                // show sign-in button, hide the sign-out button
                findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
                findViewById(R.id.sign_out_button).setVisibility(View.GONE);
            }

        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        // show sign-out button, hide the sign-in button
        findViewById(R.id.sign_in_button).setVisibility(View.GONE);
        findViewById(R.id.sign_out_button).setVisibility(View.VISIBLE);

        // (your code here: update UI, enable functionality that depends on sign in, etc)
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (mResolvingConnectionFailure) {
            // Already resolving
            return;
        }

        // If the sign in button was clicked or if auto sign-in is enabled,
        // launch the sign-in flow
        if (mSignInClicked || mAutoStartSignInflow) {
            mAutoStartSignInflow = false;
            mSignInClicked = false;
            mResolvingConnectionFailure = true;

            // Attempt to resolve the connection failure using BaseGameUtils.
            // The R.string.signin_other_error value should reference a generic
            // error string in your strings.xml file, such as "There was
            // an issue with sign in, please try again later."
            if (!BaseGameUtils.resolveConnectionFailure(this,
                    mGoogleApiClient, connectionResult,
                    RC_SIGN_IN, getString(R.string.signin_other_error))) {
                mResolvingConnectionFailure = false;
            }
        }

        findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
        findViewById(R.id.sign_out_button).setVisibility(View.GONE);
    }

    public void unlockAchievement(String achievement){
        Log.v(DEBUGTAG, achievement);
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            // Call a Play Games services API method, for example:
            Games.Achievements.unlock(mGoogleApiClient, achievement);
        } else {
            Log.v(DEBUGTAG, "User not signed in!");
        }
    }
    public void incrementAchievement(String achievement, int num){
        Log.v(DEBUGTAG, achievement);
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            // Call a Play Games services API method, for example:
            Games.Achievements.increment(mGoogleApiClient, achievement, num);
        } else {
            Log.v(DEBUGTAG, "User not signed in!");
        }
    }
}

package com.ankurmittal.translateposts;


import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.ankurmittal.translateposts.data.Constants;
import com.ankurmittal.translateposts.data.Post;
import com.ankurmittal.translateposts.data.PostsDB;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import rx.functions.Action1;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {

    private static ProgressDialog dialog;
    private static PostsDB db;
    private static ArrayList<Post> posts;
    private static SharedPreferences.Editor editor;
    private static View view;

    private static PublishSubject<String> notifier;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
        view = getListView();
        notifier = PublishSubject.create();
        Action1<String> observer = new Action1<String>() {
            @Override
            public void call(String s) {
                finish();
                startActivity(getIntent());
            }
        };
        notifier.subscribe(observer);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setTitle(PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.KEY_SETTINGS_TITLE,"Settings"));

        editor = PreferenceManager
                .getDefaultSharedPreferences(this).edit();

        editor.putString(Constants.KEY_OLD_LANG, PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString("lang_list", "en"));
        editor.commit();
        db = new PostsDB(SettingsActivity.this);
        db.open();

        posts = db.getList();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(db.isOpen()) {
            db.close();
        }
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(final Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);
                String oldLang = PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(Constants.KEY_OLD_LANG,"en");
                if(!stringValue.equals(oldLang)) {
                    String translating = PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(Constants.KEY_TRANSLATING_TITLE,"Translating");
                    //Show dialog indicating translating..
                    dialog = ProgressDialog.show(
                            preference.getContext(), "", translating+"...", true);
                    //Translate from olgLang to stringValue
                    translateText(oldLang,stringValue,preference,listPreference.getEntries()[index]);

                } else {
                    if(index>=0){
                        preference.setSummary(listPreference.getEntries()[index]);
                    }
                }

            }  else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    private static void translateText(final String oldLang, final String stringValue, final Preference preference, final CharSequence index) {

        //Get Access Token
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        Retrofit token = new Retrofit.Builder()
                .baseUrl(Constants.TokenAccessUri)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        Retrofit translate = new Retrofit.Builder()
                .baseUrl(Constants.TRANSLATE_SERVICE_URL)
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .build();


        final TokenService tokenService = token.create(TokenService.class);
        final TranslationService translateService = translate.create(TranslationService.class);
        Call<String> acessToken = tokenService.getToken("");

        acessToken.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                String token = response.body();


                //Translate static data
                translateStaticData(token, translateService, stringValue,preference,index);
                //Translate Dynamic data
                if(posts.size() > 0) {
                    translateDynamicData(token, translateService, stringValue,preference,index);
                }

            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {

                Snackbar snackbar = Snackbar
                        .make(view, "Network Error", Snackbar.LENGTH_LONG);

                snackbar.show();
                dialog.hide();
            }
        });
    }

    private static void translateStaticData(String token, TranslationService translateService, String stringValue, final Preference preference, final CharSequence index) {
        final int[] x = {0};
        final String[] staticStrings = {"What's on your mind?","Create Post","Translating","Sign out","Settings","Choose Language"};
        for(final String string:staticStrings){
            Call<String> dynamicData = translateService.translateTextArray("Bearer "+token,"en",string,stringValue);
            dynamicData.enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    x[0]++;
                    String key = getKey(string);
                    editor.putString(key, response.body());
                    editor.commit();

                    if(x[0] == staticStrings.length && posts.size() == 0) {
                        preference.setSummary(index);
                        dialog.hide();
                        dialog.dismiss();
                        notifier.onNext("Refresh");

                    }

                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    x[0]++;
                    if(posts.size() == 0){
                        dialog.hide();
                        dialog.dismiss();
                    }
                    Snackbar snackbar = Snackbar
                            .make(view, "Error while translating", Snackbar.LENGTH_LONG);

                    snackbar.show();
                }
            });
        }
    }

    private static void translateDynamicData(String token, TranslationService translateService, final String stringValue, final Preference preference, final CharSequence index) {
        final int[] x = {0};
        for(final Post post:posts){
            post.asObservable().subscribe(db);
            Call<String> dynamicData = translateService.translateTextArray("Bearer "+token,"en",post.getMessage(),stringValue);
            dynamicData.enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    x[0]++;
                    post.setTranslatedMessage(response.body());

                    if(x[0] == posts.size()) {
                        dialog.hide();
                        dialog.dismiss();
                        //Set current language to summary
                        editor.putString(Constants.KEY_OLD_LANG, stringValue);
                        editor.commit();
                        // Also set Summary
                        preference.setSummary(index);
                        notifier.onNext("Refresh");
                    }


                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    x[0]++;
                    Snackbar snackbar = Snackbar
                            .make(view, "Error while translating", Snackbar.LENGTH_LONG);

                    snackbar.show();
                    if(x[0] == posts.size()) {
                        dialog.hide();

                    }
                }
            });
        }
    }

    private static String getKey(String string) {

            if(string.equals("What's on your mind?")){
                return Constants.KEY_EDIT_TEXT_HINT;
            } else if(string.equals("Create Post")){
                return Constants.KEY_CREATE_POST_TITLE;
            } else if(string.equals("Translating")){
                return Constants.KEY_TRANSLATING_TITLE;
            } else if(string.equals("Sign out")){
                return Constants.KEY_SIGN_OUT_TITLE;
            } else if(string.equals("Settings")){
                return Constants.KEY_SETTINGS_TITLE;
            } else if(string.equals("Choose Language")){
                return Constants.KEY_CHOOSE_LANGUAGE_TITLE;
            }
        return null;

    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }



    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);
            Preference customPref = (Preference) findPreference("lang_list");
            customPref.setTitle(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(Constants.KEY_CHOOSE_LANGUAGE_TITLE,"Choose Language"));

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("lang_list"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                Intent intent = new Intent(getActivity(), WallActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }





}

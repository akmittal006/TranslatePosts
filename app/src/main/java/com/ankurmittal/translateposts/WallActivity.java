package com.ankurmittal.translateposts;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.ankurmittal.translateposts.data.Constants;
import com.ankurmittal.translateposts.data.Post;
import com.ankurmittal.translateposts.data.PostsDB;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import rx.functions.Action1;

public class WallActivity extends AppCompatActivity implements View.OnClickListener, GoogleApiClient.OnConnectionFailedListener{



    private static final int SIGNIN_RC = 42 ;
    private EditText messageEditText;
    private Button postButton;
    private PostsDB database;
    private RelativeLayout signInView;
    private RecyclerView mRecyclerView;
    private PostsAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private String email;

    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wall);

       GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this,this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();


        findViewById(R.id.sign_in_button).setOnClickListener(this);
        signInView = (RelativeLayout) findViewById(R.id.signInLayout);

        database = new PostsDB(this);
        messageEditText = (EditText) findViewById(R.id.messageEditText);
        postButton = (Button) findViewById(R.id.postButton);
        messageEditText.setHint(PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.KEY_EDIT_TEXT_HINT,"What's on your mind?"));
        postButton.setText(PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.KEY_CREATE_POST_TITLE,"Create Post"));

        postButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!messageEditText.getText().toString().trim().equals("")) {

                    postNewMessage(messageEditText.getText());
                    messageEditText.setText("");
                    messageEditText.setHint(PreferenceManager.getDefaultSharedPreferences(WallActivity.this).getString(Constants.KEY_EDIT_TEXT_HINT,"What's on your mind?"));
                }

            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.postsListView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(false);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);


    }

    private void postNewMessage(Editable text) {
        Post newPost = new Post();
        newPost.setMessage(text.toString().trim());
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "HH:mm yyyy-MM-dd", Locale.getDefault());
        Date date = new Date();
        newPost.setDateTime(dateFormat.format(date));

        //save in database
        database.insert(newPost);
        //update ui
        mAdapter = new PostsAdapter(this,database);
        mRecyclerView.setAdapter(mAdapter);

    }

    @Override
    protected void onStop() {
        super.onStop();
        database.close();
    }

    @Override
    protected void onPause() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        database.open();
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone())
        {
            //Cached login
            GoogleSignInResult result = opr.get();

            handleSignInResult(result);

        } else {
            final ProgressDialog dialog = ProgressDialog.show(
                    this, "", "Signing in...", true);
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(GoogleSignInResult googleSignInResult) {
                    dialog.hide();
                    dialog.dismiss();
                    handleSignInResult(googleSignInResult);
                }
            });
        }
        // specify an adapter
        mAdapter = new PostsAdapter(this,database);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI...
                Intent intent = new Intent( this, SettingsActivity.class );
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.putExtra( PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.GeneralPreferenceFragment.class.getName() );
                intent.putExtra( PreferenceActivity.EXTRA_NO_HEADERS, true );
                startActivity(intent);
                return true;

            case R.id.action_signout:

                Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(Status status) {
                                // ...
                                database.deleteAll();
                                setDefaultBackToEnglish();

                                updateUI(false);
                            }
                        });


                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private void setDefaultBackToEnglish() {
        PreferenceManager.getDefaultSharedPreferences(this).edit().clear().apply();
        finish();
        startActivity(getIntent());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options, menu);

        MenuItem item = menu.findItem(R.id.action_signout);
        item.setTitle(PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.KEY_SIGN_OUT_TITLE,"SIGN OUT"));

        MenuItem itemSettings = menu.findItem(R.id.action_settings);
        itemSettings.setTitle(PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.KEY_SETTINGS_TITLE,"Settings"));

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onClick(View view) {
        Intent intent  = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(intent,SIGNIN_RC);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SIGNIN_RC) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            email = acct.getEmail();
            updateUI(true);
        } else {
            // Signed out, show unauthenticated UI.
            updateUI(false);
        }
    }

    private void updateUI(boolean b) {
        if(b) {
            getSupportActionBar().show();
            signInView.setVisibility(View.GONE);
            messageEditText.setVisibility(View.VISIBLE);
            getSupportActionBar().setSubtitle(email);

        } else {
            messageEditText.clearFocus();
            messageEditText.setVisibility(View.GONE);
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                messageEditText.clearFocus();
            }
            getSupportActionBar().hide();
            signInView.setVisibility(View.VISIBLE);

        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Snackbar snackbar = Snackbar
                .make(signInView, "Network Error", Snackbar.LENGTH_LONG);

        snackbar.show();
    }

}

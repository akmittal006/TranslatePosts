package com.ankurmittal.translateposts;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ankurmittal.translateposts.data.Constants;
import com.ankurmittal.translateposts.data.Post;
import com.ankurmittal.translateposts.data.PostsDB;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

/**
 * Created by AnkurMittal2 on 13-12-2016.
 */

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.ViewHolder> {
    private final Context mContext;
    private final PostsDB db;
    private ArrayList<Post> mDataset;

    public void refresh() {
        notifyDataSetChanged();
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView mTextView;
        public TextView mTranslatingLabel;
        public TextView mTimeView;

        public ViewHolder(View v) {
            super(v);
            mTextView = (TextView) v.findViewById(R.id.textView);
            mTranslatingLabel = (TextView) v.findViewById(R.id.translatingLabel);
            mTimeView = (TextView) v.findViewById(R.id.dateTextView);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public PostsAdapter(Context context, PostsDB postsDB) {

        mContext = context;
        db = postsDB;
        mDataset = db.getList();
    }

    // Create new views (invoked by the layout manager)
    @Override
    public PostsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);

        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v);

        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        if(PreferenceManager.getDefaultSharedPreferences(mContext).getString(Constants.KEY_OLD_LANG,"en").equals("en")){
            //Already in english
            holder.mTextView.setText(mDataset.get(position).getMessage());
        } else {
            //translation reqd
            if(mDataset.get(position).getTranslatedMessage().equals("")) {
                holder.mTextView.setText(mDataset.get(position).getMessage());
                String translating = PreferenceManager.getDefaultSharedPreferences(mContext).getString(Constants.KEY_TRANSLATING_TITLE,"Translating");
                holder.mTranslatingLabel.setText(translating+"...");
                holder.mTranslatingLabel.setVisibility(View.VISIBLE);
                translateText(holder.mTranslatingLabel,mDataset.get(position));
            } else {
                holder.mTextView.setText(mDataset.get(position).getTranslatedMessage());
            }
        }


        holder.mTimeView.setText(mDataset.get(position).getDateTime());


    }

    private void translateText(final TextView mTranslatingLabel, final Post postToTranslate) {
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
                //Translate Dynamic data

                final Post post = postToTranslate;
                post.asObservable().subscribe(db);
                Call<String> dynamicData = translateService
                        .translateTextArray("Bearer "+token,"en",post.getMessage(), PreferenceManager.getDefaultSharedPreferences(mContext).getString(Constants.KEY_OLD_LANG,"en"));
                dynamicData.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {

                        post.setTranslatedMessage(response.body());
                        mTranslatingLabel.setVisibility(View.INVISIBLE);
                        mDataset = db.getList();
                        notifyDataSetChanged();
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        mTranslatingLabel.setText("Error while translating");
                        notifyDataSetChanged();
                    }
                });
            }




            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Snackbar snackbar = Snackbar
                        .make(mTranslatingLabel, "Network Error", Snackbar.LENGTH_LONG);

                snackbar.show();
            }
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
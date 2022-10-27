package com.example.android.newsappusingguardianapi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.LoaderManager;
import android.content.Loader;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import java.util.List;


public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<Article>>, ListView.OnItemClickListener {

    public static final int ARTICLE_LOADER_ID = 1;
    public static final String REQUEST_URL =

            "https://content.guardianapis.com/search";

    ListView mListView;

    ArticleAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.news_list);

        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();

        mListView = (ListView) findViewById(R.id.list);
        mListView.setOnItemClickListener(this);
        TextView mTextView = (TextView) findViewById(R.id.nointernet_text_view);


        if (networkInfo != null && networkInfo.isConnected()) {
            LoaderManager loaderManager = getLoaderManager();
            loaderManager.initLoader(ARTICLE_LOADER_ID, null, this);
        }
        else {
            mTextView.setVisibility(View.VISIBLE);
            mTextView.setText(R.string.no_internet_text);
        }
    }

    @Override
    public Loader<List<Article>> onCreateLoader(int id, Bundle args) {
        Uri baseUri = Uri.parse(REQUEST_URL);
        Uri.Builder uriBuilder = baseUri.buildUpon();

        uriBuilder.appendQueryParameter("q", "halloween");
        uriBuilder.appendQueryParameter("api-key", "test");
        uriBuilder.appendQueryParameter("show-tags", "contributor");
        return new ArticleLoader(this, uriBuilder.toString());
    }

    @Override
    public void onLoadFinished(Loader<List<Article>> loader, List<Article> data) {
        if (data.size() == 0) {
            TextView mTextView = (TextView) findViewById(R.id.nonews_text_view);
            mTextView.setVisibility(View.VISIBLE);
            mTextView.setText(R.string.no_news_text);
        }
        else{
            mAdapter = new ArticleAdapter(this, data);
            mListView.setAdapter(mAdapter);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<Article>> loader) {
    }



    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

        Article currentArticle = (Article) adapterView.getItemAtPosition(position);

        Uri url = Uri.parse(currentArticle.getUrl());

        Intent i = new Intent(Intent.ACTION_VIEW);
        if (i.resolveActivity(getPackageManager()) != null) {
            i.setData(url);
            startActivity(i);
        }
    }
}

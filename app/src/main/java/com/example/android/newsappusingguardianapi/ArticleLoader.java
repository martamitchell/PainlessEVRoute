package com.example.android.newsappusingguardianapi;

import android.content.Context;
import android.content.AsyncTaskLoader;
import java.util.List;


public class ArticleLoader extends AsyncTaskLoader<List<Article>> {
    private String mUrl;

    public ArticleLoader(Context context, String url) {
        super(context);
        mUrl = url;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    public List<Article> loadInBackground() {
        if(mUrl == null) {
            return null;
        }
        return QueryUtils.fetchNewsData(mUrl);
    }
}


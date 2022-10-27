package com.example.android.newsappusingguardianapi;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;


public class ArticleAdapter extends ArrayAdapter<Article> {

    List<Article> mNewsList;

    public ArticleAdapter(Context context, List<Article> newsList) {
        super(context, 0, newsList);
        mNewsList = newsList;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        Article halloweenNews = mNewsList.get(position);

        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
        }

        TextView textviewTitle = (TextView) view.findViewById(R.id.headline_text_view);
        textviewTitle.setText(halloweenNews.getTitle());

        TextView textviewAuthor = (TextView) view.findViewById(R.id.author_text_view);
        textviewAuthor.setText(halloweenNews.getAuthor());

        TextView textviewSection = (TextView) view.findViewById(R.id.section_text_view);
        textviewSection.setText(halloweenNews.getSection());

        TextView textviewDate = (TextView) view.findViewById(R.id.date_text_view);
        textviewDate.setText(halloweenNews.getDate());

        return view;
    }
}
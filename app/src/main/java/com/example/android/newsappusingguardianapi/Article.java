package com.example.android.newsappusingguardianapi;


public class Article {
    private String mTitle;
    private String mDate;
    private String mUrl;
    private String mSection;
    private String mAuthor;

    public Article(String title, String author, String date, String url, String section) {
        mTitle = title;
        mDate = date;
        mUrl = url;
        mSection = section;
        mAuthor = author;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getDate() {
        return mDate;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getSection() {
        return mSection;
    }

    public String getAuthor() {
        return mAuthor;
    }

}
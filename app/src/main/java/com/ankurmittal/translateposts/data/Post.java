package com.ankurmittal.translateposts.data;

import rx.Observable;
import rx.subjects.PublishSubject;

/**
 * Created by AnkurMittal2 on 13-12-2016.
 */

public class Post {

    private String dateTime;
    private String message;
    private String translatedMessage;
    private int id;

    private PublishSubject<Post> notifier;

    public Post() {
        translatedMessage = "";
        notifier = PublishSubject.create();
    }

    public String getTranslatedMessage() {
        return translatedMessage;
    }

    public void setTranslatedMessage(String translatedMessage) {
        this.translatedMessage = translatedMessage;
        notifier.onNext(this);
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Observable<Post> asObservable() {
        return notifier;
    }


}

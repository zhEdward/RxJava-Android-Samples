package com.morihacky.android.rxjava.rxbus;

import android.app.Activity;
import android.os.Bundle;
import android.os.PersistableBundle;

import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;


public class DemoActivity extends Activity {

    public static final  String TAG ="DemoActivity";

    //defind an observable
    public static class DummyEvent {
        int var1;
        String var2;
    }


    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);

        //emit observable item to target subscriber
        RxBus.getDefault().send(new DummyEvent());



        //do a  rx to reciver this emit data

        //pattern1
        Subscription s1 = RxBus.getDefault().asObservable()
                .observeOn(AndroidSchedulers.mainThread())//spec for android
                .subscribe(new Observer<Object>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Object o) {
                        //do something
                    }
                });

        //pattern2
        Subscription s2 = RxBus.getDefault().asObservable()
                .observeOn(AndroidSchedulers.mainThread()).//spec for android
                ofType(DummyEvent.class).subscribe(new Action1<DummyEvent>() {
                    @Override
                    public void call(DummyEvent dummyEvent) {
                        //do something
                    }
                });

        //pattern-lamda equal pattern2（need compile with java）
        Subscription s3 = RxBus.getDefault().asObservable()
                //.observeOn(AndroidSchedulers.mainThread()) spec for android
                .subscribe(dummy -> {
                        }
                        //do something
                );


        //onDestory you should unSubscribe all of observables

        s1.unsubscribe();
        s2.unsubscribe();
        s3.unsubscribe();
        //or use wrapper subscription
        CompositeSubscription rxSubscription = new CompositeSubscription(s1, s2, s3);

        //rxSubscription.add(s1);
        //rxSubscription.add(s2);
        //rxSubscription.add(s3);

        //call in onDestory() or when not use it
        rxSubscription.clear();
    }


}

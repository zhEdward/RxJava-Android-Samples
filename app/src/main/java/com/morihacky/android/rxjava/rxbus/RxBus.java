package com.morihacky.android.rxjava.rxbus;

import com.jakewharton.rxrelay.PublishRelay;
import com.jakewharton.rxrelay.Relay;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;
import rx.subscriptions.CompositeSubscription;

/**
 * courtesy: https://gist.github.com/benjchristensen/04eef9ca0851f3a5d7bf
 * <p>
 * fork from https://github.com/kaushikgopal/RxJava-Android-Samples/blob/master/app/src/main/java/com/morihacky/android/rxjava/rxbus/RxBus.java
 */
public class RxBus {

    /**
     * 使用来自  com.jakewharton.rxrelay library jar 构建的线程安全的 observabler
     * <p>
     * add  compile 'com.jakewharton.rxrelay:rxrelay:1.2.0' in you prokect-level gradle
     */

    private final Relay<Object, Object> _bus = PublishRelay.create().toSerialized();

    /**
     * 使用 rxjava中的jar 构造 线程安全的 observabler,PublishSubject 有一个risk:
     * 在Subject被创建后到有观察者订阅它之前这个时间段内，一个
     * 或多个数据可能会丢失。如果要确保来自原始Observable的所有数据都被分发，你需要这样
     * 做：或者使用Create创建那个Observable以便手动给它引入"冷"Observable的行为（ 当所有观
     * 察者都已经订阅时才开始发射数据） ，或者改用ReplaySubject
     */
    @Deprecated
    private final Subject<Object, Object> _busJava = new SerializedSubject<> (PublishSubject.create ());


    private static volatile RxBus defaultInstance;


    public static RxBus getDefault() {
        if (defaultInstance == null) {
            synchronized (RxBus.class) {
                if (defaultInstance == null) {
                    defaultInstance = new RxBus ();
                }
            }
        }
        return defaultInstance;
    }


    public void send(Object o) {
        _bus.call(o);
    }

    /**
     * 做为可观察对象
     *
     * @return
     */
    public Observable<Object> asObservable() {
        return _bus.asObservable ();
    }


    public boolean hasObservers() {
        return _bus.hasObservers();

    }


    //--------------------------------------使用 rxjava jar 中实现 rxbus

    @Deprecated
    public void sendRx(Object o) {
        _busJava.onNext (o);
    }


    @Deprecated
    public Observable<Object> asObservableRx() {
        return _busJava.asObservable ();
    }

    @Deprecated
    public boolean hasObserversRx() {
        return _busJava.hasObservers ();
    }


    //defind an observable
    public static class DummyEvent {
    }

    ;


//    {
//
//
//        //emit observable item to target subscriber
//        RxBus.getDefault ().send (new DummyEvent ());
//
//        //do a  rx to reciver this emit data
//
//        //pattern1
//        Subscription s1 = RxBus.getDefault ().asObservable ().observeOn (AndroidSchedulers.mainThread ())//spec for android
//                .subscribe (new Observer<Object> () {
//                    @Override
//                    public void onCompleted() {
//
//                    }
//
//                    @Override
//                    public void onError(Throwable e) {
//
//                    }
//
//                    @Override
//                    public void onNext(Object o) {
//                        //do something
//                    }
//                });
//
//        //pattern2
//        Subscription s2 = RxBus.getDefault ().asObservable ().observeOn (AndroidSchedulers.mainThread ()).//spec for android
//                ofType (DummyEvent.class).subscribe (new Action1<DummyEvent> () {
//            @Override
//            public void call(DummyEvent dummyEvent) {
//                //do something
//            }
//        });
//
//        //pattern-lamda equal pattern2
//        Subscription s3 = RxBus.getDefault ().asObservable ()
//                //.observeOn(AndroidSchedulers.mainThread()) spec for android
//                .subscribe (dummy -> {
//                        }
//                        //do something
//                );
//
//
//        //onDestory you should unSubscribe all of observables
//
//        s1.unsubscribe ();
//        s2.unsubscribe ();
//        s3.unsubscribe ();
//        //or use wrapper subscription
//        CompositeSubscription rxSubscription = new CompositeSubscription (s1, s2, s3);
//
//        //rxSubscription.add(s1);
//        //rxSubscription.add(s2);
//        //rxSubscription.add(s3);
//
//        //call in onDestory() or when not use it
//        rxSubscription.clear ();
//
//
//    }


}
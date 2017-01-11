package com.morihacky.android.rxjava.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.morihacky.android.rxjava.R;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

/**
 * 异步耗时操作
 */
public class ConcurrencyWithSchedulersDemoFragment
      extends BaseFragment {

    @Bind(R.id.progress_operation_running) ProgressBar _progress;
    @Bind(R.id.list_threading_log) ListView _logsList;

    private LogAdapter _adapter;
    private List<String> _logs;
    //一个类中存在多个 rx subscribe ，使用集合统一管理
    private CompositeSubscription _subscriptions = new CompositeSubscription ();

    @Override
    public void onDestroy() {
        super.onDestroy();
        ButterKnife.unbind(this);
        _subscriptions.clear ();//memory leak
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        _setupLogger();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_concurrency_schedulers, container, false);
        ButterKnife.bind(this, layout);
        return layout;
    }

    @OnClick(R.id.btn_start_operation)
    public void startLongOperation() {

        _progress.setVisibility(View.VISIBLE);
        _log("Button Clicked");

        //全部异步操作
        Subscription s = _getObservable ().subscribeOn (Schedulers.io ())//异步操作
              .observeOn(AndroidSchedulers.mainThread()).subscribe (_getObserver ()); // Observer

        _subscriptions.add (s);
    }

    private Observable<String> _getObservable() {
        //        return Observable.just(true).map(aBoolean -> {
        //            _log("Within Observable");
        //            _doSomeLongOperation_thatBlocksCurrentThread();
        //            return aBoolean;
        //        });


        //通过map(默认在main thread 上调度 耗时操作可能阻塞) 对原先 的 observable 序列进行封装再 emit
        return Observable.just (false, true).map (new Func1<Boolean, String> () {
            @Override
            public String call(Boolean aBoolean) {
                //  Log.i(TAG, "call: "+aBoolean);
                _doSomeLongOperation_thatBlocksCurrentThread ();
                return aBoolean ? "chinese" : "ABC";
            }
        });

    }

    /**
     * Observer that handles the result through the 3 important actions:
     *
     * 1. onCompleted
     * 2. onError
     * 3. onNext
     */
    private Observer<String> _getObserver() {
        return new Observer<String> () {

            @Override
            public void onCompleted() {
                _log("On complete");
                _progress.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e, "Error in RxJava Demo concurrency");
                _log(String.format("Boo! Error %s", e.getMessage()));
                _progress.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onNext(String bool) {
                Log.i (TAG, "onNext: " + bool);
                _log (String.format ("onNext with return \"%s\"", bool));
            }
        };
    }

    // -----------------------------------------------------------------------------------
    // Method that help wiring up the example (irrelevant to RxJava)
    private void _doSomeLongOperation_thatBlocksCurrentThread() {
        _log("performing long operation");

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Timber.d("Operation was interrupted");
        }
    }

    private void _log(String logMsg) {

        if (_isCurrentlyOnMainThread()) {
            _logs.add(0, logMsg + " (main thread) ");
            _adapter.clear();
            _adapter.addAll(_logs);
        } else {
            _logs.add(0, logMsg + " (NOT main thread) ");

            // You can only do below stuff on main thread
            new Handler(Looper.getMainLooper()).post(() -> {
                _adapter.clear();
                _adapter.addAll(_logs);
            });
        }
    }

    private void _setupLogger() {
        _logs = new ArrayList<>();
        _adapter = new LogAdapter(getActivity(), new ArrayList<>());
        _logsList.setAdapter(_adapter);
    }

    private boolean _isCurrentlyOnMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    private class LogAdapter
          extends ArrayAdapter<String> {

        public LogAdapter(Context context, List<String> logs) {
            super(context, R.layout.item_log, R.id.item_log, logs);
        }
    }
}
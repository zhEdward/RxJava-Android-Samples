package com.morihacky.android.rxjava.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;

import com.jakewharton.rxbinding.widget.RxCompoundButton;
import com.jakewharton.rxbinding.widget.RxRadioGroup;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.jakewharton.rxbinding.widget.TextViewTextChangeEvent;
import com.morihacky.android.rxjava.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static co.kaush.core.util.CoreNullnessUtils.isNullOrEmpty;
import static java.lang.String.format;

public class DebounceSearchEmitterFragment extends BaseFragment {

    @Bind(R.id.list_threading_log)
    ListView _logsList;
    @Bind(R.id.input_txt_debounce)
    EditText _inputSearchText;
    @Bind(R.id.checkBox)
    CheckBox _checkBox;

    private LogAdapter _adapter;
    private List<String> _logs;

    private Subscription _subscription;

    @Override
    public void onDestroy() {
        super.onDestroy();
        //_subscription.unsubscribe();
        compositeSubscription.clear ();
        ButterKnife.unbind(this);
    }

    CompositeSubscription compositeSubscription;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        compositeSubscription = new CompositeSubscription ();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_debounce, container, false);
        ButterKnife.bind(this, layout);


        return layout;
    }

    @OnClick(R.id.clr_debounce)
    public void onClearLog() {
        _logs = new ArrayList<>();
        _adapter.clear();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        _setupLogger();

        //        _subscription = RxTextView.textChangeEvents(_inputSearchText)
        //              .debounce(400, TimeUnit.MILLISECONDS)// default Scheduler is Computation
        //              .filter(changes -> isNotNullOrEmpty(_inputSearchText.getText().toString()))
        //              .observeOn(AndroidSchedulers.mainThread())
        //              .subscribe(_getSearchObserver());

        //as know for java6
        //changes -> isNotNullOrEmpty(_inputSearchText.getText().toString())

        //订阅 checkbox 的状态
        compositeSubscription.add (RxCompoundButton.checkedChangeEvents (_checkBox).observeOn (AndroidSchedulers.mainThread ()).map (compoundButtonCheckedChangeEvent -> {
            return compoundButtonCheckedChangeEvent.isChecked ();
        }).subscribe (new Action1<Boolean> () {
            @Override
            public void call(Boolean aBoolean) {
                _log ("_checkBox status:" + aBoolean);
            }
        }));


        RxRadioGroup.checkedChangeEvents (throttleGroup).observeOn (AndroidSchedulers.mainThread ()).map (event -> event.checkedId ()).subscribe (ids -> {

            if (_subscription != null) _subscription.unsubscribe ();
            //int to 16进制 string 显示
            Log.i (TAG, "radioButton change: " + String.format ("%02x", ids));
            if (ids == R.id.radioButton1) {
                //  debounce 和 throttleWithTimeout 操作符（每次发射 之间相隔400ms+ 就发射一次 最后缓存的item 内容 emit，其余丢弃）
                _subscription = RxTextView.textChangeEvents (_inputSearchText).debounce (400, TimeUnit.MILLISECONDS)// default Scheduler is Computation（默认当前线程），只取在每个400ms间隔内 最后发射出的item 进行emit
                        .filter (new Func1<TextViewTextChangeEvent, Boolean> () {//当前textChange 如果为空串 就不emit（不处罚emit）
                            @Override
                            public Boolean call(TextViewTextChangeEvent textViewTextChangeEvent) {
                                CharSequence cs = textViewTextChangeEvent.text ();
                                String changeStr = cs.toString ();
                                //                        byte[] b = changeStr.getBytes();
                                //                        changeStr = "";
                                //                        for (byte bt :
                                //                                b) {
                                //                            changeStr += "#" + String.format(Locale.getDefault(), "%02X", bt);
                                //                        }
                                _log (changeStr);
                                //  Log.i(TAG, "call: " + changeStr);

                                //                        byte[] temp;
                                //                        try {
                                //                            temp = changeStr.getBytes("UTF-8");
                                //                            Log.i(TAG, "call: " + changeStr + "," + new String(temp));
                                //                        } catch (UnsupportedEncodingException e) {
                                //                            e.printStackTrace();
                                //                        }

                                return isNullOrEmpty (changeStr);
                            }
                        }).observeOn (AndroidSchedulers.mainThread ()).subscribe (_getSearchObserver ());
            } else if (ids == R.id.radioButton2) {
                //定时发射（sample 等价于 throttleLast ） 定时400ms 采样 一次 并发送 该间隔内 最后一个采样的数据
                _subscription = RxTextView.textChangeEvents (_inputSearchText).sample (400, TimeUnit.MILLISECONDS).filter (event -> {
                    String changeStr = event.text ().toString ();
                    _log (changeStr);
                    return !TextUtils.isEmpty (changeStr);
                }).subscribeOn (AndroidSchedulers.mainThread ()).subscribe (event -> Log.i (TAG, "onNext:" + event.toString ()));
            } else {
                //every 400ms interval to emit the fisrt item during in this interval
                _subscription = RxTextView.textChanges (_inputSearchText).throttleFirst (400, TimeUnit.MILLISECONDS).filter (chars -> {
                    String change = chars.toString ();
                    _log (change);
                    return isNullOrEmpty (change);
                }).subscribe (s -> Log.i (TAG, "onNext: " + s));
            }
            // compositeSubscription.add(_subscription);
        });

        //throttleGroup.check(R.id.radioButton1);

    }


    @Bind(R.id.radioGroup)
    RadioGroup throttleGroup;


    // -----------------------------------------------------------------------------------
    // Main Rx entities

    private Observer<TextViewTextChangeEvent> _getSearchObserver() {
        return new Observer<TextViewTextChangeEvent> () {
            @Override
            public void onCompleted() {
                Timber.d("--------- onComplete");
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e, "--------- Woops on error!");
                _log("Dang error. check your logs");
            }

            @Override
            public void onNext(TextViewTextChangeEvent onTextChangeEvent) {
                _log(format("Searching for %s", onTextChangeEvent.text().toString()));
            }
        };
    }

    // -----------------------------------------------------------------------------------
    // Method that help wiring up the example (irrelevant to RxJava)

    private void _setupLogger() {
        _logs = new ArrayList<>();
        _adapter = new LogAdapter(getActivity(), new ArrayList<>());
        _logsList.setAdapter(_adapter);
    }

    private void _log(String logMsg) {

        if (_isCurrentlyOnMainThread()) {
            _logs.add(0, logMsg + " (main thread) ");
            _adapter.clear();
            _adapter.addAll(_logs);
        } else {
            _logs.add(0, logMsg + " (NOT main thread) ");

            // You can only do below stuff on main thread.
            new Handler(Looper.getMainLooper()).post(() -> {
                _adapter.clear();
                _adapter.addAll(_logs);
            });
        }
    }

    private boolean _isCurrentlyOnMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    private class LogAdapter extends ArrayAdapter<String> {

        public LogAdapter(Context context, List<String> logs) {
            super(context, R.layout.item_log, R.id.item_log, logs);
        }
    }
}
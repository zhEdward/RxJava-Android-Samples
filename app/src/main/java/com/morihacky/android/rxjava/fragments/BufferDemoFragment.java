package com.morihacky.android.rxjava.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import com.jakewharton.rxbinding.view.RxView;
import com.morihacky.android.rxjava.R;
import com.morihacky.android.rxjava.wiring.LogAdapter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import timber.log.Timber;

import static com.jakewharton.rxbinding.view.RxView.clickEvents;

/**
 * This is a demonstration of the `buffer` Observable.
 * <p>
 * The buffer observable allows taps to be collected only within a time span. So taps outside the
 * 2s limit imposed by buffer will get accumulated in the next log statement.
 * <p>
 * If you're looking for a more foolproof solution that accumulates "continuous" taps vs
 * a more dumb solution as show below (i.e. number of taps within a timespan)
 * look at {@link com.morihacky.android.rxjava.rxbus.RxBusDemo_Bottom3Fragment} where a combo
 * of `publish` and `buffer` is used.
 * <p>
 * Also http://nerds.weddingpartyapp.com/tech/2015/01/05/debouncedbuffer-used-in-rxbus-example/
 * if you're looking for words instead of code
 */
public class BufferDemoFragment extends BaseFragment {

    @Bind(R.id.list_threading_log)
    ListView _logsList;
    @Bind(R.id.btn_start_operation)
    Button _tapBtn;

    @Bind(R.id.btn3)
    Button _btn3;

    @Bind(R.id.btn4)
    Button _btn4;


    private LogAdapter _adapter;
    private List<String> _logs;

    private Subscription _subscription;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
    }

    @OnClick({R.id.btn1, R.id.btn2})
    public void btnClick(View b) {
        switch (b.getId ()) {
            case R.id.btn1:
                Log.i (TAG, "btnClick: 1");
                break;
            case R.id.btn2:
                Log.i (TAG, "btnClick: 2");
                break;
            case R.id.btn3:
                Log.i (TAG, "btnClick: 3");
                break;
        }
    }

    //    @OnClick(R.id.btn4)
    //    public void btn4() {
    //        Log.i (TAG, "btn4:");
    //    }


    @Override
    public void onResume() {
        super.onResume ();
        _subscription = _getBufferedSubscription ();
    }

    @Override
    public void onPause() {
        super.onPause ();
        _subscription.unsubscribe ();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated (savedInstanceState);
        _setupLogger ();

        // TODO: 2016/12/28  该操作符 还不知道 具体使用场景
        //interval 操作符不支持 反压
        //        Observable.interval(1, TimeUnit.MILLISECONDS)//.takeUntil (num-> num>30)
        //                .onBackpressureDrop ()//onBackpressureDrop
        //                .observeOn(Schedulers.newThread())
        //                .subscribe(new Subscriber<Long>() {
        //
        //                    @Override
        //                    public void onStart() {
        //                        Log.w(TAG,"interval-start");
        //                            request(10);
        //                    }
        //
        //                    @Override
        //                    public void onCompleted() {
        //                        Log.w (TAG, "onCompleted: " );
        //                    }
        //                    @Override
        //                    public void onError(Throwable e) {
        //                        Log.e(TAG,"interval-ERROR"+e.toString());
        //                    }
        //
        //                    @Override
        //                    public void onNext(Long aLong) {
        //                        Log.w(TAG,"TAG"+"---->"+aLong);
        //                        try {
        //                            Thread.sleep(1000);
        //                        } catch (InterruptedException e) {
        //                            e.printStackTrace();
        //                        }finally {
        //                            request(10);
        //                        }
        //                    }
        //                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate (R.layout.fragment_buffer, container, false);

        View v = layout.findViewById (R.id.btn_start_operation);
        v.setOnLongClickListener (View -> {
            Log.i (TAG, "setOnLongClickListener:");
            return true;
        });

        new Thread (() -> {
            try {
                Thread.sleep (3000);
                Log.i (TAG, "custom thread:");
            } catch (InterruptedException e) {
                e.printStackTrace ();
            }
        }).start ();

        ButterKnife.bind (this, layout);

        //对数组进行排序使用
        Arrays.sort (arrays, (s1, s2) -> {
            //"123".substring(10);

            return Integer.parseInt (s1) - Integer.parseInt (s2);
        });
        for (String s : arrays) {
            Log.i (TAG, "foreach: " + s);
        }

        List<String> list = Arrays.asList (arrays);

        //        _btn3.setOnClickListener (new View.OnClickListener () {
        //            @Override
        //            public void onClick(View v) {
        //                // "123".substring(100);
        //                Log.i (TAG, "onClick: _btn3");
        //            }
        //        });


        //把发射的事件 切割为每3个 为一组（window） 封装成 observables 再进行发射
        Observable<Observable<Integer>> oodummy = RxView.clickEvents (_btn3).map (event -> 1).window (3);
        oodummy.map (w -> ++btn3_click_count).subscribe (new Action1<Integer> () {
            @Override
            public void call(Integer integer) {
                Log.i (TAG, "btn3 call: " + integer.intValue ());
            }
        });


        RxView.clickEvents (_btn4).onBackpressureBuffer ().map (clickEvents -> ++btn4_click_count).subscribe (new Subscriber<Integer> () {
            @Override
            public void onStart() {
                super.onStart ();
                request (3);
            }

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer integer) {

                Log.i (TAG, "onNext: btn4->" + integer.intValue ());
                request (3);//
            }
        });


        // list.forEach(System.out::println); java8 unsupported grammer?


        //在指定的 looper进行 emit observable
        Looper backgroundLooper = weakHandler.getLooper ();
        Observable.just ("one", "two", "three", "four", "five").delay (2, TimeUnit.SECONDS).observeOn (AndroidSchedulers.from (backgroundLooper))//可以在 任意已存在的looper 进行订阅
                .subscribe (new Action1<String> () {
                    @Override
                    public void call(String s) {
                        _log ("onNext:" + s);
                    }
                });


        return layout;
    }

    int btn3_click_count;
    int btn4_click_count;

    public void regWindowObervable(Observable<Integer> original) {
        original.subscribe (c -> Log.i (TAG, "subscribe window cell:" + c));
    }


    private StaticHandler weakHandler = new StaticHandler (this);

    /**
     * 静态内部类配合 WeakReference 防止 Memory Leak
     * <p>
     * 原因参见 https://drakeet.me/android-leaks
     */
    public static class StaticHandler extends Handler {
        private final WeakReference<BufferDemoFragment> mActivity;


        public StaticHandler(BufferDemoFragment activity) {
            mActivity = new WeakReference<BufferDemoFragment> (activity);
        }


        @Override
        public void handleMessage(Message msg) {
            super.handleMessage (msg);
            try {
                Thread.sleep (3000);
            } catch (InterruptedException e) {
                e.printStackTrace ();
            } finally {
                if (mActivity.get () != null) {
                    // TODO: 2016/12/14
                    mActivity.get ()._log (msg.what + "," + msg.obj);
                }
            }

        }
    }


    String[] arrays = {"111", "222", "333", "444", "555", "666"};

    @Override
    public void onDestroyView() {
        super.onDestroyView ();
        ButterKnife.unbind (this);


    }

    // -----------------------------------------------------------------------------------
    // Main Rx entities

    private Subscription _getBufferedSubscription() {

        //        RxView.clickEvents(_tapBtn).map(new Func1<ViewClickEvent, String>() {
        //            @Override
        //            public String call(ViewClickEvent viewClickEvent) {
        //                return "1";
        //            }
        //        }).buffer(2000,TimeUnit.MILLISECONDS).subscribe(new Observer<List<String>>() {
        //            @Override
        //            public void onCompleted() {
        //
        //            }
        //
        //            @Override
        //            public void onError(Throwable e) {
        //
        //            }
        //
        //            @Override
        //            public void onNext(List<String> integers) {
        //
        //            }
        //        });


        // map:把每次clickEvent 事件 转变为 1次 整型次数
        return clickEvents (_tapBtn).map (onClickEvent -> {
            Timber.d ("--------- GOT A TAP");
            _log ("GOT A TAP");
            return 1;
        }).buffer (2, TimeUnit.SECONDS)//  定期2s从Observable中收集项目到bundle(中转)中并发出这些项目
                //而不是一次一个地发射物品
                .observeOn (AndroidSchedulers.mainThread ())
                //.subscribeOn(Schedulers.newThread())
                .subscribe (new Subscriber<List<Integer>> () {

                    @Override
                    public void onCompleted() {
                        // fyi: you'll never reach here
                        Timber.d ("----- onCompleted");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e (e, "--------- Woops on error!");
                        _log ("Dang error! check your logs");
                    }

                    @Override
                    public void onNext(List<Integer> integers) {
                        Timber.d ("--------- onNext");
                        if (integers.size () > 0) {
                            _log (String.format ("%d taps", integers.size ()));
                        } else {
                            _log (String.format (Locale.getDefault (), "%d taps", 0));
                            Timber.d ("--------- No taps received ");
                        }
                    }

                    @Override
                    public void onStart() {
                        super.onStart ();
                        _log ("subscriber-onStart（）");
                    }
                });
    }

    // -----------------------------------------------------------------------------------
    // Methods that help wiring up the example (irrelevant to RxJava)

    private void _setupLogger() {
        _logs = new ArrayList<> ();
        _adapter = new LogAdapter (getActivity (), new ArrayList<> ());
        _logsList.setAdapter (_adapter);
    }

    private void _log(String logMsg) {

        if (_isCurrentlyOnMainThread ()) {
            _logs.add (0, logMsg + " (main thread) ");
            _adapter.clear ();
            _adapter.addAll (_logs);
        } else {
            _logs.add (0, logMsg + " (NOT main thread) ");

            // You can only do below stuff on main thread.
            new Handler (Looper.getMainLooper ()).post (() -> {
                _adapter.clear ();
                _adapter.addAll (_logs);
            });
        }
    }

    private boolean _isCurrentlyOnMainThread() {
        return Looper.myLooper () == Looper.getMainLooper ();
    }
}

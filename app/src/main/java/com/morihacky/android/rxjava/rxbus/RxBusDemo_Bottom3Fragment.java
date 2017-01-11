package com.morihacky.android.rxjava.rxbus;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.morihacky.android.rxjava.MainActivity;
import com.morihacky.android.rxjava.R;
import com.morihacky.android.rxjava.fragments.BaseFragment;

import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;
import rx.subscriptions.CompositeSubscription;


/**
 * @see {@link RxBusDemoFragment}
 * @see {@link RxBusDemo_TopFragment}
 * <p>
 * TODO
 */
public class RxBusDemo_Bottom3Fragment extends BaseFragment {

    @Bind(R.id.demo_rxbus_tap_txt)
    TextView _tapEventTxtShow;
    @Bind(R.id.demo_rxbus_tap_count)
    TextView _tapEventCountShow;
    private RxBus _rxBus;
    private CompositeSubscription _subscriptions;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_rxbus_bottom, container, false);
        ButterKnife.bind(this, layout);
        return layout;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        _rxBus = ((MainActivity) getActivity()).getRxBusSingleton();
    }

    @Override
    public void onStart() {
        super.onStart();
        _subscriptions = new CompositeSubscription ();

        ConnectableObservable<Object> tapEventEmitter = _rxBus.asObservable ().publish ();

        _subscriptions//
                .add (tapEventEmitter.subscribe (event -> {
                    if (event instanceof RxBusDemoFragment.TapEvent) {
                        //每点击一次 就触发一次
                        _showTapText ();
                    }
                }));

        //该函数变换 没明白什么意思???
        Subscription s = tapEventEmitter.publish (new Func1<Observable<Object>, Observable<List<Object>>> () {
            @Override
            public Observable<List<Object>> call(Observable<Object> stream) {
                //  Log.i(TAG,"call me:");
                //离散 均匀，控制如果1s内订阅到多次 只发射最后一次，其余的丢弃
                //通俗理解：每次点击的间隔要在1s以上 才记录次click，放入 o1
                Observable o1 = stream.debounce (1, TimeUnit.SECONDS);
                //  Log.i(TAG,"call me1:");
                // o1 需要在 缓存到一定的量时候 才会再次发射
                return stream.buffer (o1);
            }
        })
                //               .publish(stream ->//添加一个变换函数
                //                stream.buffer(stream.debounce(1, TimeUnit.SECONDS)))
                .observeOn (AndroidSchedulers.mainThread ()).subscribe (taps -> {
                    _showTapCount (taps.size ());
                });

        _subscriptions.add (s);

        //只有订阅之后 才会emit observables
        _subscriptions.add (tapEventEmitter.connect ());

    }

    @Override
    public void onStop() {
        super.onStop();
        _subscriptions.clear ();
    }

    // -----------------------------------------------------------------------------------
    // Helper to show the text via an animation

    int displayCount;

    private void _showTapText() {
        //        Log.i(TAG, "_showTapText: trigger");
        displayCount++;
        _tapEventTxtShow.setVisibility(View.VISIBLE);
        _tapEventTxtShow.setAlpha(1f);
        ViewCompat.animate (_tapEventTxtShow).alphaBy (-1f).setDuration (400).setStartDelay (100);
    }

    private void _showTapCount(int size) {
        Log.i (TAG, "_showTapCount:" + displayCount + " is equal with " + size);
        displayCount = 0;

        _tapEventCountShow.setText(String.valueOf(size));
        _tapEventCountShow.setVisibility(View.VISIBLE);
        _tapEventCountShow.setScaleX(1f);
        _tapEventCountShow.setScaleY(1f);
        ViewCompat.animate(_tapEventCountShow).scaleXBy (-1f).scaleYBy (-1f).setDuration (800).setStartDelay (100);
    }
}

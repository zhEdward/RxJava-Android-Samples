package com.morihacky.android.rxjava.volley;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.morihacky.android.rxjava.R;
import com.morihacky.android.rxjava.fragments.BaseFragment;
import com.morihacky.android.rxjava.wiring.LogAdapter;

import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

public class VolleyDemoFragment extends BaseFragment {

    public static final String TAG = "VolleyDemoFragment";

    @Bind(R.id.list_threading_log)
    ListView _logsList;

    private List<String> _logs;
    private LogAdapter _adapter;

    private CompositeSubscription _compositeSubscription = new CompositeSubscription ();

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate (R.layout.fragment_volley, container, false);
        ButterKnife.bind (this, layout);
        return layout;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated (savedInstanceState);
        _setupLogger ();
    }

    @Override
    public void onPause() {
        super.onPause ();
        _compositeSubscription.clear ();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView ();
        ButterKnife.unbind (this);
    }

    /**
     * Creates and returns an observable generated from the Future returned from
     * {@code getRouteData()}. The observable can then be subscribed to as shown in
     * {@code startVolleyRequest()}
     *
     * @return Observable<JSONObject>
     */
    public Observable<JSONObject> newGetRouteData() {


        //annoymous inner class 写法
        //        Observable.defer(new Func0<Observable<JSONObject>>() {
        //            @Override
        //            public Observable<JSONObject> call() {
        //                try {
        //                    return Observable.just(getRouteData());
        //                } catch (ExecutionException | InterruptedException e) {
        //                    e.printStackTrace();
        //                    return Observable.error(e);
        //                }finally {
        //                }
        //            }
        //        });

        //lamba syntax
        return Observable.defer (() -> {//通过 subscribeOn 指定 调度器(异步网络请求)
            try {
                //volley 请求远程数据
                return Observable.just (getRouteData ("fuzhou"), getRouteData ("beijing"), getRouteData ("youqi"));

            } catch (InterruptedException | ExecutionException e) {
                Log.e ("routes", e.getMessage ());
                return Observable.error (e);
            }
        });
    }

    @OnClick(R.id.btn_start_operation)
    void startRequest() {
        startVolleyRequest ();
    }

    private void startVolleyRequest() {
        _compositeSubscription.add (newGetRouteData ().
                doOnSubscribe (() -> Log.i (TAG, "defer " + "has be subscribed")).
                subscribeOn (Schedulers.io ()).
                observeOn (AndroidSchedulers.mainThread ()).
                subscribe (new Observer<JSONObject> () {
                    @Override
                    public void onCompleted() {
                        Log.e (TAG, "onCompleted");
                        Timber.d ("----- onCompleted");
                        _log ("onCompleted ");
                    }

                    @Override
                    public void onError(Throwable e) {
                        VolleyError cause = (VolleyError) e.getCause ();
                        String s = new String (cause.networkResponse.data, Charset.forName ("UTF-8"));
                        Log.e (TAG, s);
                        Log.e (TAG, cause.toString ());
                        _log ("onError " + s);
                    }

                    @Override
                    public void onNext(JSONObject jsonObject) {
                        Log.e (TAG, "onNext " + jsonObject.toString ());
                        _log ("onNext " + jsonObject.toString ());

                    }
                }));
    }

    public static Map<String, String> cityMaps = new HashMap<> ();

    static {
        cityMaps.put ("beijing", "101010100");
        cityMaps.put ("fuzhou", "101230101");
        cityMaps.put ("haerbin", "101050101");
        cityMaps.put ("youqi", "101230809");
    }



    /**
     * 注意异步调用 否则阻塞main thread
     * <p>
     * Converts the Asynchronous Request into a Synchronous Future that can be used to
     * block via {@code Future.get()}. Observables require blocking/synchronous functions
     *
     *@param  pinyinCity
     *
     * @return JSONObject
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private JSONObject getRouteData(String pinyinCity) throws ExecutionException, InterruptedException {
        Log.i (TAG, "volley start");

        Thread.sleep (1000);

        RequestFuture<JSONObject> future = RequestFuture.newFuture ();
        String url = "http://www.weather.com.cn/adat/sk/" + cityMaps.get (pinyinCity).toString () + ".html";
        // url = "http://www.weather.com.cn/adat/sk/101010100.html";
        if (url == null) {
            throw new IllegalArgumentException ("查询地拼音不正确 或者暂时没有收入");
        } else {
            Log.i (TAG, "getRouteData: " + url);
        }
        JsonObjectRequest req = new JsonObjectRequest (Request.Method.GET, url, future, future);

        MyVolley.getRequestQueue ().add (req);
        Log.i (TAG, "volley over");
        return future.get ();
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

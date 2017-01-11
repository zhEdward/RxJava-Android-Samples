package com.morihacky.android.rxjava.fragments;

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
import android.widget.TextView;

import com.morihacky.android.rxjava.R;
import com.morihacky.android.rxjava.retrofit.Contributor;
import com.morihacky.android.rxjava.retrofit.GithubApi;
import com.morihacky.android.rxjava.retrofit.GithubService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * 本地缓存啊/在线数据 公共作用
 */
public class PseudoCacheFragment extends BaseFragment {

    @Bind(R.id.info_pseudoCache_demo)
    TextView infoText;
    @Bind(R.id.info_pseudoCache_listSubscription)
    ListView listSubscriptionInfo;
    @Bind(R.id.info_pseudoCache_listDtl)
    ListView listDetail;

    private ArrayAdapter<String> adapterDetail, adapterSubscriptionInfo;
    private HashMap<String, Long> contributionMap = null;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_pseudo_cache, container, false);
        ButterKnife.bind(this, layout);
        return layout;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    @OnClick(R.id.btn_pseudoCache_concat)
    public void onConcatBtnClicked() {
        infoText.setText(R.string.msg_pseudoCache_demoInfo_concat);
        wireupDemo();
        //观察者 -> 订阅者  subscriber
        //被观察对象        observable

        // 按照concat  顺序连接被观察者
        //
        Observable.concat(getSlowCachedDiskData(), getFreshNetworkData()).subscribeOn (Schedulers.io ()) // observber 在异步调度器上订阅 该被观察对象
                .observeOn (AndroidSchedulers.mainThread ())//指定被观察者 在主线程上 调用observber（subscriber的 onNext onComplete onError）
                .subscribe (new Subscriber<Contributor> () {//主线程回调
                    @Override
                    public void onCompleted() {
                        Timber.d ("done loading all data");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e (e, "arr something went wrong");
                    }

                    @Override
                    public void onNext(Contributor contributor) {
                        contributionMap.put (contributor.login, contributor.contributions);
                        adapterDetail.clear ();
                        adapterDetail.addAll (mapAsList (contributionMap));
                    }


                });
    }

    @OnClick(R.id.btn_pseudoCache_concatEager)
    public void onConcatEagerBtnClicked() {
        infoText.setText(R.string.msg_pseudoCache_demoInfo_concatEager);
        wireupDemo();

        //订阅同时发生，减少等待耗时（这里disk cache IO数据过慢，如何还是依照 concat顺序订阅的话，将导致整个执行时长延迟 ）
        //Observable.concate

        Observable.concatEager (getCachedDiskData (), getFreshNetworkData ()).subscribeOn (Schedulers.io ()) // we want to add a list item at time of subscription
                .observeOn (AndroidSchedulers.mainThread ()).subscribe (new Subscriber<Contributor> () {
            @Override
            public void onCompleted() {
                Timber.d ("done loading all data");
            }

            @Override
            public void onError(Throwable e) {
                Timber.e (e, "arr something went wrong");
            }

            @Override
            public void onNext(Contributor contributor) {
                contributionMap.put (contributor.login, contributor.contributions);
                adapterDetail.clear ();
                adapterDetail.addAll (mapAsList (contributionMap));
            }
        });

    }

    @OnClick(R.id.btn_pseudoCache_merge)
    public void onMergeBtnClicked() {
        infoText.setText(R.string.msg_pseudoCache_demoInfo_merge);
        wireupDemo();

        //类似于 concat 但是 2个 pbservables合并后的数据交错在一起 顺序无法确定(不建议使用)
        Observable.merge(getCachedDiskData(), getFreshNetworkData()).subscribeOn (Schedulers.io ()) // we want to add a list item at time of subscription
                .observeOn (AndroidSchedulers.mainThread ()).subscribe (new Subscriber<Contributor> () {
            @Override
            public void onCompleted() {
                Timber.d ("done loading all data");
            }

            @Override
            public void onError(Throwable e) {
                Timber.e (e, "arr something went wrong");
            }

            @Override
            public void onNext(Contributor contributor) {
                contributionMap.put (contributor.login, contributor.contributions);
                adapterDetail.clear ();
                adapterDetail.addAll (mapAsList (contributionMap));
            }
        });
    }

    @OnClick(R.id.btn_pseudoCache_mergeSlowDisk)
    public void onMergeSlowBtnClicked() {
        infoText.setText(R.string.msg_pseudoCache_demoInfo_mergeSlowDisk);
        wireupDemo();

        //访问网络获得数据 可能被先展示，知道 本地缓存读取完毕 将覆盖"最新"的数据，导致显示错乱（不建议使用）
        Observable.merge(getSlowCachedDiskData(), getFreshNetworkData()).subscribeOn (Schedulers.io ()) // we want to add a list item at time of subscription
                .observeOn (AndroidSchedulers.mainThread ()).subscribe (new Subscriber<Contributor> () {
            @Override
            public void onCompleted() {
                Timber.d ("done loading all data");
            }

            @Override
            public void onError(Throwable e) {
                Timber.e (e, "arr something went wrong");
            }

            @Override
            public void onNext(Contributor contributor) {
                contributionMap.put (contributor.login, contributor.contributions);
                adapterDetail.clear ();
                adapterDetail.addAll (mapAsList (contributionMap));
            }
        });
    }

    //在 读取 本地/网络 缓存数据并进行切换的过程 该操作机制比较 适用大部分请求场景
    @OnClick(R.id.btn_pseudoCache_mergeOptimized)
    public void onMergeOptimizedBtnClicked() {
        infoText.setText(R.string.msg_pseudoCache_demoInfo_mergeOptimized);
        wireupDemo();

        getFreshNetworkData ().publish (network -> {
            //另一种pattern（取消{} ,return 关键字）
            return Observable.merge (network, getCachedDiskData ().takeUntil (network));
        }).subscribeOn (Schedulers.io ()) // we want to add a list item at time of subscription 异步执行
                .observeOn (AndroidSchedulers.mainThread ()).subscribe (new Subscriber<Contributor> () {
            @Override
            public void onCompleted() {
                Timber.d ("done loading all data");
            }

            @Override
            public void onError(Throwable e) {
                Timber.e (e, "arr something went wrong");
            }

            @Override
            public void onNext(Contributor contributor) {
                contributionMap.put (contributor.login, contributor.contributions);
                adapterDetail.clear ();
                adapterDetail.addAll (mapAsList (contributionMap));
            }
        });
    }


    //该方式：先请求network在此过程中如果local cache 完成就先显示出来
    //如果 network优于 local cache完成 那将丢弃local 直接display network
    @OnClick(R.id.btn_pseudoCache_mergeOptimizedSlowDisk)
    public void onMergeOptimizedWithSlowDiskBtnClicked() {
        infoText.setText(R.string.msg_pseudoCache_demoInfo_mergeOptimizedSlowDisk);
        wireupDemo();


        getFreshNetworkData ().publish (new Func1<Observable<Contributor>, Observable<Contributor>> () {
            @Override
            public Observable<Contributor> call(Observable<Contributor> network) {
                //当 network 开始发送数据的时候 ，就结束掉 takeUntil返回的observable 的发射的items

                return Observable.merge (network, getSlowCachedDiskData ().takeUntil (network));
            }
        })
                //  equal as above
                //        getFreshNetworkData()//
                //                .publish(network ->//
                //                        Observable.merge(network,//
                //                                getSlowCachedDiskData().takeUntil(network)))

                .subscribeOn (Schedulers.io ()) // we want to add a list item at time of subscription
                .observeOn (AndroidSchedulers.mainThread ()).subscribe (new Subscriber<Contributor> () {
            @Override
            public void onCompleted() {
                Timber.d ("done loading all data");
            }

            @Override
            public void onError(Throwable e) {
                Timber.e (e, "arr something went wrong");

            }

            @Override
            public void onNext(Contributor contributor) {
                contributionMap.put (contributor.login, contributor.contributions);
                adapterDetail.clear ();
                adapterDetail.addAll (mapAsList (contributionMap));
            }
        });
    }

    // -----------------------------------------------------------------------------------
    // WIRING for example

    private void wireupDemo() {
        contributionMap = new HashMap<>();

        adapterDetail = new ArrayAdapter<>(getActivity(), R.layout.item_log_white, R.id.item_log, new ArrayList<>());
        listDetail.setAdapter(adapterDetail);

        adapterSubscriptionInfo = new ArrayAdapter<>(getActivity(), R.layout.item_log_white, R.id.item_log, new ArrayList<> ());
        listSubscriptionInfo.setAdapter(adapterSubscriptionInfo);
    }

    private Observable<Contributor> getSlowCachedDiskData() {

        Observable.timer (3000, TimeUnit.MILLISECONDS).flatMap (new Func1<Long, Observable<?>> () {
            @Override
            public Observable<?> call(Long aLong) {
                return getCachedDiskData ();
            }
        });

        //只是为了 模拟延迟操作 flatMap 并无任何操作
        return Observable.timer (3, TimeUnit.SECONDS).flatMap (dummy -> getCachedDiskData ());//延迟3s 获取本地缓存的数据
    }

    /**
     * 调用本地缓存数据先
     *
     * @return
     */
    private Observable<Contributor> getCachedDiskData() {
        List<Contributor> list = new ArrayList<>();
        Map<String, Long> map = dummyDiskData();

        for (String username : map.keySet()) {
            Contributor c = new Contributor();
            c.login = username;
            c.contributions = map.get(username);
            list.add(c);
        }

        return Observable.from (list)//
                .doOnSubscribe (() -> new Handler (Looper.getMainLooper ())//
                        .post (() -> adapterSubscriptionInfo.add ("(disk) cache subscribed")))//
                //                .doOnCompleted(() -> new Handler(Looper.getMainLooper())//
                //                        .post(() -> adapterSubscriptionInfo.add("(disk) cache completed")));
                //equal with above
                .doOnCompleted (new Action0 () {
                    @Override
                    public void call() {
                        new Handler (Looper.getMainLooper ()).post (() -> {
                            adapterSubscriptionInfo.add ("(disk) cache completed");
                        });
                    }
                }).doOnError (new Action1<Throwable> () {
                    @Override
                    public void call(Throwable throwable) {
                        new Handler (Looper.getMainLooper ()).post (() -> {
                            //when emit encounter problem
                        });
                    }
                });
    }

    /**
     * 再进行网络请求，拉取最新数据 (延时2s 才开始请求newtork 数据，并且设置 1s 发射超时)
     *
     * @return
     */
    private Observable<Contributor> getFreshNetworkData() {
        String githubToken = getResources().getString(R.string.github_oauth_token);
        GithubApi githubService = GithubService.createGithubService(githubToken);
        Observable<List<Contributor>> ox = githubService.contributors ("square", "retrofit");
        //这里 单一的list<Contributor> 订阅项 函数变换为 单个 <Contributor> 订阅项
        return ox.flatMap (contributors -> {

            try {
                Thread.sleep (2000);
            } catch (InterruptedException e) {
                Log.e (TAG, "getFreshNetworkData: " + e.toString ());
            }
            return Observable.from (contributors);
        })// 对解析回来的数据 再进包装然后发射
                .timeout (100, TimeUnit.SECONDS).doOnSubscribe (() -> new Handler (Looper.getMainLooper ())//
                        .post (() -> adapterSubscriptionInfo.add ("(network) subscribed")))//
                .doOnCompleted (() -> new Handler (Looper.getMainLooper ())//
                        .post (() -> adapterSubscriptionInfo.add ("(network) completed")));
    }

    //替代 combineLatest annoymous inner class
    private List<String> mapAsList(HashMap<String, Long> map) {
        List<String> list = new ArrayList<>();

        for (String username : map.keySet()) {
            String rowLog = String.format ("%s [贡献数 %d]", username, contributionMap.get (username));
            list.add(rowLog);
        }

        return list;
    }

    /**
     * 本地缓存假数据
     *
     * @return
     */
    private Map<String, Long> dummyDiskData() {
        Map<String, Long> map = new HashMap<>();
        map.put("JakeWharton", 0L);
        map.put("pforhan", 0L);
        map.put("edenman", 0L);
        map.put("swankjesse", 0L);
        map.put("bruceLee", 0L);
        return map;
    }
}

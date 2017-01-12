package com.morihacky.android.rxjava.pagination;

import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.morihacky.android.rxjava.MainActivity;
import com.morihacky.android.rxjava.R;
import com.morihacky.android.rxjava.fragments.BaseFragment;
import com.morihacky.android.rxjava.rxbus.RxBus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

public class PaginationFragment extends BaseFragment {

    @Bind(R.id.list_paging)
    RecyclerView _pagingList;
    @Bind(R.id.progress_paging)
    ProgressBar _progressBar;
    @Bind(R.id.title)
    TextView _title;

    private CompositeSubscription _subscriptions;
    private PaginationAdapter _adapter;
    private RxBus _bus;
    private PublishSubject<Integer> _paginator;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated (savedInstanceState);

        _bus = ((MainActivity) getActivity ()).getRxBusSingleton ();

        LinearLayoutManager layoutManager = new LinearLayoutManager (getActivity ());
        layoutManager.setOrientation (LinearLayoutManager.VERTICAL);
        _pagingList.setLayoutManager (layoutManager);

        _adapter = new PaginationAdapter (_bus);
        _pagingList.setAdapter (_adapter);

        _paginator = PublishSubject.create ();
    }

    @Override
    public void onStart() {
        super.onStart ();
        _subscriptions = new CompositeSubscription ();


        //subscribeOn : 不指定，则表示所有操作符在 subscribe()所执行的调度器上
        //observerOn  ：指定 subscribe() 回调方法所执行线程

        Subscription s2 = _paginator.doOnSubscribe (() -> {
            Log.i (TAG, "doOnSubscribe: " + isCurrentThread ());
        }).onBackpressureDrop ()//让 observables 支持 背压操作
                .concatMap (nextPage -> _itemsFromNetworkCall (nextPage + 1, 10))//
                .observeOn (AndroidSchedulers.mainThread ())//控制（未指定调度器的）所有操作符，在主线程执行
                .map (items -> {
                    //该op不再特定scheduler执行，但是指定的事务涉及到 mainthread
                    //所以在该op的链式调用前需要用 observeOn 指定 主线程中执行
                    int start = _adapter.getItemCount () - 1;
                    _adapter.addItems (items);
                    _adapter.notifyItemRangeInserted (start, 10);

                    _progressBar.setVisibility (View.INVISIBLE);
                    Log.i (TAG, "map-op " + isCurrentThread ());
                    return null;
                }).subscribe (o -> {
                    //request (10);// call in subscriber(onNext,onStart)
                    Log.i (TAG, "onNext-callback " + isCurrentThread ());
                });


        // I'm using an Rxbus purely to hear from a nested button click
        // we don't really need Rx for this part. it's just easy ¯\_(ツ)_/¯
        Subscription s1 = _bus.asObservable ().subscribe (event -> {
            if (event instanceof PaginationAdapter.ItemBtnViewHolder.PageEvent) {

                // trigger the paginator for the next event
                //接收到 事件总线 开始 加载更多数据
                int nextPage = _adapter.getItemCount () - 1;
                _paginator.onNext (nextPage);

            }
        });

        _subscriptions.add (s1);
        _subscriptions.add (s2);
    }


    public String isCurrentThread() {
        return (Looper.getMainLooper () == Looper.myLooper ()) ? " [Main Thread] " : " [Not Main Thread] ";
    }

    @Override
    public void onStop() {
        super.onStop ();
        _subscriptions.clear ();
    }


    /**
     * Fake Observable that simulates a network call and then sends down a list of items
     *
     * @param start item项 开始加载的编号
     * @param count 一次刷新 加载多少项
     * @return
     */
    private Observable<List<String>> _itemsFromNetworkCall(int start, int count) {
        return Observable.just (true).map (dummy -> {
            Log.i (TAG, "_itemsFromNetworkCall-map1-op" + isCurrentThread ());
            return dummy;
        }).doOnUnsubscribe (() -> Log.i (TAG, "_itemsFromNetworkCall-doOnUnscribe-op:" + isCurrentThread ())).doOnSubscribe (() -> Log.i (TAG, "_itemsFromNetworkCall-doOnSubscribe-op:" + isCurrentThread ())).doOnCompleted (() -> Log.i (TAG, "_itemsFromNetworkCall-doOnCompleted-callback:" + isCurrentThread ()))
                //.observeOn (AndroidSchedulers.mainThread ())//之前delay op 异步操作 所以要调度主线程才可以指定doOnNext（）
                .doOnNext (dummy -> {//每次触发加载新数据的时候 显示 progressbar
                    _progressBar.setVisibility (View.VISIBLE);
                    Log.i (TAG, "_itemsFromNetworkCall-doOnNext-callback:" + isCurrentThread ());
                }).observeOn (AndroidSchedulers.mainThread ())//在主线程通知订阅者
                .delay (2, TimeUnit.SECONDS)//非主线程（delay op 将会在 computation 调度器执行）
                // ↓↓ 之后的op（e.g map op） 如果没有指定调度器都将是异步线程执行事物
                .map (dummy -> {//boolean to List<String> transformed

                    Log.i (TAG, "_itemsFromNetworkCall-map2-op: " + isCurrentThread ());
                    List<String> items = new ArrayList<> ();
                    for (int i = 0; i < count; i++) {
                        items.add ("Item " + (start + i));
                    }
                    return items;
                });
    }


    // -----------------------------------------------------------------------------------
    // WIRING up the views required for this example

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate (R.layout.fragment_pagination, container, false);
        ButterKnife.bind (this, layout);
        return layout;
    }


}

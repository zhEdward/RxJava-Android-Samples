package com.morihacky.android.rxjava.fragments;

import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.morihacky.android.rxjava.R;
import com.morihacky.android.rxjava.retrofit.Contributor;
import com.morihacky.android.rxjava.retrofit.GithubApi;
import com.morihacky.android.rxjava.retrofit.GithubService;
import com.morihacky.android.rxjava.retrofit.User;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static android.text.TextUtils.isEmpty;
import static java.lang.String.format;

/***
 * TODO 未看，很多知识点不了解
 */
public class RetrofitFragment extends Fragment {

    @Bind(R.id.demo_retrofit_contributors_username)
    EditText _username;
    @Bind(R.id.demo_retrofit_contributors_repository)
    EditText _repo;
    @Bind(R.id.log_list)
    ListView _resultList;

    private ArrayAdapter<String> _adapter;
    private GithubApi _githubService;
    private CompositeSubscription _subscriptions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String githubToken = getResources().getString(R.string.github_oauth_token);
        _githubService = GithubService.createGithubService(githubToken);

        _subscriptions = new CompositeSubscription ();


    }

    final String TAG = RetrofitFragment.class.getSimpleName ();

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View layout = inflater.inflate(R.layout.fragment_retrofit, container, false);
        ButterKnife.bind(this, layout);

        _adapter = new ArrayAdapter<>(getActivity(), R.layout.item_log, R.id.item_log, new ArrayList<>());
        //_adapter.setNotifyOnChange(true);
        _resultList.setAdapter(_adapter);

        return layout;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        _subscriptions.unsubscribe ();
    }


    @OnClick(R.id.btn_demo_retrofit_contributors)
    public void onListContributorsClicked() {
        _adapter.clear();


        _subscriptions.add (//
                //Observable.just(ArrayList < Contributor >).ob
                _githubService.contributors (_username.getText ().toString (), _repo.getText ().toString ()).subscribeOn (Schedulers.io ())//IO 线程中处理 网络请求
                        .observeOn (AndroidSchedulers.mainThread ())//把处理的结果在 mainThread 发送到Observer中(onNext onComplete onError e.g. )
                        .subscribe (new Subscriber<List<Contributor>> () {
                            @Override
                            public void onCompleted() {
                                Timber.d ("Retrofit call 1 completed");
                            }

                            @Override
                            public void onError(Throwable e) {
                                Timber.e (e, "woops we got an error while getting the list of contributors");
                            }

                            @Override
                            public void onNext(List<Contributor> contributors) {
                                Log.i (TAG, "onNext: " + contributors.size () + " in main thread? " + _isCurrentlyOnMainThread ());

                                for (Contributor c : contributors) {
                                    //                                    _adapter.add(format("%s has made %d contributions to %s",
                                    //                                            c.login,
                                    //                                            c.contributions,
                                    //                                            _repo.getText().toString()));

                                    Timber.d ("%s has made %d contributions to %s", c.login, c.contributions, _repo.getText ().toString ());
                                }
                            }

                            @Override
                            public void onStart() {
                                super.onStart ();
                                Log.i (TAG, "onStart: isMainthread:" + _isCurrentlyOnMainThread ());
                                Toast.makeText (getActivity (), "onStart", Toast.LENGTH_SHORT).show ();
                            }

                            private boolean _isCurrentlyOnMainThread() {
                                return Looper.myLooper () == Looper.getMainLooper ();
                            }
                        }));
    }

    @OnClick(R.id.btn_demo_retrofit_contributors_with_user_info)
    public void onListContributorsWithFullUserInfoClicked() {
        _adapter.clear();


        _subscriptions.add (_githubService.contributors (_username.getText ().toString (), _repo.getText ().toString ())
                //  .flatMap(Observable::from) 等价于如下写法
                .flatMap (new Func1<List<Contributor>, Observable<Contributor>> () {
                    @Override
                    public Observable<Contributor> call(List<Contributor> contributors) {
                        //单个 list<object> observable 转为  list.size() 长度的的 observables 进行发射
                        return Observable.from (contributors);
                    }
                })//多次使用 函数变换 observable
                .flatMap (new Func1<Contributor, Observable<Pair<User, Contributor>>> () {

                    @Override
                    public Observable<Pair<User, Contributor>> call(Contributor contributor) {
                        Observable<User> _userObservable = _githubService.user (contributor.login).filter (user -> !isEmpty (user.name) && !isEmpty (user.email) && !isEmpty (user.location));
                        // 把contributor贡献数 + 拥有完整信息的contributor 的2个可以观察对象 打包进行emit
                        return Observable.zip (_userObservable, Observable.just (contributor), Pair::new);
                    }
                })//等价于如下的lambda表达式
                //                .flatMap(contributor -> {//抽取contributors的 子成员变量user过滤后，
                //                    Observable<User> _userObservable = _githubService.user(contributor.login)
                //                            .filter(user -> !isEmpty(user.name) && !isEmpty(user.email) && !isEmpty(user.location));
                //                    // 把contributor贡献数 + 拥有完整信息的contributor 的2个可以观察对象 打包进行emit
                //                    return Observable.zip(_userObservable,
                //                            Observable.just(contributor),
                //                            Pair::new);//把 发送的两组 observable 压缩(zip)成一个 Pair<K,T> 再进行发射
                //                    //zip 值发射数据项最少的observable 项
                //                })
                .subscribeOn (Schedulers.newThread ()).observeOn (AndroidSchedulers.mainThread ()).subscribe (new Subscriber<Pair> () {
                    @Override
                    public void onCompleted() {
                        Timber.d ("Retrofit call 2 completed ");
                        _adapter.add ("Retrofit call 2 completed ");

                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e (e, "error while getting the list of contributors along with full " + "names");
                    }

                    @Override
                    public void onNext(Pair pair) {
                        User user = ((Pair<User, Contributor>) pair).first;
                        Contributor contributor = ((Pair<User, Contributor>) pair).second;

                        _adapter.add (format ("%s(%s) from %s has made %d contributions to %s", user.name, user.email, user.location, contributor.contributions, _repo.getText ().toString ()));

                        _adapter.notifyDataSetChanged ();

                        Timber.d ("%s(%s) from %s has made %d contributions to %s", user.name, user.email, user.location, contributor.contributions, _repo.getText ().toString ());
                    }
                }));
    }
}

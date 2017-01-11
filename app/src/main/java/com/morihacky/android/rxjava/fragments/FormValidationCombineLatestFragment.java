package com.morihacky.android.rxjava.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.jakewharton.rxbinding.widget.RxTextView;
import com.morihacky.android.rxjava.R;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action1;
import timber.log.Timber;

import static android.text.TextUtils.isEmpty;
import static android.util.Patterns.DOMAIN_NAME;
import static android.util.Patterns.EMAIL_ADDRESS;

/**
 *
 */
public class FormValidationCombineLatestFragment extends BaseFragment {

    @Bind(R.id.btn_demo_form_valid)
    TextView _btnValidIndicator;
    @Bind(R.id.demo_combl_email)
    EditText _email;
    @Bind(R.id.demo_combl_password)
    EditText _password;
    @Bind(R.id.demo_combl_num)
    EditText _number;

    private Observable<CharSequence> _emailChangeObservable;
    private Observable<CharSequence> _passwordChangeObservable;
    private Observable<CharSequence> _numberChangeObservable;

    private Subscription _subscription = null;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate (R.layout.fragment_form_validation_comb_latest, container, false);
        ButterKnife.bind(this, layout);

        // 订阅该observable 会立马发射数据（除非构造 connectableObservable） 到 onNext中所以 使用skip(1)剔除第一次发射的无用数据
        _emailChangeObservable = RxTextView.textChanges (_email).skip (1);
        _passwordChangeObservable = RxTextView.textChanges (_password).skip (1);
        _numberChangeObservable = RxTextView.textChanges (_number).skip (1);

        _combineLatestEvents();

        return layout;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
        _subscription.unsubscribe ();
    }

    private boolean zipApi(CharSequence newEmail, CharSequence newPassword, CharSequence newNumber) {
        Log.i (TAG, "zipApi: 1111111");
        return true;
    }

    private void _combineLatestEvents() {

        //        Observable.combineLatest(_emailChangeObservable, _passwordChangeObservable, _numberChangeObservable, new Func3<CharSequence, CharSequence, CharSequence, Boolean>() {
        //            @Override
        //            public Boolean call(CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3) {
        //                //子线程处理 事件
        //                return null;
        //            }
        //        }).subscribe(new Observer<Boolean>() {
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
        //            public void onNext(Boolean aBoolean) {
        //
        //            }
        //        });


        DOMAIN_NAME.matcher ("ssss");

        //首次发射：源订阅集合每个observable都发射一次数据 触发
        //再次发射：源订阅集合每个observable均重新发射一次数据 才触发
        Observable.zip (_emailChangeObservable, _passwordChangeObservable, _numberChangeObservable, this::zipApi).subscribe (new Action1<Boolean> () {
            @Override
            public void call(Boolean aBoolean) {
                Log.d (TAG, "call: for lamba " + aBoolean);
            }
        });


        _subscription = Observable.combineLatest (_emailChangeObservable, _passwordChangeObservable, _numberChangeObservable, (newEmail, newPassword, newNumber) -> {
            //首次发射：只有当前所订阅的observables 全部都发射一次数据之后才会触发
            //再次发射：订阅的observables中有任意一个发射新的项，就会触发新的回调
            Log.i (TAG, "_combineLatestEvents: 1111111");

            //使用正则表达式 判断email 是否合法
            boolean emailValid = !isEmpty (newEmail) && EMAIL_ADDRESS.matcher (newEmail).matches ();
            if (!emailValid) {
                _email.setError ("Invalid Email!");
            }

            boolean passValid = !isEmpty (newPassword) && newPassword.length () > 8;
            if (!passValid) {
                _password.setError ("Invalid Password!");
            }

            boolean numValid = !isEmpty (newNumber);
            if (numValid) {
                int num = Integer.parseInt (newNumber.toString ());
                numValid = num > 0 && num <= 100;
            }
            if (!numValid) {
                _number.setError ("Invalid Number!");
            }

            Log.i (TAG, "_combineLatestEvents: " + emailValid + "," + passValid + "," + numValid);

            return emailValid && passValid && numValid;

        })//func method处于异步操作
                .subscribe (new Observer<Boolean> () {
                    @Override
                    public void onCompleted() {
                        Timber.d ("completed");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e (e, "there was an error");
                    }

                    @Override
                    public void onNext(Boolean formValid) {
                        //三个条件全部满足 才可以 认为合法
                        if (formValid) {
                            _btnValidIndicator.setBackgroundColor (ContextCompat.getColor (getActivity (), R.color.blue));
                        } else {
                            _btnValidIndicator.setBackgroundColor (ContextCompat.getColor (getActivity (), R.color.gray));
                        }


                    }
                });
    }
}

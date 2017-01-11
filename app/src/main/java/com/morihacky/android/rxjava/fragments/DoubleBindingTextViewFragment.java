package com.morihacky.android.rxjava.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.morihacky.android.rxjava.R;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnTextChanged;
import rx.Subscription;
import rx.functions.Action1;
import rx.subjects.PublishSubject;

import static android.text.TextUtils.isEmpty;

/**
 * 使用 publishSubject 为范本构造 rxbus 雏形
 */
public class DoubleBindingTextViewFragment
      extends BaseFragment {

    @Bind(R.id.double_binding_num1) EditText _number1;
    @Bind(R.id.double_binding_num2) EditText _number2;
    @Bind(R.id.double_binding_result) TextView _result;

    Subscription _subscription;
    PublishSubject<Float> _resultEmitterSubject;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_double_binding_textview, container, false);
        ButterKnife.bind(this, layout);

        _resultEmitterSubject = PublishSubject.create ();


        //        _subscription = _resultEmitterSubject//
        //              .asObservable()//
        //              .subscribe(aFloat -> {
        //                  _result.setText(String.valueOf(aFloat));
        //              });

        //lambda -> anonymous inner class
        _subscription = _resultEmitterSubject.asObservable ().subscribe (new Action1<Float> () {
            @Override
            public void call(Float aFloat) {
                _result.setText (String.valueOf (aFloat));
            }
        });

        onNumberChanged();
        _number2.requestFocus();

        return layout;
    }

    //num1 num2 的 float数字文本发生变化回调
    @OnTextChanged({R.id.double_binding_num1, R.id.double_binding_num2})
    public void onNumberChanged() {
        float num1 = 0;
        float num2 = 0;

        if (!isEmpty(_number1.getText().toString())) {
            num1 = Float.parseFloat(_number1.getText().toString());
        }

        if (!isEmpty(_number2.getText().toString())) {
            num2 = Float.parseFloat(_number2.getText().toString());
        }

        //发布观察事件(注意遵循 原则)
        _resultEmitterSubject.onNext(num1 + num2);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _subscription.unsubscribe ();
        ButterKnife.unbind(this);
    }
}

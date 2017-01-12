package com.morihacky.android.rxjava.pagination;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.morihacky.android.rxjava.R;
import com.morihacky.android.rxjava.rxbus.RxBus;

import java.util.ArrayList;
import java.util.List;

class PaginationAutoAdapter
      extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    final String TAG = "PaginationAutoAdapter";

    private static final int ITEM_LOG = 0;

    private final List<String> _items = new ArrayList<>();
    private final RxBus _bus;

    PaginationAutoAdapter(RxBus bus) {
        _bus = bus;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return ItemLogViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((ItemLogViewHolder) holder).bindContent(_items.get(position));

        //判断当前位置是否已经在recycleview 末尾 触发加载事件(实现自动加载更多)
        Log.i (TAG, "onBindViewHolder: " + position);
        boolean lastPositionReached = position == _items.size() - 1;
        if (lastPositionReached) {
            //事件总线 发出通知
            _bus.send(new PageEvent());
        }
    }

    @Override
    public int getItemViewType(int position) {
        return ITEM_LOG;
    }

    @Override
    public int getItemCount() {
        return _items.size();
    }

    //子序列片段 合并到 集合序列中
    void addItems(List<String> items) {
        _items.addAll(items);
    }

    private static class ItemLogViewHolder
          extends RecyclerView.ViewHolder {
        ItemLogViewHolder(View itemView) {
            super(itemView);
        }

        static ItemLogViewHolder create(ViewGroup parent) {
            return new ItemLogViewHolder(LayoutInflater.from(parent.getContext())
                                               .inflate(R.layout.item_log, parent, false));
        }

        void bindContent(String content) {
            ((TextView) itemView).setText(content);
        }
    }

    static class PageEvent { }
}

package com.allcom.elastos_webrtc_android_demo.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

public class BaseAdapter<T> extends RecyclerView.Adapter<BaseHolder> {
    public OnItemClickListener mOnItemClickListener;
    public List<T> mList = new ArrayList<>();
    private int layoutId;

    public BaseAdapter(int layoutId) {
        this.layoutId = layoutId;
    }

    public void addAll(List<T> mList) {
        if (mList != null && mList.size() > 0) {
            this.mList.addAll(mList);
        }
    }

    public void add(T entity) {
        if (entity != null) {
            mList.add(entity);
        }
    }

    public void clear() {
        if (mList != null) {
            mList.clear();
            notifyDataSetChanged();
        }
    }

    //onCreateViewHolder用来给rv创建缓存
    @Override
    public BaseHolder onCreateViewHolder(ViewGroup parent, final int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        BaseHolder holder = new BaseHolder(view);
        return holder;
    }

    //onBindViewHolder给缓存控件设置数据
    @Override
    public void onBindViewHolder(final BaseHolder holder, final int position) {
        final T item = mList.get(position);
        if (mOnItemClickListener != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemClickListener.onClick(holder.itemView, position, item);
                }
            });
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mOnItemClickListener.onLongClick(holder.itemView, position, item);
                    return true;
                }
            });
        }
        convert(holder, item, position);
    }

    /**
     * @param holder 列表视图holder.
     * @param item   绑定视图实体类
     */
    protected void convert(final BaseHolder holder, T item) {
        //什么都没有做

    }

    /**
     * 实现类可选择实现convert(final BaseHolder holder, T item, final int position).
     * 或
     * convert(BaseHolder holder, T item)
     *
     * @param holder   列表视图holder
     * @param item     绑定视图实体类
     * @param position 位置
     */
    protected void convert(final BaseHolder holder, T item, final int position) {
        //什么都没有做
        convert(holder, item);
    }

    /**
     * @return
     */
    @Override
    public int getItemCount() {
        return mList == null ? 0 : mList.size();
    }


    public interface OnItemClickListener<T> {
        void onClick(View view, int position, T item);

        void onLongClick(View view, int position, T item);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }


}

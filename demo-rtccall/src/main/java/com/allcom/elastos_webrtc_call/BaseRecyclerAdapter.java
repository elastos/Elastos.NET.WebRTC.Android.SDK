package com.allcom.elastos_webrtc_call;

import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;

import androidx.annotation.LayoutRes;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * BaseRecyclerAdapter
 * Created by mingzhenli on 2017/12/11.
 */
public abstract class BaseRecyclerAdapter<T> extends RecyclerView.Adapter<SmartViewHolder> implements ListAdapter {
    private final int mLayoutId;
    protected final List<T> mList;
    private final DataSetObservable mDataSetObservable = new DataSetObservable();
    private int mLastPosition = -1;
    private boolean mOpenAnimationEnable = true;
    private AdapterView.OnItemClickListener mListener;

    protected BaseRecyclerAdapter(@LayoutRes int layoutId) {
        setHasStableIds(false);
        this.mList = new ArrayList<>();
        this.mLayoutId = layoutId;
    }

    public BaseRecyclerAdapter(Collection<T> collection, @LayoutRes int layoutId) {
        setHasStableIds(false);
        this.mList = new ArrayList<>(collection);
        this.mLayoutId = layoutId;
    }

    public BaseRecyclerAdapter(Collection<T> collection, @LayoutRes int layoutId, AdapterView.OnItemClickListener listener) {
        setHasStableIds(false);
        setOnItemClickListener(listener);
        this.mList = new ArrayList<>(collection);
        this.mLayoutId = layoutId;
    }

    private void addAnimate(SmartViewHolder holder, int postion) {
        if (mOpenAnimationEnable && mLastPosition < postion) {
            holder.itemView.setAlpha(0);
            holder.itemView.animate().alpha(1).start();
            mLastPosition = postion;
        }
    }

    @Override
    public SmartViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new SmartViewHolder(LayoutInflater.from(parent.getContext()).inflate(mLayoutId, parent, false), mListener);
    }

    @Override
    public void onBindViewHolder(SmartViewHolder holder, int position) {
        onBindViewHolder(holder, position < mList.size() ? mList.get(position) : null, position);
    }

    protected abstract void onBindViewHolder(SmartViewHolder holder, T model, int position);

    @Override
    public int getItemCount() {
        return mList.size();
    }

    @Override
    public void onViewAttachedToWindow(SmartViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        addAnimate(holder, holder.getLayoutPosition());
    }

    public void setOpenAnimationEnable(boolean enable) {
        this.mOpenAnimationEnable = enable;
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener listener) {
        mListener = listener;
    }

    public void replaceAll(final List<T> tList) {
        if (mList != null) {
            mList.clear();
        }
        if (tList != null) {
            mList.addAll(tList);
        }
        notifyDataSetChanged();
    }

    public void addAll(List<T> tList) {
        if (tList != null && mList != null) {
            mList.addAll(tList);
        }
        notifyDataSetChanged();
    }

    public void remove(int position) {
        mList.remove(position);
    }

//    public boolean hasStableIds() {
//        return false;
//    }

    public void registerDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.registerObserver(observer);
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.unregisterObserver(observer);
    }

    public void notifyListDataSetChanged() {
        mDataSetObservable.notifyChanged();
    }

    public void notifyDataSetInvalidated() {
        mDataSetObservable.notifyInvalidated();
    }

    public boolean areAllItemsEnabled() {
        return true;
    }

    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SmartViewHolder holder;
        if (convertView != null) {
            holder = (SmartViewHolder) convertView.getTag();
        } else {
            holder = onCreateViewHolder(parent, getItemViewType(position));
            convertView = holder.itemView;
            convertView.setTag(holder);
        }
        onBindViewHolder(holder, position);
        addAnimate(holder, position);
        return convertView;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    public boolean isEmpty() {
        return getCount() == 0;
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public int getCount() {
        return mList.size();
    }

}

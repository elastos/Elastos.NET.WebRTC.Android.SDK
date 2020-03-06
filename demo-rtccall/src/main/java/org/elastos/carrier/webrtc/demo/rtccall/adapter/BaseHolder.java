package org.elastos.carrier.webrtc.demo.rtccall.adapter;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;

import androidx.recyclerview.widget.RecyclerView;

public class BaseHolder extends RecyclerView.ViewHolder {
    //不写死控件变量，而采用Map方式
    private HashMap<Integer, View> mViews = new HashMap<>();

    public BaseHolder(View itemView) {
        super(itemView);
    }

    private TextView textView;

    /**
     * 获取控件的方法
     */
    public <T> T getView(Integer viewId) {
        //根据保存变量的类型 强转为该类型
        View view = mViews.get(viewId);
        if (view == null) {
            view = itemView.findViewById(viewId);
            //缓存
            mViews.put(viewId, view);
        }
        return (T) view;
    }

    /**
     * 传入文本控件id和设置的文本值，设置文本
     */
    public BaseHolder setText(final Integer viewId, String value) {
        textView = getView(viewId);
        if (value == null || "null".equals(value)) {
            value = "";
        }
        if (textView != null) {
            textView.setText(value);
        }
        return this;
    }

    public BaseHolder setTextColor(Integer viewId, int color) {
        textView = getView(viewId);
        textView.setTextColor(color);
        return this;
    }

    public BaseHolder setTextBackground(Drawable drawable) {
        textView.setBackgroundDrawable(drawable);
        return this;
    }

    /**
     * 隐藏控件.
     *
     * @param flag
     * @return
     */
    public BaseHolder setVisible(boolean flag) {
        if (flag) {
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setVisibility(View.GONE);
        }
        return this;
    }


    /**
     * 传入图片控件id和资源id，设置图片
     */
    public BaseHolder setImageResource(Integer viewId, Integer resId) {
        ImageView imageView = getView(viewId);
        if (imageView != null) {
            imageView.setBackgroundResource(resId);
        }
        return this;
    }
}


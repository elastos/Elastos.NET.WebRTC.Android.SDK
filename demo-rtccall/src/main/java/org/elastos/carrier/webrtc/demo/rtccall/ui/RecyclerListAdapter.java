package org.elastos.carrier.webrtc.demo.rtccall.ui;


import android.graphics.Color;

import org.elastos.carrier.webrtc.demo.rtccall.R;
import org.elastos.carrier.webrtc.demo.rtccall.adapter.BaseAdapter;
import org.elastos.carrier.webrtc.demo.rtccall.adapter.BaseHolder;
import org.elastos.carrier.webrtc.demo.rtccall.model.Friend;

import java.util.List;

import androidx.annotation.LayoutRes;

public class RecyclerListAdapter extends BaseAdapter<Friend> {

    public RecyclerListAdapter(List<Friend> friends, @LayoutRes int layoutId) {
        super(layoutId);

    }

    @Override
    protected void convert(final BaseHolder holder, final Friend item) {
        holder.setText(R.id.value_tv, item.getName());
        if (item.isOnline()) {
            holder.setTextColor(R.id.value_tv, Color.GREEN);
        } else {
            holder.setTextColor(R.id.value_tv, Color.RED);
        }

////        holder.setTextColor(R.id.fault_name_tv, Color.parseColor(item.getFaultTypeColor()));
//        if (!TextUtils.isEmpty(item.getCreateDate())) {
//            holder.setText(R.id.fault_date_tv, TimeTransformUtil.getShowLocalTime(item.getCreateDate()));//TimeTransformUtil.getShowLocalTime(item.getCreateDate()).substring(0, 10));
//        } else {
//            holder.setText(R.id.fault_date_tv, "");
//        }
//        holder.setText(R.id.fault_severity_tv, "等级：" + item.getSeverityTypeName());
//        holder.setText(R.id.person_tv, "处理人：" + item.getProcessingPersonName());
////        holder.setText(R.id.statu_iv, "( " + item.getNewState() + " )");
//        holder.setText(R.id.statu_tv, item.getStateName());
////        holder.setTextColor(R.id.statu_tv, Color.parseColor(getStateColor(item.getState())));
//        FaultRoundArImageView pic = holder.itemView.findViewById(R.id.fault_iv);
//        if (null == holder || item.getPathList() == null || item.getPathList().size() == 0) {
//            Glide.with(mContext).load(R.mipmap.ic_fault_picture_no).apply(options).into(pic);
//        } else {
//            Glide.with(mContext).load(item.getPathList().get(0)).apply(options).into(pic);
//        }
    }



}

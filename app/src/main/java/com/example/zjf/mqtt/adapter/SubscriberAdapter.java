package com.example.zjf.mqtt.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.zjf.mqtt.R;
import com.example.zjf.mqtt.bean.Message;

import java.util.List;

public class SubscriberAdapter extends RecyclerView.Adapter<SubscriberAdapter.MyHolder> {
    private static final String TAG = "SubscriberAdapter";
    private List<Message> messageList;
    private Context context;

    public SubscriberAdapter(List<Message> messageList,Context context){
        this.messageList = messageList;
        this.context = context;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_subscriber,parent,false);
        MyHolder myHolder = new MyHolder(view);
        return myHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        Message message = messageList.get(position);

        if (message != null) {
            if (message.isLeft){
                holder.rlLeft.setVisibility(View.VISIBLE);
                holder.rlRight.setVisibility(View.GONE);
                Log.d(TAG,"message.string === " + message.string);
                holder.tvLeft.setText(message.string);
            } else {
                holder.rlLeft.setVisibility(View.GONE);
                holder.rlRight.setVisibility(View.VISIBLE);
                Log.d(TAG,"MESSAGE.STRING === " + message.string);
                holder.tvRight.setText(message.string);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    class MyHolder extends RecyclerView.ViewHolder{
        TextView tvLeft;
        RelativeLayout rlLeft;
        TextView tvRight;
        RelativeLayout rlRight;
        public MyHolder(View itemView) {
            super(itemView);
            tvLeft = itemView.findViewById(R.id.tv_left);
            rlLeft = itemView.findViewById(R.id.rl_left);
            tvRight = itemView.findViewById(R.id.tv_right);
            rlRight = itemView.findViewById(R.id.rl_right);
        }
    }

    public void addListDate(Message message) {
        messageList.add(message);
        notifyDataSetChanged();
    }
}

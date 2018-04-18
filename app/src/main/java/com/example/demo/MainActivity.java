package com.example.demo;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //rv
        RecyclerView rv = findViewById(R.id.rv);
        rv.setAdapter(new RecyclerView.Adapter() {

            class ViewHolder extends RecyclerView.ViewHolder {
                ViewHolder(View itemView) {
                    super(itemView);
                }
            }

            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View v = ((Activity) parent.getContext()).getLayoutInflater().inflate(R.layout.item, parent, false);
                return new ViewHolder(v);
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

            }

            @Override
            public int getItemCount() {
                return 0;
            }
        });
        rv.setLayoutManager(new LinearLayoutManager(this));

        PullToRefreshLayout refreshLayout = findViewById(R.id.refresh_layout);

        //header

        View header = refreshLayout.setHeader(R.layout.header);

        TextView tv = header.findViewById(R.id.tv);
        ImageView iv = header.findViewById(R.id.iv);
        ProgressBar progressBar = header.findViewById(R.id.progress_bar);

        //自定义header中UI的变化

        refreshLayout.setListener(new PullToRefreshLayout.Listener() {
            @Override
            public void onChange() {
                tv.setText("释放刷新");

                //旋转箭头

                ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(iv, "rotation", 0f, -180f);
                objectAnimator.setDuration(500);
                objectAnimator.start();
            }

            @Override
            public void onRecover() {
                tv.setText("下拉刷新");

                //把箭头转回来

                ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(iv, "rotation", -180f, 0);
                objectAnimator.setDuration(500);
                objectAnimator.start();
            }

            @Override
            public void onRefresh() {
                tv.setText("刷新中");

                //复原ImageView的朝向成初始状态（被属性动画改变了）

                ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(iv, "rotation", -180f, 0);
                objectAnimator.setDuration(0);
                objectAnimator.start();

                //隐藏ImageView，显示圆形进度条控件

                iv.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFinish() {
                tv.setText("下拉刷新");

                //隐藏圆形进度条，显示ImageView

                iv.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onPull(float rate) {

            }
        });

        //remove refreshing state

        findViewById(R.id.bt).setOnClickListener(v -> refreshLayout.refreshFinished());
    }
}

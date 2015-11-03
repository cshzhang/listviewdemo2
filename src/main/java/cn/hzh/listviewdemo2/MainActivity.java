package cn.hzh.listviewdemo2;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cn.hzh.listviewdemo2.view.QQListView;

public class MainActivity extends AppCompatActivity
{
    private QQListView mQQListView;
    private BaseAdapter mAdapter;

    private List<String> mDatas = new ArrayList<String>();
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initDatas();

        mQQListView = (QQListView) findViewById(R.id.id_listview);
        mAdapter = new ArrayAdapter<String>(this,
                -1, mDatas)
        {
            @Override
            public View getView(int position, View convertView, ViewGroup parent)
            {
                if(convertView == null)
                {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.item, parent, false);
                }
                TextView tv = (TextView) convertView.findViewById(R.id.id_text);
                tv.setText(getItem(position));
                return convertView;
            }
        };
        mQQListView.setAdapter(mAdapter);

        mQQListView.setOnItemRightViewClickListener(new QQListView.OnItemRightViewClickListener()
        {
            @Override
            public void onItemRightViewClick(int position, View view)
            {
                Log.d("TAG", "remove item");
                mDatas.remove(position);
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    private void initDatas()
    {
        int i = 'A';
        for(; i <= 'Z'; i++)
        {
            mDatas.add((char)i + "");
        }
    }

}

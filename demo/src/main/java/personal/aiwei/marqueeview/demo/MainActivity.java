package personal.aiwei.marqueeview.demo;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

import personal.aiwei.marqueeview.MarqueeView;

public class MainActivity extends AppCompatActivity {

    private MarqueeView marqueeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        marqueeView = findViewById(R.id.marquee_view);
        marqueeView.setAdapter(new MAdapter(this));

        marqueeView.startPlay();
    }

    static class MAdapter extends BaseAdapter {

        private LayoutInflater layoutInflater;
        private List<String> datas;

        MAdapter(Context context) {
            layoutInflater = LayoutInflater.from(context);
            datas = Arrays.asList("喜欢两个人", "听说爱情回来过", "走在红毯那一天", "旧梦");
        }

        @Override
        public int getCount() {
            return datas.size();
        }

        @Override
        public Object getItem(int position) {
            return datas.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = layoutInflater.inflate(R.layout.item_list_m, parent, false);
                convertView.setTag(new ViewHolder(convertView));
            }

            ViewHolder viewHolder = (ViewHolder) convertView.getTag();
            viewHolder.vTitle.setText(datas.get(position));

            return convertView;
        }

        static class ViewHolder {
            TextView vTitle;

            ViewHolder(View itemView) {
                vTitle = itemView.findViewById(R.id.title);
            }
        }
    }

}

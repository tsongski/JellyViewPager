package com.tsongski.jellyviewpager.ui.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.tsongski.jellyviewpager.JellyViewPager;
import com.tsongski.jellyviewpager.R;
import com.tsongski.jellyviewpager.ui.fragment.JellyFragment;

public class MainActivity extends FragmentActivity implements View.OnClickListener{

    private JellyViewPager mPager;
    public static final int PAGER_COUNT = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initPager();
    }

    private void initPager() {
        mPager = (JellyViewPager) findViewById(R.id.pager);
        mPager.setAdapter(new MyAdapter(getSupportFragmentManager()));
        mPager.setCurrentItem(0);
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        findViewById(R.id.next).setOnClickListener(this);
        findViewById(R.id.pre).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.next:
                mPager.showPre();
                break;
            case R.id.pre:
                mPager.showNext();
                break;
        }
    }

    private class MyAdapter extends FragmentPagerAdapter {

        public MyAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return PAGER_COUNT;
        }

        @Override
        public Fragment getItem(int position) {

            return new JellyFragment();
        }
    }

}

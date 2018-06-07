package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    private Cursor mCursor;
    private long mStartId;

    private long mSelectedItemId;
    private int mSelectedItemUpButtonFloor = Integer.MAX_VALUE;
    private int mTopInset;
    private int selectedPosition;

    private MyPagerAdapter myPagerAdapter;
    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;
    private ImageView photoImage;
    private TextView titleTextView;
    private TextView subtitleTextView;
    private Bundle extras;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
        supportStartPostponedEnterTransition();

        extras = getIntent().getExtras();
        setContentView(R.layout.activity_article_detail);

        mPager = findViewById(R.id.pager);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(toolbar != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        titleTextView = findViewById(R.id.article_title);
        subtitleTextView = findViewById(R.id.article_byline);
        photoImage = findViewById(R.id.photo);

        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
                selectedPosition = extras.getInt(ArticleListActivity.SELECTED_POSITION, 0);

        }

        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged(int state) {
            }

            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                }
                selectedPosition = position;
                setUpFragmentAndActivity();
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }
        });

        Log.d("SELECTED POSITION", " " + selectedPosition);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;

        if(mCursor != null) {
            setUpFragmentAndActivity();

        }

        mPagerAdapter = new MyPagerAdapter(getFragmentManager(), mCursor);
        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(selectedPosition);


        /** mPagerAdapter.notifyDataSetChanged();

        // Select the start ID
        if (mStartId > 0) {
            mCursor.moveToFirst()
            while (!mCursor.isAfterLast()) {
                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                    final int position = mCursor.getPosition();
                    mPager.setCurrentItem(position, false);
                    break;
                }
                mCursor.moveToNext();
            }
            mStartId = 0;
        } */
    }

    private void setUpFragmentAndActivity() {
        mCursor.moveToPosition(selectedPosition);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String imageTransition = extras.getString(ArticleListActivity.TRANSITION_NAME);
            photoImage.setTransitionName(imageTransition);
        }

        Picasso.get()
                .load(mCursor.getString(ArticleLoader.Query.PHOTO_URL))
                .into(photoImage, new Callback() {
                    @Override
                    public void onSuccess() {
                        supportStartPostponedEnterTransition();
                    }

                    @Override
                    public void onError(Exception e) {
                        supportStartPostponedEnterTransition();
                    }
                });

        String title = mCursor.getString(ArticleLoader.Query.TITLE);
        titleTextView.setText(title);

        Date date = parsePublishedDate();
        if(date.before(START_OF_EPOCH.getTime())) {
            subtitleTextView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            date.getTime(),
                            System.currentTimeMillis(),
                            DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                    + " by " + mCursor.getString(ArticleLoader.Query.AUTHOR)
            ));
        } else {
            subtitleTextView.setText(Html.fromHtml(
                    outputFormat.format(date)
                    + " by " + mCursor.getString(ArticleLoader.Query.AUTHOR)
            ));
        }
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    /**public void onUpButtonFloorChanged(long itemId, ArticleDetailFragment fragment) {
        if (itemId == mSelectedItemId) {
            //updateUpButtonPosition();
        }
    }

    private void updateUpButtonPosition() {
        int upButtonNormalBottom = mTopInset + mUpButton.getHeight();
        mUpButton.setTranslationY(Math.min(mSelectedItemUpButtonFloor - upButtonNormalBottom, 0));
    } */

    private class MyPagerAdapter extends FragmentStatePagerAdapter {

        private WeakReference<Cursor> cursorWeakReference;

        public MyPagerAdapter(FragmentManager fm, Cursor cursor) {
            super(fm);
            cursorWeakReference = new WeakReference<>(cursor);
        }


        @Override
        public Fragment getItem(int position) {
            cursorWeakReference.get().moveToPosition(position);
            return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID), mCursor.getString(ArticleLoader.Query.BODY));
        }

        @Override
        public int getCount() {
            return cursorWeakReference.get().getCount();
        }
    }
}

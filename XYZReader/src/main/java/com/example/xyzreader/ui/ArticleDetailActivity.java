package com.example.xyzreader.ui;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.ImageView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {


    public static final String ARG_POSITION = "position";
    public static final String STATE_CURRENT_PAGE = "state_current_page";

    private Cursor mCursor;
    private long mStartId;

    private long mSelectedItemId;
    private int mSelectedItemUpButtonFloor = Integer.MAX_VALUE;
    private int mTopInset;

    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;
    private View mUpButtonContainer;
    private View mUpButton;


    private boolean mReturning;
    private int mCurrentPosition;
    private int mStartingPosition;
    private ArticleDetailFragment mCurrentFragment;

    private SharedElementCallback callBack = new SharedElementCallback() {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (mReturning) {
                Log.d(this.toString(), "Tag");
                Log.d(this.toString(), "Current Item No: " + mPager.getCurrentItem());
                ImageView sharedElement = (mPagerAdapter.getRegisteredFragment(mPager.getCurrentItem())).getPhotoView();
                if (sharedElement == null) {
                    sharedElements.clear();
                    names.clear();
                } else if (mStartingPosition != mCurrentPosition) {
                    if (sharedElement.getTransitionName() == null) {
                        sharedElement.setTransitionName("image_" + mCurrentPosition);
                    }
                    names.clear();
                    sharedElements.clear();
                    names.add(sharedElement.getTransitionName());
                    sharedElements.put(sharedElement.getTransitionName(), sharedElement);
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

        if (Utils.isLollipopOrUp()) {
            Transition t = TransitionInflater.from(this).inflateTransition(R.transition.change_image_transform);
            getWindow().setSharedElementEnterTransition(t);
            Slide s = new Slide(Gravity.BOTTOM);
            s.excludeTarget(android.R.id.statusBarBackground, true);
            s.excludeTarget(android.R.id.navigationBarBackground, true);
            s.excludeTarget(R.id.photo_container, true);
            s.excludeTarget(R.id.main_toolbar, true);
            Fade s2 = new Fade();
            TransitionSet transitionSet = new TransitionSet();
            transitionSet.addTransition(s).addTransition(s2);
            getWindow().setEnterTransition(transitionSet);
        }


        ActivityCompat.postponeEnterTransition(this);

        setContentView(R.layout.activity_article_detail);

        getLoaderManager().initLoader(0, null, this);
        if (Utils.isLollipopOrUp()) {
            setEnterSharedElementCallback(callBack);
        }

        mStartingPosition = getIntent().getExtras().getInt(ARG_POSITION);
        if (savedInstanceState == null) {
            mCurrentPosition = mStartingPosition;
        } else {
            mCurrentPosition = savedInstanceState.getInt(STATE_CURRENT_PAGE);
        }

        mPagerAdapter = new MyPagerAdapter(getFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);
        mPager.setPageMargin((int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));

        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                mUpButton.animate()
                        .alpha((state == ViewPager.SCROLL_STATE_IDLE) ? 1f : 0f)
                        .setDuration(300);
            }

            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                }
                mSelectedItemId = mCursor.getLong(ArticleLoader.Query._ID);
                updateUpButtonPosition();
            }
        });

        mUpButtonContainer = findViewById(R.id.up_container);

        mUpButton = findViewById(R.id.action_up);
        mUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //  onSupportNavigateUp();
                supportFinishAfterTransition();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mUpButtonContainer.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                    view.onApplyWindowInsets(windowInsets);
                    mTopInset = windowInsets.getSystemWindowInsetTop();
                    mUpButtonContainer.setTranslationY(mTopInset);
                    updateUpButtonPosition();
                    return windowInsets;
                }
            });
        }

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
                mSelectedItemId = mStartId;
            }
        }


    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_PAGE, mCurrentPosition);
    }

    @Override
    public void finishAfterTransition() {
        mReturning = true;
        Intent data = new Intent();
        data.putExtra(ArticleListActivity.EXTRA_OLD_ITEM_POSITION, mStartingPosition);
        data.putExtra(ArticleListActivity.EXTRA_NEW_ITEM_POSITION, mCurrentPosition);
        setResult(RESULT_OK, data);
        super.finishAfterTransition();
    }


    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();

        // Select the start ID
        if (mStartId > 0) {
            mCursor.moveToFirst();
            // TODO: optimize
            while (!mCursor.isAfterLast()) {
                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                    final int position = mCursor.getPosition();
                    mPager.setCurrentItem(position, false);
                    break;
                }
                mCursor.moveToNext();
            }
            mStartId = 0;
        }


    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    public void onUpButtonFloorChanged(long itemId, ArticleDetailFragment fragment) {
        if (itemId == mSelectedItemId) {
            mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
            updateUpButtonPosition();
        }
    }

    private void updateUpButtonPosition() {
        int upButtonNormalBottom = mTopInset + mUpButton.getHeight();
        mUpButton.setTranslationY(Math.min(mSelectedItemUpButtonFloor - upButtonNormalBottom, 0));
    }

    public long getSelectedItemId() {
        return mSelectedItemId;
    }


    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        SparseArray<WeakReference<Fragment>> registeredFragments = new SparseArray<>();

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            ArticleDetailFragment fragment = (ArticleDetailFragment) object;
            if (fragment != null) {
                mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
                updateUpButtonPosition();
            }
        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            mCurrentFragment = ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID));
            return mCurrentFragment;
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            WeakReference<Fragment> f = new WeakReference<Fragment>((Fragment) super.instantiateItem(container, position));
            registeredFragments.put(position, f);
            return f.get();
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            registeredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        public ArticleDetailFragment getRegisteredFragment(int position) {
            if (registeredFragments.get(position) != null) {
                return (ArticleDetailFragment) registeredFragments.get(position).get();
            }
            return null;
        }
    }
}

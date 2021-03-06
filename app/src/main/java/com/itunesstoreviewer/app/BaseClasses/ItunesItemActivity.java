package com.itunesstoreviewer.app.BaseClasses;

import android.app.ActionBar;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.*;
import android.widget.SearchView;
import android.widget.ShareActionProvider;
import com.google.gson.Gson;
import com.itunesstoreviewer.app.*;
import com.itunesstoreviewer.app.ItunesRssItemClasses.Deserializer;
import com.itunesstoreviewer.app.SlidingTabs.SlidingTabsColorsFragment;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Darien on 4/29/2015.
 */
public abstract class ItunesItemActivity extends FragmentActivity implements ItunesItemListFragment.Callbacks {
    protected final String USER_PREFS_FAV = "favorites";
    protected final String SAVED_ITEM = "saved_item";
    protected final Gson gson = Deserializer.buildGson();
    protected ActionBar actionBar;
    protected String mAppName;
    protected String mTitle;
    protected boolean mTwoPane;
    protected ShareActionProvider mShareActionProvider;
    protected ItunesItem itunesItem;
    protected ConnectivityManager connectivityManager;
    protected Drawable unfavorite;
    protected Drawable favorite;
    protected MenuItem mFavButton;
    protected CategoryAttribute categoryAttribute;
    private MenuItem searchMenuItem;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionBar = getActionBar();
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mTwoPane = getResources().getBoolean(R.bool.has_two_panes);
        mAppName = getResources().getString(R.string.app_name);
        mTitle = mAppName;
        favorite = getResources().getDrawable(R.drawable.ic_action_favorite_pink);
        unfavorite = getResources().getDrawable(R.drawable.ic_action_favorite);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mTwoPane) {
            getMenuInflater().inflate(R.menu.share_menu, menu);
            getMenuInflater().inflate(R.menu.details_menu, menu);

            MenuItem mItem = menu.findItem(R.id.menu_item_share);
            mFavButton = menu.findItem(R.id.fav_button);

            mShareActionProvider = (ShareActionProvider) mItem.getActionProvider();
            if (itunesItem != null) {
                if (ItunesAppController.userFavorites.contains(itunesItem)) {
                    mFavButton.setIcon(favorite);
                } else {
                    mFavButton.setIcon(unfavorite);
                }
            }
        }
        getMenuInflater().inflate(R.menu.search_menu, menu);
        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(true);
        //searchView.setQueryRefinementEnabled(true);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        searchMenuItem = menu.findItem(R.id.menu_search);
        if (searchMenuItem != null) {
            SearchView searchView = (SearchView) searchMenuItem.getActionView();
            searchView.setQuery("", false);
            if (mTwoPane) {
                boolean showMenuItems = hasNetworkConnection() && itunesItem != null;
                menu.findItem(R.id.menu_item_share).setVisible(showMenuItems);
                menu.findItem(R.id.fav_button).setVisible(showMenuItems);
                menu.findItem(R.id.play_store_button).setVisible(showMenuItems);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onStop() {
        super.onStop();
        List<String> favSet = new ArrayList<String>(ItunesAppController.userFavorites.size());
        for (ItunesItem e : ItunesAppController.userFavorites) {
            favSet.add(gson.toJson(e));
        }
        JSONArray jsonArray = new JSONArray(favSet);
        System.out.println("ALL FAVS: " + favSet.size() + " ----- " + jsonArray.toString());
        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(USER_PREFS_FAV, jsonArray.toString());
        editor.apply();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.play_store_button:
                launchPlayStoreSearch();
                break;
            case R.id.fav_button:
                handleFavorite(item);
                break;
            case R.id.refresh:
                refresh();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (itunesItem != null) {
            // Serialize and persist the itunes item.
            //TODO Do I still need to do this?
            outState.putSerializable(SAVED_ITEM, itunesItem);
        }
    }

    @Override
    public void onItunesItemSelected(ItunesItem item) {
        if (mTwoPane) {
            mTitle = item.getItemName();
            actionBar.setTitle(mTitle);
            //TODO Improve toggle/favoriting
            // http://stackoverflow.com/questions/11499574/toggle-button-using-two-image-on-different-state
            if (itunesItem == null) toggleFavorite(item);
            else toggleFavorite(itunesItem, item);
            itunesItem = item;
            categoryAttribute = ItunesAppController.getCategoryAttribute(itunesItem);
            setShareIntent();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.itunesitem_detail_container, ItunesItemDetailFragment.newInstance(itunesItem))
                    .commit();
        } else {
            // In single-pane mode, simply start the detail activity for the selected item.
            Intent detailIntent = new Intent(this, ItunesItemDetailActivity.class);
            detailIntent.putExtra(ItunesItemDetailFragment.ARG_ITEM_ID, item);
            startActivity(detailIntent);
        }
        invalidateOptionsMenu();
    }

    @Override
    public void networkError() {
        //TODO Call this from ItunesItemListFragment at some point
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.itunesitem_list, new ErrorFragment());
        transaction.commit();
    }

    protected void refresh() {
        onNewIntent(getIntent());
    }

    protected void launchPlayStoreSearch() {
        String query = itunesItem.getItemName() + " " + itunesItem.getArtistName();
        String searchCategory = categoryAttribute.getPlayStoreKey();
        startActivity(new Intent(Intent.ACTION_VIEW
                , Uri.parse("https://play.google.com/store/search?q=" + query + "&c=" + searchCategory)));
    }

    public void retryButtonClick(View v) {
        //TODO Move this to button inline call
        if (hasNetworkConnection()) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            SlidingTabsColorsFragment slidingTabsColorsFragment = new SlidingTabsColorsFragment();
            transaction.replace(R.id.itunesitem_list, slidingTabsColorsFragment);
            transaction.commit();
        } else {
            networkError();
        }
    }

    protected void handleFavorite(MenuItem item) {
        if (ItunesAppController.userFavorites.contains(itunesItem)) {
            ItunesAppController.userFavorites.remove(itunesItem);
            item.setIcon(unfavorite);
        } else {
            ItunesAppController.userFavorites.add(itunesItem);
            item.setIcon(favorite);
        }
    }

    private void toggleFavorite(ItunesItem e1, ItunesItem e2) {
        List<ItunesItem> userFavorites = ItunesAppController.userFavorites;
        if (userFavorites.contains(e1) != userFavorites.contains(e2)) {
            toggleFavorite(e2);
        }
    }

    private void toggleFavorite(ItunesItem e1) {
        if (ItunesAppController.userFavorites.contains(e1)) {
            mFavButton.setIcon(favorite);
        } else {
            mFavButton.setIcon(unfavorite);
        }
    }

    protected boolean hasNetworkConnection() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        invalidateOptionsMenu();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    // Call to update the share intent
    protected void setShareIntent() {
        Intent shareIntent = new Intent();
        String name = itunesItem.getItemName();
        String artist = itunesItem.getArtistName();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Checkout this content: " + name + "\n");
        shareIntent.putExtra(Intent.EXTRA_TEXT, name + " by " + artist +
                "\n" + itunesItem.getItemUrl() +
                "\n\nprovided by " + mAppName + " created by @darienurse");
        shareIntent.setType("text/plain");
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

    static public class ErrorFragment extends Fragment {
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.error_layout, container, false);
        }
    }


}

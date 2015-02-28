package com.appleappstorestop25.app;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ShareActionProvider;
import com.appleappstorestop25.app.ItunesItemClasses.*;

import java.util.ArrayList;
import java.util.List;


/**
 * An activity representing a list of ItunesItems. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link ItunesItemDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link ItunesItemListFragment} and the item details
 * (if present) is a {@link ItunesItemDetailFragment}.
 * <p>
 * This activity also implements the required
 * {@link ItunesItemListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class ItunesItemListActivity extends Activity
        implements ItunesItemListFragment.Callbacks {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    public static List<ItunesItem> itemsList;
    private ShareActionProvider mShareActionProvider;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if(itemsList==null) {
            new RequestItemsServiceTask().execute();
        }
        else {
            setContentView(R.layout.activity_itunesitem_list);
            if (findViewById(R.id.itunesitem_detail_container) != null) {
                mTwoPane = true;

                // In two-pane mode, list items should be given the
                // 'activated' state when touched.
                ((ItunesItemListFragment) getFragmentManager()
                        .findFragmentById(R.id.itunesitem_list))
                        .setActivateOnItemClick(true);
            }
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(mTwoPane) {
            // Inflate share_menu resource file.
            getMenuInflater().inflate(R.menu.share_menu, menu);

            // Locate MenuItem with ShareActionProvider
            MenuItem mItem = menu.findItem(R.id.menu_item_share);

            // Fetch and store ShareActionProvider
            mShareActionProvider = (ShareActionProvider) mItem.getActionProvider();

            // Return true to display share_menu
            return true;
        }
        else
            return super.onCreateOptionsMenu(menu);
    }

    // Call to update the share intent
    private void setShareIntent(Intent shareIntent) {
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }



    /**
     * Callback method from {@link ItunesItemListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(Integer id) {
        Log.d("DEBUG", "" + id);

        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putInt(ItunesItemDetailFragment.ARG_ITEM_ID, id);
            ItunesItemDetailFragment fragment = new ItunesItemDetailFragment();
            fragment.setArguments(arguments);
            getFragmentManager().beginTransaction()
                    .replace(R.id.itunesitem_detail_container, fragment)
                    .commit();
            if (arguments.containsKey(ItunesItemDetailFragment.ARG_ITEM_ID)) {
                ItunesItem itunesItem = ItunesItemListActivity.itemsList.get(arguments.getInt(ItunesItemDetailFragment.ARG_ITEM_ID));
                setShareIntent(itunesItem.generateShareIntent());
            }
        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, ItunesItemDetailActivity.class);
            detailIntent.putExtra(ItunesItemDetailFragment.ARG_ITEM_ID, id);
            startActivity(detailIntent);
        }
    }

    /**
     * populate list in background while showing progress dialog.
     */
    private class RequestItemsServiceTask
            extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog =
                new ProgressDialog(ItunesItemListActivity.this);

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Please wait..");
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... unused) {
            ItemService itemService = new ItemService();
            try {
                itemsList = itemService.findAllItems();
            } catch (Throwable e) {

            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            /*intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);*/
            if (dialog.isShowing()) {
                dialog.dismiss();
                setContentView(R.layout.activity_itunesitem_list);
                if (findViewById(R.id.itunesitem_detail_container) != null) {
                    mTwoPane = true;

                    // In two-pane mode, list items should be given the
                    // 'activated' state when touched.
                    ((ItunesItemListFragment) getFragmentManager()
                            .findFragmentById(R.id.itunesitem_list))
                            .setActivateOnItemClick(true);
                }
            }
            //finish();
        }
    }
}

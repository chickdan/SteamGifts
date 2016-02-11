package net.mabako.steamgifts.fragments;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.mabako.steamgifts.adapters.GiveawayAdapter;
import net.mabako.steamgifts.adapters.IEndlessAdaptable;
import net.mabako.steamgifts.core.R;
import net.mabako.steamgifts.data.Giveaway;
import net.mabako.steamgifts.fragments.interfaces.IActivityTitle;
import net.mabako.steamgifts.fragments.interfaces.IHasEnterableGiveaways;
import net.mabako.steamgifts.fragments.profile.LoadEnteredGameListTask;
import net.mabako.steamgifts.fragments.profile.ProfileGiveaway;
import net.mabako.steamgifts.fragments.util.GiveawayListFragmentStack;
import net.mabako.steamgifts.persistentdata.SavedGiveaways;
import net.mabako.steamgifts.tasks.EnterLeaveGiveawayTask;

import java.io.Serializable;
import java.util.List;

/**
 * Show a list of saved giveaways.
 */
// TODO implements IHasEnterableGiveaways?
public class SavedGiveawaysFragment extends ListFragment<GiveawayAdapter> implements IActivityTitle, IHasEnterableGiveaways {
    private static final String TAG = SavedGiveawaysFragment.class.getSimpleName();

    private SavedGiveaways savedGiveaways;

    private LoadEnteredGameListTask enteredGameListTask;
    private EnterLeaveGiveawayTask enterLeaveTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter.setFragmentValues(getActivity(), this, savedGiveaways);

        GiveawayListFragmentStack.addFragment(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        if (savedInstanceState == null) {
            enteredGameListTask = new LoadEnteredGameListTask(this, 1);
            enteredGameListTask.execute();
        }

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        savedGiveaways = new SavedGiveaways(getContext());
    }

    @Override
    public void onDetach() {
        super.onDetach();


        if (savedGiveaways != null) {
            savedGiveaways.close();
            savedGiveaways = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        GiveawayListFragmentStack.removeFragment(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (enteredGameListTask != null) {
            enteredGameListTask.cancel(true);
            enteredGameListTask = null;
        }

        if (enterLeaveTask != null) {
            enterLeaveTask.cancel(true);
            enterLeaveTask = null;
        }
    }

    @Override
    protected void refresh() {
        super.refresh();

        if (enteredGameListTask != null) {
            enteredGameListTask.cancel(true);
        }
        enteredGameListTask = new LoadEnteredGameListTask(this, 1);
    }


    @NonNull
    @Override
    protected GiveawayAdapter createAdapter() {
        return new GiveawayAdapter(-1, false, PreferenceManager.getDefaultSharedPreferences(getContext()));
    }

    @Override
    protected AsyncTask<Void, Void, ?> getFetchItemsTask(int page) {
        return null;
    }

    @Override
    protected Serializable getType() {
        return null;
    }

    @Override
    protected void fetchItems(int page) {
        if (page != 1)
            return;

        super.addItems(savedGiveaways.all(), true);
        adapter.reachedTheEnd();
    }

    @Override
    public int getTitleResource() {
        return R.string.saved_giveaways_title;
    }

    @Override
    public String getExtraTitle() {
        return null;
    }

    public void onRemoveSavedGiveaway(String giveawayId) {
        adapter.removeGiveaway(giveawayId);
    }

    /**
     * Callback for {@link #enteredGameListTask}
     * <p>Note: do NOT call this from within this class.</p>
     */
    @Override
    public void addItems(List<? extends IEndlessAdaptable> items, boolean clearExistingItems) {
        Log.d(TAG, "Fetched some " + items.size() + " entries");

        // closed or not deleted
        boolean foundAnyClosedGiveaways = false;

        // do nothing much except update the status of existing giveaways.
        for (IEndlessAdaptable endlessAdaptable : items) {
            ProfileGiveaway giveaway = (ProfileGiveaway) endlessAdaptable;
            Log.d(TAG, giveaway.getGiveawayId() + " ~> " + giveaway.isOpen() + ", " + giveaway.isDeleted());
            if (!giveaway.isOpen() && !giveaway.isDeleted()) {
                foundAnyClosedGiveaways = true;
                break;
            }

            Giveaway existingGiveaway = adapter.findItem(giveaway.getGiveawayId());
            if (existingGiveaway != null) {
                existingGiveaway.setEntries(giveaway.getEntries());
                existingGiveaway.setEntered(true);
                adapter.notifyItemChanged(existingGiveaway);
            }
        }

        // have we found any non-closed giveaways?
        if (foundAnyClosedGiveaways) {
            enteredGameListTask = null;
        } else {
            enteredGameListTask = new LoadEnteredGameListTask(this, enteredGameListTask.getPage() + 1);
            enteredGameListTask.execute();
        }
    }

    @Override
    public void requestEnterLeave(String giveawayId, String enterOrDelete, String xsrfToken) {
        if (enterLeaveTask != null)
            enterLeaveTask.cancel(true);

        enterLeaveTask = new EnterLeaveGiveawayTask(this, getContext(), giveawayId, xsrfToken, enterOrDelete);
        enterLeaveTask.execute();
    }

    @Override
    public void onEnterLeaveResult(String giveawayId, String what, Boolean success, boolean propagate) {
        if (success == Boolean.TRUE) {
            Giveaway giveaway = adapter.findItem(giveawayId);
            if (giveaway != null) {
                giveaway.setEntered(GiveawayDetailFragment.ENTRY_INSERT.equals(what));
                adapter.notifyItemChanged(giveaway);
            }
        } else {
            Log.e(TAG, "Probably an error catching the result...");
        }

        if (propagate)
            GiveawayListFragmentStack.onEnterLeaveResult(giveawayId, what, success);
    }
}

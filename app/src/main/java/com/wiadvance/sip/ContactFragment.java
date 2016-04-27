package com.wiadvance.sip;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.gson.Gson;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.wiadvance.sip.db.FavoriteContactTableHelper;
import com.wiadvance.sip.linphone.LinphoneCoreHelper;
import com.wiadvance.sip.linphone.LinphoneSipManager;
import com.wiadvance.sip.office365.AuthenticationManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreListenerBase;
import org.linphone.core.LinphoneProxyConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class ContactFragment extends Fragment {

    private static final String TAG = "ContactFragment";
    public static boolean is_fab_open = false;

    private LinphoneSipManager mWiSipManager;
    private DrawerItemAdapter mDrawerAdapter;
    private LinphoneCoreListenerBase mLinPhoneListener;

    public static ContactFragment newInstance() {

        Bundle args = new Bundle();
        ContactFragment fragment = new ContactFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWiSipManager = new LinphoneSipManager(getContext());

        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_sip, container, false);

        setupNavigationDrawer(rootView);
        setupFloatingActionButton(rootView);
        setupViewPager(rootView);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        try {
            LinphoneCore lc = LinphoneCoreHelper.getLinphoneCoreInstance(getContext());
            mLinPhoneListener = new LinphoneCoreListenerBase() {
                @Override
                public void registrationState(LinphoneCore lc, LinphoneProxyConfig cfg, LinphoneCore.RegistrationState state, final String message) {

                    if (state.equals(LinphoneCore.RegistrationState.RegistrationOk)) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (mDrawerAdapter != null) {
                                    mDrawerAdapter.notifyDataSetChanged();
                                }
                            }
                        });
                    }

                    if (state.equals(LinphoneCore.RegistrationState.RegistrationFailed)) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (mDrawerAdapter != null) {
                                    mDrawerAdapter.notifyDataSetChanged();
                                }
                                NotificationUtil.displayStatus(getContext(), "Sip registration error: " + message);
                            }
                        });
                    }
                }
            };
            lc.addListener(mLinPhoneListener);

        } catch (LinphoneCoreException e) {
            e.printStackTrace();
        }

        getSipAccounts();

    }

    @Override
    public void onStop() {
        super.onStop();

        try {
            LinphoneCore lc = LinphoneCoreHelper.getLinphoneCoreInstance(getContext());
            lc.removeListener(mLinPhoneListener);

        } catch (LinphoneCoreException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_sip, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.search_icon:
                Intent intent = SearchActivity.newIntent(getContext());
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setupViewPager(View rootView) {
        ViewPager viewPager = (ViewPager) rootView.findViewById(R.id.contacts_view_pager);
        viewPager.setAdapter(new ContactPagerAdapter(getFragmentManager()));

        TabLayout tabLayout = (TabLayout) rootView.findViewById(R.id.contacts_tab_layout);
        tabLayout.setupWithViewPager(viewPager);

        if (FavoriteContactTableHelper.getInstance(getContext()).getAll().size() == 0) {
            TabLayout.Tab tab = tabLayout.getTabAt(2); // go to phone contact
            if (tab != null) {
                tab.select();
            }
        }
    }

    private void setupFloatingActionButton(View rootView) {
        final FloatingActionButton fab_open = (FloatingActionButton) rootView.findViewById(R.id.fab_open);
        final FloatingActionButton addContactFab = (FloatingActionButton) rootView.findViewById(R.id.add_contact_fab);
        final FloatingActionButton scanAddContactFab = (FloatingActionButton) rootView.findViewById(R.id.scan_contact_fab);

        fab_open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!is_fab_open) {
                    ObjectAnimator rotateAnimator = ObjectAnimator.ofFloat(fab_open, "rotation", 0f, -45f);
                    rotateAnimator.setDuration(600);
                    rotateAnimator.start();


                    viewVisibilityDelayed(addContactFab, View.VISIBLE, 50);
                    viewVisibilityDelayed(scanAddContactFab, View.VISIBLE, 100);

                    is_fab_open = true;
                } else {
                    ObjectAnimator rotateAnimator = ObjectAnimator.ofFloat(fab_open, "rotation", -45f, 0f);
                    rotateAnimator.setDuration(600);
                    rotateAnimator.start();

                    viewVisibilityDelayed(scanAddContactFab, View.GONE, 50);
                    viewVisibilityDelayed(addContactFab, View.GONE, 100);

                    is_fab_open = false;
                }
            }
        });

        addContactFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = AddContactActivity.newIntent(getContext());
                startActivity(intent);
            }
        });

//        scanAddContactFab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                Intent intent = ScanActivity.newIntent(getContext());
//                startActivity(intent);
//            }
//        });
    }

    private void setupNavigationDrawer(View rootView) {
        List<DrawerItem> items = new ArrayList<>();

        items.add(new DrawerItem("Header", R.drawable.ic_info_outline_black_24dp));
        items.add(new DrawerItem("History", R.drawable.ic_history_black_24dp));
        items.add(new DrawerItem("Information", R.drawable.ic_info_outline_black_24dp));
        items.add(new DrawerItem("Logout", R.drawable.ic_exit_to_app_black_24dp));

        mDrawerAdapter = new DrawerItemAdapter(getContext(), items);

        Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        DrawerLayout drawerLayout = (DrawerLayout) rootView.findViewById(R.id.drawer_layout);
        ListView drawerList = (ListView) rootView.findViewById(R.id.left_drawer);

        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                getActivity(),
                (DrawerLayout) rootView.findViewById(R.id.drawer_layout),
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }
        };

        drawerToggle.syncState();
        drawerLayout.setDrawerListener(drawerToggle);

        drawerList.setAdapter(mDrawerAdapter);
        drawerList.setBackgroundColor(getResources().getColor(R.color.beige));
        drawerList.setOnItemClickListener(mDrawerItemClickListener);
    }


    private void getSipAccounts() {
        OkHttpClient client = new OkHttpClient();
        OkHttpClient clientWith60sTimeout = client.newBuilder()
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        FormBody body = new FormBody.Builder()
                .add("email", UserData.getEmail(getContext()))
                .add("access_token", AuthenticationManager.getInstance().getAccessToken())
                .build();

        Request request = new Request.Builder()
                .url(BuildConfig.HTTPS_SIP_API_SERVER)
                .post(body)
                .build();

        clientWith60sTimeout.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "onFailure() called with: " + "call = [" + call + "], e = [" + e + "]");
                NotificationUtil.displayStatus(getContext(),
                        "Backend error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                Log.d(TAG, "onResponse() called with: " + "call = [" + call + "], response = [" + response + "]");

                if (!response.isSuccessful()) {
                    NotificationUtil.displayStatus(getContext(),
                            "Backend error: " + response.body().string());
                    return;
                }

                String rawBodyString = response.body().string();

                SipApiResponse sip_data = new Gson().fromJson(rawBodyString, SipApiResponse.class);

                String sip_domain = sip_data.proxy_address + ":" + sip_data.proxy_port;
                if (getContext() != null) {
                    UserData.setSip(getContext(), sip_data.sip_account);

                    for (SipApiResponse.SipAccount acc : sip_data.sip_list) {
                        UserData.sEmailToSipBiMap.forcePut(acc.email, acc.sip_account);
                        UserData.sEmailToPhoneBiMap.forcePut(acc.email, acc.phone);
                    }

                    mWiSipManager.register(sip_data.sip_account, sip_data.sip_password, sip_domain);
                }
            }
        });
    }

    private void logout() {

        String sipNumber = UserData.getSip(getContext());
        if (sipNumber != null) {
            mWiSipManager.unregister(sipNumber);
        }

        UserData.clean(getContext());

        MixpanelAPI mixpanel = MixpanelAPI.getInstance(getContext(), BuildConfig.MIXPANL_TOKEN);
        JSONObject props = new JSONObject();
        try {
            props.put("SIP_NUMBER", LinphoneCoreHelper.getSipNumber());
            props.put("INIT_TIME", LinphoneCoreHelper.getInitTime());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mixpanel.track("LOGOUT", props);

        LinphoneCoreHelper.destroyLinphoneCore(getContext());

        AuthenticationManager.getInstance().setContextActivity(getActivity());
        AuthenticationManager.getInstance().disconnect();

        // Back to login activity
        Intent intent = LoginActivity.newIntent(getContext());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }


    private void viewVisibilityDelayed(final View view, final int visibility, long delayMillis){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                view.setVisibility(visibility);
            }
        }, delayMillis);
    }

    private AdapterView.OnItemClickListener mDrawerItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            switch (position) {
                case 1:
                    Intent intent = CallLogActivity.newIntent(getContext());
                    startActivity(intent);
                    break;
                case 2:
                    int versionCode = BuildConfig.VERSION_CODE;
                    String versionName = BuildConfig.VERSION_NAME;

                    final String message = "Version: " + versionName + "." + versionCode + "\n"
                            + "Sip Number: " + UserData.getSip(getContext()) + "\n"
                            + "Email: " + UserData.getEmail(getContext()) + "\n";

                    AlertDialog.Builder builder = new AlertDialog.Builder(
                            getContext()).setTitle("Information")
                            .setMessage(message)
                            .setPositiveButton("ok", null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    break;
                case 3:

                    AlertDialog.Builder logoutBuilder = new AlertDialog.Builder(getContext())
                            .setTitle("Logout")
                            .setMessage("Are you sure you want to logout?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    logout();
                                }
                            })
                            .setNegativeButton("No", null);
                    logoutBuilder.create().show();
                    break;
            }
        }
    };
}

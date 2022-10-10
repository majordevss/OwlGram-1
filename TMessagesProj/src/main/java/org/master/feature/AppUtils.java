package org.master.feature;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.widget.LinearLayout;

import androidx.annotation.RequiresApi;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Components.LayoutHelper;

import java.net.IDN;

public class AppUtils {

    public  static  void showGhostAlert(Activity activity, BaseFragment baseFragment, ActionBarMenuItem ghostModeItem) {
        if (activity == null) {
            return;
        }
        BottomSheet.Builder builder = new BottomSheet.Builder(activity);
        CheckBoxCell[] cells;
        builder.setTitle("Ghost Mode Setting");

        cells = new CheckBoxCell[3];
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        for (int a = 0; a < 3; a++) {
            cells[a] = new CheckBoxCell(activity, 1);
            cells[a].setBackgroundDrawable(Theme.getSelectorDrawable(false));
            cells[a].setTag(a);
            if (a == 0) {
                cells[a].setText("Ghost Mode", "", SharedAppConfig.isGhostModeEnabled, false);
            }else if(a == 1) {
                cells[a].setText("Hide Typing Status", "", SharedAppConfig.hideTypingStatus, false);
            } else {
                cells[a].setText("Don't Mark Message as Read", "", SharedAppConfig.DoNotMarkMessageAsRead, false);
            }
            cells[a].setPadding(LocaleController.isRTL ? AndroidUtilities.dp(16) : AndroidUtilities.dp(8), 0, LocaleController.isRTL ? AndroidUtilities.dp(8) : AndroidUtilities.dp(16), 0);
            linearLayout.addView(cells[a], LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            cells[a].setOnClickListener(v -> {
                Integer num = (Integer) v.getTag();
                if(num == 0){
                    SharedAppConfig.isGhostModeEnabled = !SharedAppConfig.isGhostModeEnabled;
                    cells[num].setChecked(SharedAppConfig.isGhostModeEnabled, true);

                }else if(num == 1){
                    SharedAppConfig.hideTypingStatus = !SharedAppConfig.hideTypingStatus;
                    cells[num].setChecked(SharedAppConfig.hideTypingStatus, true);

                }else if(num == 2){
                    SharedAppConfig.DoNotMarkMessageAsRead = !SharedAppConfig.DoNotMarkMessageAsRead;
                    cells[num].setChecked(SharedAppConfig.DoNotMarkMessageAsRead, true);

                }
                SharedAppConfig.saveConfig();
                ghostModeItem.setAlpha(SharedAppConfig.isGhostModeEnabled?1.0f:0.3f);

            });
        }
//        builder.setVi(12);
        builder.setCustomView(linearLayout);

//        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        BottomSheet dialog = builder.create();
        baseFragment.showDialog(dialog);
//        TextView button = (TextView) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
//        if (button != null) {
//            button.setTextColor(Theme.getColor(Theme.key_dialogTextRed2));
//        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void scheduleJob(Context context) {
//        Log.i("channeladder","scheduleJob started");
//        ComponentName serviceComponent = new ComponentName(context, MyJobService.class);
//        JobInfo.Builder builder = new JobInfo.Builder(101, serviceComponent);
//        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
//// wait at least
//        builder.setPeriodic(60 *1000);
//        builder.setRequiresCharging(false); // we don't care if the device is charging or notn
//
//        JobScheduler scheduler = (JobScheduler) ApplicationLoader.applicationContext.getSystemService(JOB_SCHEDULER_SERVICE);
//        scheduler.schedule(builder.build());
//        Log.i("channeladder","scheduleJob ended");

    }


    public static SharedConfig.ProxyInfo getProxyInfo(String link) {
        if (link == null) {
            return null;
        }
        try {

            Uri data = Uri.parse(link);
            if (data != null) {
                String user = null;
                String password = null;
                String port = null;
                String address = null;
                String secret = null;
                String scheme = data.getScheme();
                if (scheme != null) {
                    if ((scheme.equals("http") || scheme.equals("https"))) {
                        String host = data.getHost().toLowerCase();
                        if (host.equals("telegram.me") || host.equals("t.me") || host.equals("telegram.dog")) {
                            String path = data.getPath();
                            if (path != null) {
                                if (path.startsWith("/socks") || path.startsWith("/proxy")) {
                                    address = data.getQueryParameter("server");
                                    if (AndroidUtilities.checkHostForPunycode(address)) {
                                        address = IDN.toASCII(address, IDN.ALLOW_UNASSIGNED);
                                    }
                                    port = data.getQueryParameter("port");
                                    user = data.getQueryParameter("user");
                                    password = data.getQueryParameter("pass");
                                    secret = data.getQueryParameter("secret");
                                }
                            }
                        }
                    } else if (scheme.equals("tg")) {
                        String url = data.toString();
                        if (url.startsWith("tg:proxy") || url.startsWith("tg://proxy") || url.startsWith("tg:socks") || url.startsWith("tg://socks")) {
                            url = url.replace("tg:proxy", "tg://telegram.org").replace("tg://proxy", "tg://telegram.org").replace("tg://socks", "tg://telegram.org").replace("tg:socks", "tg://telegram.org");
                            data = Uri.parse(url);
                            address = data.getQueryParameter("server");
                            if (AndroidUtilities.checkHostForPunycode(address)) {
                                address = IDN.toASCII(address, IDN.ALLOW_UNASSIGNED);
                            }
                            port = data.getQueryParameter("port");
                            user = data.getQueryParameter("user");
                            password = data.getQueryParameter("pass");
                            secret = data.getQueryParameter("secret");
                        }
                    }
                }
                if (!TextUtils.isEmpty(address) && !TextUtils.isEmpty(port)) {
                    if (user == null) {
                        user = "";
                    }
                    if (password == null) {
                        password = "";
                    }
                    if (secret == null) {
                        secret = "";
                    }
                    if (port != null) {
                        return new SharedConfig.ProxyInfo(address,Integer.parseInt(port),user,password,secret);
                    }

                }
            }
        } catch (Exception ignore) {

        }
        return null;
    }


}

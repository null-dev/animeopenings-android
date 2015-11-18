package gq.nulldev.animeopenings.app.util;

/**
 * Project: AnimeOpenings
 * Created: 03/10/15
 * Author: nulldev
 */

import android.app.Activity;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.amazon.device.ads.*;
//import com.mopub.mobileads.MoPubErrorCode;
//import com.mopub.mobileads.MoPubView;
import com.startapp.android.publish.banner.Banner;
import com.startapp.android.publish.banner.BannerListener;

public class AdPreference extends Preference implements AdListener, /*MoPubView.BannerAdListener,*/ BannerListener {

    public AdPreference(Context context, AttributeSet attrs, int defStyle) {super    (context, attrs, defStyle);}
    public AdPreference(Context context, AttributeSet attrs) {super(context, attrs);}
    public AdPreference(Context context) {super(context);}

    AdLayout amazonAdView;
//    AdView adView;
//    MoPubView mpView;
    Banner saBanner;
    boolean enableAmazonAdView;
    View view;

    @Override
    protected View onCreateView(ViewGroup parent) {
        // this will create the linear layout defined in ads_layout.xml
        view = super.onCreateView(parent);

        // the context is a PreferenceActivity
        Activity activity = (Activity)getContext();

        Log.i("AnimeOpenings", "Loading ads...");
        //Create amazon adview
        AdRegistration.setAppKey("8bfebb2aafce4e718bda4bfb8055c588");
        amazonAdView = new AdLayout(activity, com.amazon.device.ads.AdSize.SIZE_AUTO);
        amazonAdView.setListener(this);

        //Create MoPub view
//        mpView = new MoPubView(activity);
//        mpView.setAdUnitId("f7b7778b555c40ab9379b29f7126149d");
//        mpView.setBannerAdListener(this);
        saBanner = new Banner(activity);
        saBanner.setBannerListener(this);

        // Create the admob adView
//        adView = new AdView(activity);
//        adView.setAdSize(com.google.android.gms.ads.AdSize.BANNER);
//        adView.setAdUnitId(MainActivity.SETTINGS_AD_UNIT);

        enableAmazonAdView = true;
        ((LinearLayout)view).addView(amazonAdView);
        amazonAdView.loadAd();

        return view;
    }

    @Override
    protected void onPrepareForRemoval() {
        super.onPrepareForRemoval();
//        mpView.destroy();
    }

    @Override
    public void onAdLoaded(Ad ad, AdProperties adProperties) {
        Log.i("AnimeOpenings", "Amazon ad loaded!");
        if(!enableAmazonAdView) {
            enableAmazonAdView = true;
//            ((LinearLayout)view).removeView(adView);
//            ((LinearLayout)view).removeView(mpView);
            ((LinearLayout)view).removeView(saBanner);
            ((LinearLayout)view).addView(amazonAdView);
        }
    }

    @Override
    public void onAdFailedToLoad(Ad ad, AdError adError) {

        Log.w("AnimeOpenings", "Failed to load Amazon ad!");
        Log.w("AnimeOpenings", adError.getMessage());

        if(enableAmazonAdView) {
            enableAmazonAdView = false;
            ((LinearLayout)view).removeView(amazonAdView);
//            ((LinearLayout)view).addView(adView);
//            ((LinearLayout)view).addView(mpView);
            ((LinearLayout)view).addView(saBanner);
        }

        // Initiate a generic request to load it with an ad
//        AdRequest request = new AdRequest.Builder().build();
//        adView.loadAd(request);
//        mpView.loadAd();
    }

    @Override
    public void onAdExpanded(Ad ad) {

    }

    @Override
    public void onAdCollapsed(Ad ad) {

    }

    @Override
    public void onAdDismissed(Ad ad) {

    }

    /*@Override
    public void onBannerLoaded(MoPubView banner) {
        Log.i("AnimeOpenings", "MoPub banner loaded!");
        Log.i("AnimeOpenings", "AdKeywords: " + banner.getKeywords());
    }

    @Override
    public void onBannerFailed(MoPubView banner, MoPubErrorCode errorCode) {
        Log.w("AnimeOpenings", "Failed to load MoPub banner ad (EC: " + errorCode.name() + ")");
    }

    @Override
    public void onBannerClicked(MoPubView banner) {

    }

    @Override
    public void onBannerExpanded(MoPubView banner) {

    }

    @Override
    public void onBannerCollapsed(MoPubView banner) {

    }*/

    @Override
    public void onReceiveAd(View view) {
        Log.i("AnimeOpenings", "Loaded StartApp ad!");
    }

    @Override
    public void onFailedToReceiveAd(View view) {
        Log.w("AnimeOpenings", "Failed to load StartApp ad!");
    }

    @Override
    public void onClick(View view) {

    }
}

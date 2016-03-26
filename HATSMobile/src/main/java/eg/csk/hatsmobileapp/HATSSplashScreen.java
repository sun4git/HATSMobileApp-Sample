package eg.csk.hatsmobileapp;

import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.ImageView;

/*
Author: Suneel
 */

public class HATSSplashScreen extends Activity{
    public static boolean done = false;
    ImageView img;
    Object lock = new Object();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_layout);

        img = (ImageView)findViewById(R.id.splashImage);
        img.setBackgroundResource(R.drawable.logo);

        new PrefetchData().execute();
		//comment above line and comment below to disable n/w and network & app check
		//----
//		if(!HATSMain.changedFlag) HATSMain.changedFlag = true; //to force checks on main activity
//		Intent i = new Intent(getApplicationContext(), HATSMain.class);
//		startActivity(i);
//        finish();
		//----
    }
    

    @Override
    public void onActivityResult(int request, int result, Intent intent){
    	if(!HATSMain.changedFlag) HATSMain.changedFlag = true; //to force checks in main activity
    	Intent i = new Intent(getApplicationContext(), HATSMain.class);
    	startActivity(i);
    	finish();
    }
   
    @Override
    public void onConfigurationChanged(Configuration newConfig){
    	//do nothing here - to take care of screen orientation changes
    	//when screen orientation is changed, this will be called as we added 
    	//configChanges parameter in manifest xml
    	super.onConfigurationChanged(newConfig);
    }
    
    private class PrefetchData extends AsyncTask<Void, Void, Void> {
    	
    	private boolean isNetworkAvailable() {
    	    ConnectivityManager connectivityManager 
    	          = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    	    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    	}
    	@SuppressWarnings("deprecation")
    	private void showNetworkErrorDialog(){

    		synchronized (lock){
    			AlertDialog alertDialog = new AlertDialog.Builder(HATSSplashScreen.this).create();

    			alertDialog.setTitle(getResources().getString(R.string.network_error_title));
    			alertDialog.setMessage(getResources().getString(R.string.network_error_text));
    			alertDialog.setButton(getResources().getString(R.string.ok_button_text), new DialogInterface.OnClickListener() {
    				public void onClick(DialogInterface dialog, int which) {

    					finish();
    					System.exit(0);
    				}
    			});
    			alertDialog.setCancelable(false);
    			alertDialog.show();
    		}
    	}
    	
    	private boolean isAppServerAvailable(){
    		try {
    			URL url = new URL(buildURLFromSettings());
    			HttpURLConnection urlc;
    			urlc = (HttpURLConnection) url.openConnection();
    			urlc.setConnectTimeout(5000); // mTimeout is in seconds
    			urlc.connect();
    			if (urlc.getResponseCode() == 200) {
    				return true;
    			} else {
    				return false;
    			}
    		} catch (Exception e) {
    			e.printStackTrace();
    			return false;
    		}
    	}
    	
    	private String buildURLFromSettings(){
    		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(HATSSplashScreen.this);
    		StringBuffer sb = new StringBuffer();
    		sb.append("http://");
    		sb.append(prefs.getString("appserver_port", "hilive.demos.ibm.com:80").trim()+"/");
    		sb.append(prefs.getString("hatsapp_name", "DemoApp").trim()+"/index.jsp?");
    		sb.append("host="+prefs.getString("host_addr", "iseriesd.demos.ibm.com").trim()+"&");
    		sb.append("port="+prefs.getString("host_port", "23").trim()+"&");
    		if("1".equalsIgnoreCase(prefs.getString("host_type", "3").trim()))
    			sb.append("sessionType=1&TNEnhanced=false");
    		else if("2".equalsIgnoreCase(prefs.getString("host_type", "3").trim()))
    			sb.append("sessionType=1&TNEnhanced=true");
    		else if("3".equalsIgnoreCase(prefs.getString("host_type", "3").trim()))
    			sb.append("sessionType=2");
    		return sb.toString();
    	}
    	
    	private void showServerErrorDialog(){

    		synchronized (lock){
    			AlertDialog.Builder alertDialog = new AlertDialog.Builder(HATSSplashScreen.this);

    			alertDialog.setTitle(getResources().getString(R.string.app_error_title));

    			alertDialog.setMessage(getResources().getString(R.string.app_error_text));


    			alertDialog.setPositiveButton(getResources().getString(R.string.settings_button_text), new DialogInterface.OnClickListener() {
    				public void onClick(DialogInterface dialog,int which) {
    					//disconnected = true;
    					showHATSSettings();
    				}
    			});

    			alertDialog.setNegativeButton(getResources().getString(R.string.close_button_text), new DialogInterface.OnClickListener() {
    				public void onClick(DialogInterface dialog, int which) {
    					dialog.cancel();
    					finish();
    					System.exit(0);
    				}
    			});
    			alertDialog.setCancelable(false);
    			alertDialog.show();
    		}
    	}
    	
        private void showHATSSettings(/*View view*/){
    		synchronized (lock){
    			Intent intent = new Intent(HATSSplashScreen.this, HATSSettings.class);
    			startActivityForResult(intent, 0);//(intent);
    			lock.notify();
    		}
    	}
        
    	
    	 
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // before making http calls         
 
        }
 
        @Override
        protected Void doInBackground(Void... arg0) {
        	CookieSyncManager.createInstance(getApplicationContext());
        	CookieManager.getInstance().removeAllCookie();
        	
        	if(!isNetworkAvailable()){
        		HATSSplashScreen.this.runOnUiThread(new Runnable(){
        			public void run(){
        				showNetworkErrorDialog();
        				return;
        			}
        		});
        	}

        	else if(!isAppServerAvailable()){ 
        		HATSSplashScreen.this.runOnUiThread(new Runnable(){
        			public void run(){
        				showServerErrorDialog();
        				//return;
        			}
        		});
        	}

        	else {
        		Intent i = new Intent(HATSSplashScreen.this, HATSMain.class);
        		startActivity(i);
        		done = true;
        		return null;
        	}
        	
        	synchronized (lock){
        		try {
        			lock.wait();
        		} catch (InterruptedException e) {
        			e.printStackTrace();
        		}
        		return null;
        	}
        }
 
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // close this activity
            if(done)
            	finish();
        }
    }
}

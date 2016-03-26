package eg.csk.hatsmobileapp;

import java.util.Properties;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
//import android.util.Log;
//import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebSettings.ZoomDensity;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/*
Author: Suneel
 */

public class HATSMain extends Activity {
	WebView webView;
	ProgressBar progressBar;
	Spinner keypadSpinner;
	Button keypadButton;
	TextView textArea;
	WebSettings webSettings;
	static boolean changedFlag = true;
	static boolean firstTime = true;
	static boolean restarting = false;//to decide if we have to show keypad 
	boolean disconnected = false;
	boolean keypadVisible = false;
	boolean keypadDragged = false;
    boolean searchingError = false;
	static String currentURL;
	static String colors[][] = {
		{"0","default"},
		{"1","blue"},
		{"2","brown"},
		{"3","cyan"},
		{"4","darkblue"},
		{"5","darkbrown"},
		{"6","lime"},
		{"7","gray"},
		{"8","orange"},
		{"9","pink"},
		{"10","red"},
		{"11","white"},
		{"12","yellow"}
		};
	static String hostKeyPad[][] = {
		{"PF1","[pf1]"},
		{"PF2","[pf2]"},
		{"PF3","[pf3]"},
		{"PF4","[pf4]"},
		{"PF5","[pf5]"},
		{"PF6","[pf6]"},
		{"PF7","[pf7]"},
		{"PF8","[pf8]"},
		{"PF9","[pf9]"},
		{"PF10","[pf10]"},
		{"PF11","[pf11]"},
		{"PF12","[pf12]"},
		{"PF13","[pf13]"},
		{"PF14","[pf14]"},
		{"PF15","[pf15]"},
		{"PF16","[pf16]"},
		{"PF17","[pf17]"},
		{"PF18","[pf18]"},
		{"PF19","[pf19]"},
		{"PF20","[pf20]"},
		{"PF21","[pf21]"},
		{"PF22","[pf22]"},
		{"PF23","[pf23]"},
		{"PF24","[pf24]"},

		{"ENTER","[enter]"},
		{"CLEAR","[clear]"},
		{"SYSREQ","[sysreq]"},
		{"ATTN","[attn]"},

		{"PAGEUP","[pageup]"},
		{"PAGEDN","[pagedn]"},

		{"HELP","[help]"},
		{"PRINT","[printhost]"},
		{"PA1","[pa1]"},
		{"PA2","[pa2]"},
		{"PA3","[pa3]"},
		{"AltView","[altview]"},
		{"Reset","[reset]"},
		{"Field exit","[fldext]"},
		{"Field plus","[field+]"},
		{"Field minus","[field-]"}
	};
	//static boolean changeOrientationOnly = false;
	Properties colorProps = new Properties();
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	@SuppressLint({ "SetJavaScriptEnabled", "NewApi", "JavascriptInterface" })
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(android.os.Build.VERSION.SDK_INT < 14 || ViewConfiguration.get(this).hasPermanentMenuKey())
			requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.activity_hatsmain);
		HATSSplashScreen.done = true;//since its loaded anyways, mark splash screen as complete just to make sure.
		if(firstTime)
			Toast.makeText(getApplicationContext(), getResources().getString(R.string.start_app_text), Toast.LENGTH_LONG).show();

		else if(changedFlag)
			runOnUiThread(new Runnable() {
		        @Override
		        public void run() {
		        	Toast.makeText(getApplicationContext(), getResources().getString(R.string.restart_app_text), Toast.LENGTH_LONG).show();
		        }
		    });

		if(webView == null){
			webView = (WebView) findViewById(R.id.webView1);
		}
		progressBar = (ProgressBar) findViewById(R.id.progressBar1);

		keypadSpinner = (Spinner) findViewById(R.id.keypad_spinner);

		keypadButton = (Button) findViewById(R.id.keypad_button);
		keypadButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if(!keypadDragged)
					sendHostKey(keypadSpinner.getSelectedItemPosition());
				else
					keypadDragged = false;
			}
		});

		webView.setVisibility(View.GONE);
		webView.setFindListener(new WebView.FindListener(){
			@Override
			public void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting) {
				if (numberOfMatches > 0) {
					webView.setVisibility(View.GONE);
					disconnected = true;
					setCanRestart(true);
				}
                else if(!searchingError){
                    searchingError = true;
                    webView.findAllAsync(getResources().getString(R.string.browser_find_error_text));
                }
				else {
                    if(searchingError)
                        searchingError = false;
					webView.setVisibility(View.VISIBLE);
					disconnected = false;
					setCanRestart(false);
				}
			}
		});
		setKeypadDisplay(false);

		//workaround to avoid NetworkOnMainThreadException //implement isNetworkAvailable and isAppServerAvailable methods to a different Thread
		if (android.os.Build.VERSION.SDK_INT > 9) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}
		if((!HATSSplashScreen.done || changedFlag || restarting) && !isNetworkAvailable()){
			showNetworkErrorDialog();
			return;
		}

		if((!HATSSplashScreen.done || changedFlag || restarting) && !isAppServerAvailable()){
			showServerErrorDialog();
			return;
		}
		if(!HATSSplashScreen.done || changedFlag || restarting){ //firstime this check is done in splash screen
			CookieSyncManager.createInstance(getApplicationContext());
			CookieManager.getInstance().removeAllCookie();
		}

		webSettings = webView.getSettings();
		webView.setWebViewClient(new MyWebViewClient());
		webSettings.setJavaScriptEnabled(true);
		//webView.addJavascriptInterface(new CustomJavaScriptInterface(), "EXTRACT_HTML_DATA");
		webSettings.setSavePassword(false);
		webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
		webSettings.setAppCacheEnabled(false);

		if(firstTime || changedFlag){
			currentURL = buildURLFromSettings();
			changedFlag=false;
		}
		loadColorRemap();
		webView.loadUrl(currentURL);
		setOrientation();
		disconnected = false;
		webView.setVisibility(View.VISIBLE);
	}

	private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap bmp){
        	progressBar.setVisibility(View.VISIBLE);
        	super.onPageStarted(view,url,bmp);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String url) {
        	try {
                view.stopLoading();
            } catch (Exception e) {
            }
            try {
                view.clearView();
                view.loadData("", "text/html", "UTF-8");
            } catch (Exception e) {
            }
        	AlertDialog.Builder alertDialog = new AlertDialog.Builder(HATSMain.this);
    		alertDialog.setTitle(getResources().getString(R.string.network_error_title));
    		alertDialog.setMessage(getResources().getString(R.string.network_error_2_text) + "\n"+getResources().getString(R.string.error_code_text)+errorCode);
					//alertDialog.setMessage(""+num);
					alertDialog.setPositiveButton(getResources().getString(R.string.restart_button_text), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(getApplicationContext(), getResources().getString(R.string.restart_conn_text), Toast.LENGTH_LONG).show();
								}
							});
							restarting = true;
							disconnected = true;
							Intent intent = getIntent();
							finish();
							startActivity(intent);
						}
					});
    		alertDialog.setNegativeButton(getResources().getString(R.string.close_button_text), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					closeApp();
				}
			});
			alertDialog.setNeutralButton(getResources().getString(R.string.settings_button_text), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					disconnected = true;
					restarting = true;
					showHATSSettings();
				}
			});

    		alertDialog.setCancelable(false);

    		alertDialog.show();
    		super.onReceivedError(view, errorCode, description, url);
        }
        @SuppressWarnings("deprecation")
		@Override
        public void onPageFinished(WebView view, String url){
         	//int num = view.findAll("you may restart the application by clicking restart.");//depricated above API16(v4.0) and returning 0 always
        	if(android.os.Build.VERSION.SDK_INT > 15) {
				view.findAllAsync(getResources().getString(R.string.browser_find_restart_text));
			}
			else{
				int numberOfMatches = view.findAll(getResources().getString(R.string.browser_find_restart_text));
				if (numberOfMatches > 0) {
					webView.setVisibility(View.GONE);
					disconnected = true;
					setCanRestart(true);
				}
				else {
					numberOfMatches = view.findAll(getResources().getString(R.string.browser_find_error_text));
					if (numberOfMatches > 0) {
						webView.setVisibility(View.GONE);
						disconnected = true;
						setCanRestart(true);
					}
					else {
						webView.setVisibility(View.VISIBLE);
						disconnected = false;
						setCanRestart(false);
					}
				}
			}

			if(!disconnected)
        		processColorRemap();

			if(firstTime || restarting){
				firstTime = false;
				restarting = false;
				setKeypadDisplay(true);
			}

			super.onPageFinished(view, url);
			progressBar.setVisibility(View.GONE);
        }
    }

	private void showDisconnectedDialogIfNeeded(){
		if(canRestart/*disconnected && !restarting*//*num > 0*/){
    		webView.clearMatches();
    		webView.loadData("", "text/html", "UTF-8");
			setKeypadDisplay(false);
    	}
    	AlertDialog.Builder alertDialog = new AlertDialog.Builder(HATSMain.this);
		alertDialog.setTitle(getResources().getString(R.string.disconnect_title));
		alertDialog.setMessage(getResources().getString(R.string.connection_ended_text));

		alertDialog.setPositiveButton(getResources().getString(R.string.restart_button_text), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getApplicationContext(), getResources().getString(R.string.restart_conn_text), Toast.LENGTH_LONG).show();
					}
				});
				restarting = true;
				Intent intent = getIntent();
				finish();
				startActivity(intent);

			}
		});
		alertDialog.setNegativeButton(getResources().getString(R.string.close_button_text), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				closeApp();
			}
		});
		alertDialog.setNeutralButton(getResources().getString(R.string.settings_button_text), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				disconnected = true;
				restarting = true;
				showHATSSettings();
			}
		});
		alertDialog.setCancelable(false);
		if(canRestart/*disconnected && !restarting*//*num > 0*/){
			canRestart = false;
			disconnected = false;
    		alertDialog.show();
    		return;
		}
	}

	private boolean isNetworkAvailable() {
	    ConnectivityManager connectivityManager
	          = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}

	@SuppressWarnings("deprecation")
	private void showNetworkErrorDialog(){
		AlertDialog alertDialog = new AlertDialog.Builder(HATSMain.this).create();

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

	private boolean isAppServerAvailable(){
		return true;
        //hats app on the server receives a request. comment above return statement and uncomment below lines only if required
//		try {
//			URL url = new URL(buildURLFromSettings());
//			HttpURLConnection urlc;
//			urlc = (HttpURLConnection) url.openConnection();
//			urlc.setConnectTimeout(5000); // mTimeout is in seconds
//			urlc.connect();
//			if (urlc.getResponseCode() == 200) {
//				urlc.disconnect();//suneel
//				return true;
//			} else {
//				return false;
//			}
//		} catch (Exception e) {
//			return false;
//		}
	}

	private void showServerErrorDialog(){
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(HATSMain.this);

		alertDialog.setTitle(getResources().getString(R.string.app_error_title));

		alertDialog.setMessage(getResources().getString(R.string.app_error_text));


		alertDialog.setPositiveButton(getResources().getString(R.string.settings_button_text), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int which) {
				disconnected = true;
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

	private void showExitDialog(){
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(HATSMain.this);

		alertDialog.setTitle(getResources().getString(R.string.close_app_title));
		alertDialog.setMessage(getResources().getString(R.string.close_app_text));
				alertDialog.setPositiveButton(getResources().getString(R.string.yes_button_text), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        webView.loadUrl("javascript:ms('disconnect','HATSForm');");
                        finish();
                        System.exit(0);
                    }
                });

		alertDialog.setNegativeButton(getResources().getString(R.string.no_button_text), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
		alertDialog.setCancelable(false);
		alertDialog.show();
	}
	private void setOrientation(){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean optimize = prefs.getBoolean("optimize_checkbox", true/*false*/);
		if(optimize){//(!optimize)
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}
		else{
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR/*SCREEN_ORIENTATION_PORTRAIT*/);
		}
		//make above settings common for both now
		webView.setInitialScale(0);
		webSettings.setLoadWithOverviewMode(true);
		if(webView.getUrl().toLowerCase().contains("DemoApp".toLowerCase()))//
			webSettings.setUseWideViewPort(true);
		else
			webSettings.setUseWideViewPort(false);//true//this is to fit any hats web app //can give a setting based on the requirement
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setSupportZoom(true);
        webSettings.setDefaultZoom(ZoomDensity.FAR);
	}
	private String buildURLFromSettings(){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
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
		//sb.append("sessionType="+prefs.getString("host_type", "2").trim());
		return sb.toString();
		//System.out.println(s);
	}
	private void showHATSSettings(/*View view*/){
		Intent intent = new Intent(this, HATSSettings.class);
		startActivityForResult(intent, 0);//(intent);
	}

	private void sendHostKey(int keyID){
		webView.loadUrl("javascript:ms('"+hostKeyPad[keyID][1]+"', 'HATSForm')");
	}
	
	private void loadColorRemap(){
		boolean refresh = false;
		colorProps.clear();
		String genString1 = "javascript:(function() { var inputs = document.all; for(var i = 0; i < inputs.length; i++) {";
		String genString2 = " } } return; }).call(this);";
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		StringBuffer tempSB = new StringBuffer();
		boolean tempBool1 = false;
		boolean tempBool2 = false;
		int tempBg = 0;
		int tempFg = 0;
		boolean enableColors = prefs.getBoolean("enable_color_remap", false);
		if(enableColors){
			if(!prefs.getString("green_field_background", "0").trim().equalsIgnoreCase("0")){
				tempBg = Integer.parseInt(prefs.getString("green_field_background", "0").trim());
				tempBool1=true;
			}
			else {
				tempBg = 0;
				tempBool1=false;
				refresh = true;
			}
			if(!prefs.getString("green_field_foreground", "0").trim().equalsIgnoreCase("0")){
				tempFg = Integer.parseInt(prefs.getString("green_field_foreground", "0").trim());
				tempBool2=true;
			}
			else {
				tempFg = 0;
				tempBool2 = false;
				refresh = true;
			}
			if(tempBool1 || tempBool2){
				tempSB.append(genString1);
				tempSB.append("if(inputs[i].className.indexOf('HGREEN') == 0){");// || inputs[i].className.indexOf('HATSLINK') == 0){");
				if(tempBool1)
					tempSB.append("inputs[i].style.backgroundColor='"+colors[tempBg][1]+"';");

				if(tempBool2)
					tempSB.append("inputs[i].style.color='"+colors[tempFg][1]+"';");

				tempSB.append("} if(inputs[i].className.indexOf('HATSLINK') == 0){");
				if(tempBool1)
					tempSB.append("inputs[i].parentNode.parentNode.parentNode.parentNode.parentNode.style.backgroundColor='"+colors[tempBg][1]+"';");//(a->td->tr>table->td->tr)
				if(tempBool2)
					tempSB.append("inputs[i].style.color='"+colors[tempFg][1]+"';");

				tempSB.append(genString2);

				colorProps.put("green", tempSB.toString());
				tempSB.delete(0, tempSB.length());
			}

			//green white red cyan blue pink magenta yellow

			if(!prefs.getString("white_field_background", "0").trim().equalsIgnoreCase("0")){
				tempBg = Integer.parseInt(prefs.getString("white_field_background", "0").trim());
				tempBool1=true;
			}
			else {
				tempBg = 0;
				tempBool1=false;
				refresh = true;
			}
			if(!prefs.getString("white_field_foreground", "0").trim().equalsIgnoreCase("0")){
				tempFg = Integer.parseInt(prefs.getString("white_field_foreground", "0").trim());
				tempBool2=true;
			}
			else {
				tempFg = 0;
				tempBool2 = false;
				refresh = true;
			}
			if(tempBool1 || tempBool2){
				tempSB.append(genString1);
				tempSB.append("if(inputs[i].className.indexOf('HWHITE') == 0){");
				if(tempBool1)
					tempSB.append("inputs[i].style.backgroundColor='"+colors[tempBg][1]+"';");
				if(tempBool2)
					tempSB.append("inputs[i].style.color='"+colors[tempFg][1]+"';");
				tempSB.append(genString2);
				colorProps.put("white", tempSB.toString());
				tempSB.delete(0, tempSB.length());
			}

			if(!prefs.getString("red_field_background", "0").trim().equalsIgnoreCase("0")){
				tempBg = Integer.parseInt(prefs.getString("red_field_background", "0").trim());
				tempBool1=true;
			}
			else {
				tempBg = 0;
				tempBool1=false;
				refresh = true;
			}
			if(!prefs.getString("red_field_foreground", "0").trim().equalsIgnoreCase("0")){
				tempFg = Integer.parseInt(prefs.getString("red_field_foreground", "0").trim());
				tempBool2=true;
			}
			else {
				tempFg = 0;
				tempBool2 = false;
				refresh = true;
			}
			if(tempBool1 || tempBool2){
				tempSB.append(genString1);
				tempSB.append("if(inputs[i].className.indexOf('HRED') == 0){");
				if(tempBool1)
					tempSB.append("inputs[i].style.backgroundColor='"+colors[tempBg][1]+"';");
				if(tempBool2)
					tempSB.append("inputs[i].style.color='"+colors[tempFg][1]+"';");
				tempSB.append(genString2);
				colorProps.put("red", tempSB.toString());
				tempSB.delete(0, tempSB.length());
			}

			if(!prefs.getString("cyan_field_background", "0").trim().equalsIgnoreCase("0")){
				tempBg = Integer.parseInt(prefs.getString("cyan_field_background", "0").trim());
				tempBool1=true;
			}
			else {
				tempBg = 0;
				tempBool1=false;
				refresh = true;
			}
			if(!prefs.getString("cyan_field_foreground", "0").trim().equalsIgnoreCase("0")){
				tempFg = Integer.parseInt(prefs.getString("cyan_field_foreground", "0").trim());
				tempBool2=true;
			}
			else {
				tempFg = 0;
				tempBool2 = false;
				refresh = true;
			}
			if(tempBool1 || tempBool2){
				tempSB.append(genString1);
				tempSB.append("if(inputs[i].className.indexOf('HCYAN') == 0){");
				if(tempBool1)
					tempSB.append("inputs[i].style.backgroundColor='"+colors[tempBg][1]+"';");
				if(tempBool2)
					tempSB.append("inputs[i].style.color='"+colors[tempFg][1]+"';");
				tempSB.append(genString2);
				colorProps.put("cyan", tempSB.toString());
				tempSB.delete(0, tempSB.length());
			}

			if(!prefs.getString("magenta_field_background", "0").trim().equalsIgnoreCase("0")){
				tempBg = Integer.parseInt(prefs.getString("magenta_field_background", "0").trim());
				tempBool1=true;
			}
			else {
				tempBg = 0;
				tempBool1=false;
				refresh = true;
			}
			if(!prefs.getString("magenta_field_foreground", "0").trim().equalsIgnoreCase("0")){
				tempFg = Integer.parseInt(prefs.getString("magenta_field_foreground", "0").trim());
				tempBool2=true;
			}
			else {
				tempFg = 0;
				tempBool2 = false;
				refresh = true;
			}
			if(tempBool1 || tempBool2){
				tempSB.append(genString1);
				tempSB.append("if(inputs[i].className.indexOf('HMAGENTA') == 0){");
				if(tempBool1)
					tempSB.append("inputs[i].style.backgroundColor='"+colors[tempBg][1]+"';");
				if(tempBool2)
					tempSB.append("inputs[i].style.color='"+colors[tempFg][1]+"';");
				tempSB.append(genString2);
				colorProps.put("magenta", tempSB.toString());
				tempSB.delete(0, tempSB.length());
			}


			if(!prefs.getString("yellow_field_background", "0").trim().equalsIgnoreCase("0")){
				tempBg = Integer.parseInt(prefs.getString("yellow_field_background", "0").trim());
				tempBool1=true;
			}
			else {
				tempBg = 0;
				tempBool1=false;
				refresh = true;
			}
			if(!prefs.getString("yellow_field_foreground", "0").trim().equalsIgnoreCase("0")){
				tempFg = Integer.parseInt(prefs.getString("yellow_field_foreground", "0").trim());
				tempBool2=true;
			}
			else {
				tempFg = 0;
				tempBool2 = false;
				refresh = true;
			}
			if(tempBool1 || tempBool2){
				tempSB.append(genString1);
				tempSB.append("if(inputs[i].className.indexOf('HLYELLOW') == 0){");
				if(tempBool1)
					tempSB.append("inputs[i].style.backgroundColor='"+colors[tempBg][1]+"';");
				if(tempBool2)
					tempSB.append("inputs[i].style.color='"+colors[tempFg][1]+"';");
				tempSB.append(genString2);
				colorProps.put("yellow", tempSB.toString());
				tempSB.delete(0, tempSB.length());
			}

			if(!prefs.getString("pink_field_background", "0").trim().equalsIgnoreCase("0")){
				tempBg = Integer.parseInt(prefs.getString("pink_field_background", "0").trim());
				tempBool1=true;
			}
			else {
				tempBg = 0;
				tempBool1=false;
				refresh = true;
			}
			if(!prefs.getString("pink_field_foreground", "0").trim().equalsIgnoreCase("0")){
				tempFg = Integer.parseInt(prefs.getString("pink_field_foreground", "0").trim());
				tempBool2=true;
			}
			else {
				tempFg = 0;
				tempBool2 = false;
				refresh = true;
			}
			if(tempBool1 || tempBool2){
				tempSB.append(genString1);
				tempSB.append("if(inputs[i].className.indexOf('HLRED') == 0){");
				if(tempBool1)
					tempSB.append("inputs[i].style.backgroundColor='"+colors[tempBg][1]+"';");
				if(tempBool2)
					tempSB.append("inputs[i].style.color='"+colors[tempFg][1]+"';");
				tempSB.append(genString2);
				colorProps.put("pink", tempSB.toString());
				tempSB.delete(0, tempSB.length());
			}

			if(!prefs.getString("blue_field_background", "0").trim().equalsIgnoreCase("0")){
				tempBg = Integer.parseInt(prefs.getString("blue_field_background", "0").trim());
				tempBool1=true;
			}
			else {
				tempBg = 0;
				tempBool1=false;
				refresh = true;
			}
			if(!prefs.getString("blue_field_foreground", "0").trim().equalsIgnoreCase("0")){
				tempFg = Integer.parseInt(prefs.getString("blue_field_foreground", "0").trim());
				tempBool2=true;
			}
			else {
				tempFg = 0;
				tempBool2 = false;
				refresh = true;
			}
			if(tempBool1 || tempBool2){
				tempSB.append(genString1);
				tempSB.append("if(inputs[i].className.indexOf('HBLUE') == 0){");
				if(tempBool1)
					tempSB.append("inputs[i].style.backgroundColor='"+colors[tempBg][1]+"';");
				if(tempBool2)
					tempSB.append("inputs[i].style.color='"+colors[tempFg][1]+"';");
				tempSB.append(genString2);
				colorProps.put("blue", tempSB.toString());
				tempSB.delete(0, tempSB.length());
			}

			//screen_background

			if(!prefs.getString("screen_background", "0").trim().equalsIgnoreCase("0")){
				tempBg = Integer.parseInt(prefs.getString("screen_background", "0").trim());
				tempBool1=true;
			}
			else {
				tempBg = 0;
				tempBool1=false;
				refresh = true;
			}
			if(tempBool1){
				tempSB.append("javascript:void(document.body.style.backgroundColor='"+colors[tempBg][1]+"');");
				colorProps.put("screenbg", tempSB.toString());
				tempSB.delete(0, tempSB.length());

				tempSB.append("javascript:(function() { var inputs = document.getElementsByTagName('input'); for(var i = 0; i < inputs.length; i++) {if(inputs[i].type.indexOf('text') == 0 || inputs[i].type.indexOf('password') == 0) { inputs[i].style.backgroundColor='"+colors[tempBg][1]+"'; } } return; }).call(this);");
				colorProps.put("inputbg", tempSB.toString());
				tempSB.delete(0, tempSB.length());
			}
		}
		else{
			refresh = true;
			colorProps.clear();
		}

		if(refresh) webView.loadUrl("javascript:ms('refresh','HATSForm');");
	}

	private void processColorRemap(){
		if(colorProps.containsKey("green"))
			webView.loadUrl(colorProps.getProperty("green"));
		if(colorProps.containsKey("white"))
			webView.loadUrl(colorProps.getProperty("white"));
		if(colorProps.containsKey("red"))
			webView.loadUrl(colorProps.getProperty("red"));
		if(colorProps.containsKey("cyan"))
			webView.loadUrl(colorProps.getProperty("cyan"));
		if(colorProps.containsKey("magenta"))
			webView.loadUrl(colorProps.getProperty("magenta"));
		if(colorProps.containsKey("yellow"))
			webView.loadUrl(colorProps.getProperty("yellow"));
		if(colorProps.containsKey("pink"))
			webView.loadUrl(colorProps.getProperty("pink"));
		if(colorProps.containsKey("blue"))
			webView.loadUrl(colorProps.getProperty("blue"));

		if(colorProps.containsKey("screenbg")){
			webView.loadUrl(colorProps.getProperty("screenbg"));
			if(!colorProps.containsKey("green") && colorProps.containsKey("inputbg"))
				webView.loadUrl(colorProps.getProperty("inputbg"));
		}
	}

	@SuppressLint("NewApi")
	private void restartConn(){
		CookieSyncManager.createInstance(getApplicationContext());
		CookieManager.getInstance().removeAllCookie();
		if(webView != null){
			webView.clearCache(true);
			webView.clearHistory();
			webView.destroy();
		}
		//changedFlag = true; //make sure URL is created again
		if(android.os.Build.VERSION.SDK_INT > 14)
			this.recreate();
		else{
			Intent intent = getIntent();
			finish();
			startActivity(intent);
		}
	}

	private void closeApp(){
		CookieSyncManager.createInstance(getApplicationContext());
		CookieManager.getInstance().removeAllCookie();
		if(webView != null){
			webView.clearCache(true);
			webView.clearHistory();
			webView.destroy();
		}
		finish();
		System.exit(0);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig){
		//do nothing here - to take care of screen orientation changes
		//when screen orientation is changed, this will be called as we added 
		//configChanges parameter in manifest xml
		super.onConfigurationChanged(newConfig);
	}
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if(keypadVisible)
			menu.getItem(0).setChecked(true);//host keypad menu item - 0
		else
			menu.getItem(0).setChecked(false);//host keypad menu item - 0
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.hatsmain, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
		case R.id.action_settings:
			//currentURL = webView.getUrl(); uncomment this only if there is a plan to enable URL overwriting. initially did that
			showHATSSettings();
			return true;
		case R.id.action_disconnect:
			webView.loadUrl("javascript:ms('disconnect','HATSForm');");
			disconnected = true;
			return true;
		case R.id.action_show_keypad:
			if(item.isChecked()){
				setKeypadDisplay(false);
				item.setChecked(false);
			}
			else{
				setKeypadDisplay(true);
				item.setChecked(true);
			}
			return true;
		/*case R.id.action_hide_keypad:
			setKeypadDisplay(false);
			return true;*/
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onActivityResult(int request, int result, Intent intent){
		if(firstTime) restarting = true;
		if(changedFlag || disconnected){
			if(!disconnected)
				webView.loadUrl("javascript:ms('disconnect','HATSForm');");
			restarting = true;
			restartConn();
		}
		else{
			loadColorRemap();
			processColorRemap();
			setOrientation();//dummy remove it soon
		}
	}

	//@Override
	public void onBackPressed() {
		// exit the application
		showExitDialog();
	}

	private boolean canRestart = false;
	public void setCanRestart(boolean val){
		canRestart = val;
		showDisconnectedDialogIfNeeded();
	}


    //floating keypad
    private void setKeypadDisplay(boolean val){
        if(val){
            keypadSpinner.setSelection(24);//enter key
            keypadSpinner.setVisibility(View.VISIBLE);
            keypadButton.setVisibility(View.VISIBLE);
            keypadVisible = true;
            keypadButton.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View button, MotionEvent me) {

                    if (me.getAction() == MotionEvent.ACTION_MOVE  ){
                        keypadDragged = true;
                        DisplayMetrics displaymetrics = new DisplayMetrics();
                        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

                        int spinnerHeight = keypadSpinner.getHeight();
                        int spinnerWidth = keypadSpinner.getWidth();
                        int buttonHeight = keypadButton.getHeight();
                        int buttonWidth = keypadButton.getWidth();

                        int screenHeight = displaymetrics.heightPixels;
                        int screenWidth = displaymetrics.widthPixels;

                        int bottomEnd = screenHeight - ((buttonHeight+ spinnerHeight));

                        int bL = 0; int bT = 0; int bR = 0; int bB = 0; int sL = 0; int sT = 0; int sR = 0;int sB = 0;



                        LayoutParams buttonParams = new LayoutParams(buttonWidth,  buttonHeight);
                        LayoutParams spinnerParams = new LayoutParams(spinnerWidth, spinnerHeight);


                        if((int)me.getRawX() - spinnerWidth >= 0){

                            if((int)me.getRawX() < screenWidth - buttonWidth){
                                bL = (int)me.getRawX() - buttonWidth;
                                sL = (int)me.getRawX() - spinnerWidth;
                            }
                            else{
                                bL = screenWidth - buttonWidth;
                                sL = screenWidth - spinnerWidth;
                            }

                            if((int)(me.getRawY() - buttonHeight - spinnerHeight) >= 0 && (int)(me.getRawY() + buttonHeight + spinnerHeight) < bottomEnd){
                                bT = (int)(me.getRawY() - buttonHeight);
                                sT = (int)(me.getRawY() - buttonHeight - spinnerHeight);
                            }else{
                                if((int)(me.getRawY() + buttonHeight + spinnerHeight) >= bottomEnd){
                                    bT = bottomEnd - buttonHeight;
                                    sT = bottomEnd - buttonHeight - spinnerHeight;
                                    bB = bottomEnd;
                                    sB = bottomEnd - buttonHeight;
                                }
                                else{
                                    bT = spinnerHeight;
                                    sT = 0;
                                    bB = (int)(me.getRawY() - buttonHeight);
                                    sB = (int)(me.getRawY() - buttonHeight - spinnerHeight);
                                }
                            }
                        }

                        else{
                            bL = spinnerWidth-buttonWidth;
                            if((int)(me.getRawY() - buttonHeight - spinnerHeight) >= 0 && (int)(me.getRawY() + buttonHeight + spinnerHeight) < bottomEnd){
                                bT = (int)(me.getRawY() - buttonHeight);
                                sT = (int)(me.getRawY() - buttonHeight - spinnerHeight);
                            }else{
                                if((int)(me.getRawY() + buttonHeight + spinnerHeight) >= bottomEnd){
                                    bT = bottomEnd - buttonHeight;
                                    sT = bottomEnd - buttonHeight - spinnerHeight;
                                    bB = bottomEnd;
                                    sB = bottomEnd - buttonHeight;
                                }
                                else{
                                    bT = spinnerHeight;
                                    sT = 0;
                                    bB = (int)(me.getRawY() - buttonHeight);
                                    sB = (int)(me.getRawY() - buttonHeight - spinnerHeight);
                                }
                            }
                            sL = 0;

                        }

                        buttonParams.setMargins(bL, bT, bR, bB);
                        spinnerParams.setMargins(sL, sT, sR, sB);

                        keypadButton.setLayoutParams(buttonParams);
                        keypadSpinner.setLayoutParams(spinnerParams);
                        keypadButton.requestLayout();
                        keypadSpinner.requestLayout();
                        webView.requestLayout();
                        return true;
                    }
                    else{
                        return false;
                    }

                }
            });

            //this is to make sure we can drag holding spinner view also. but after releasing we see spinner expanding.
            //have to find what we can do. but till then its advisable to drag using button
            keypadSpinner.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View spinner, MotionEvent me) {

                    if (me.getAction() == MotionEvent.ACTION_MOVE  ){
                        keypadDragged = true;
                        DisplayMetrics displaymetrics = new DisplayMetrics();
                        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

                        int spinnerHeight = keypadSpinner.getHeight();
                        int spinnerWidth = keypadSpinner.getWidth();
                        int buttonHeight = keypadButton.getHeight();
                        int buttonWidth = keypadButton.getWidth();

                        int screenHeight = displaymetrics.heightPixels;
                        int screenWidth = displaymetrics.widthPixels;

                        int bottomEnd = screenHeight - ((buttonHeight+ spinnerHeight));

                        int bL = 0; int bT = 0; int bR = 0; int bB = 0; int sL = 0; int sT = 0; int sR = 0;int sB = 0;



                        LayoutParams buttonParams = new LayoutParams(buttonWidth,  buttonHeight);
                        LayoutParams spinnerParams = new LayoutParams(spinnerWidth, spinnerHeight);

                        if((int)me.getRawX() - spinnerWidth >= 0){

                            if((int)me.getRawX() < screenWidth - buttonWidth){
                                bL = (int)me.getRawX() - buttonWidth;
                                sL = (int)me.getRawX() - spinnerWidth;
                            }
                            else{
                                bL = screenWidth - buttonWidth;
                                sL = screenWidth - spinnerWidth;
                            }

                            if((int)(me.getRawY() - buttonHeight - spinnerHeight) >= 0 && (int)(me.getRawY() + buttonHeight + spinnerHeight) < bottomEnd){
                                bT = (int)(me.getRawY() - buttonHeight);
                                sT = (int)(me.getRawY() - buttonHeight - spinnerHeight);
                            }else{
                                if((int)(me.getRawY() + buttonHeight + spinnerHeight) >= bottomEnd){
                                    bT = bottomEnd - buttonHeight;
                                    sT = bottomEnd - buttonHeight - spinnerHeight;
                                    bB = bottomEnd;
                                    sB = bottomEnd - buttonHeight;
                                }
                                else{
                                    bT = spinnerHeight;
                                    sT = 0;
                                    bB = (int)(me.getRawY() - buttonHeight);
                                    sB = (int)(me.getRawY() - buttonHeight - spinnerHeight);
                                }
                            }
                        }

                        else{
                            bL = spinnerWidth-buttonWidth;
                            if((int)(me.getRawY() - buttonHeight - spinnerHeight) >= 0 && (int)(me.getRawY() + buttonHeight + spinnerHeight) < bottomEnd){
                                bT = (int)(me.getRawY() - buttonHeight);
                                sT = (int)(me.getRawY() - buttonHeight - spinnerHeight);
                            }else{
                                if((int)(me.getRawY() + buttonHeight + spinnerHeight) >= bottomEnd){
                                    bT = bottomEnd - buttonHeight;
                                    sT = bottomEnd - buttonHeight - spinnerHeight;
                                    bB = bottomEnd;
                                    sB = bottomEnd - buttonHeight;
                                }
                                else{
                                    bT = spinnerHeight;
                                    sT = 0;
                                    bB = (int)(me.getRawY() - buttonHeight);
                                    sB = (int)(me.getRawY() - buttonHeight - spinnerHeight);
                                }
                            }
                            sL = 0;

                        }

                        buttonParams.setMargins(bL, bT, bR, bB);
                        spinnerParams.setMargins(sL, sT, sR, sB);

                        keypadButton.setLayoutParams(buttonParams);
                        keypadSpinner.setLayoutParams(spinnerParams);
                        keypadButton.requestLayout();
                        keypadSpinner.requestLayout();
                        webView.requestLayout();
                        return true;
                    }
                    else{
                        return false;
                    }

                }
            });
        }else{
            keypadSpinner.setVisibility(View.GONE);
            keypadButton.setVisibility(View.GONE);
            keypadVisible = false;
        }
    }
}
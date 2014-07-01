/*
 * Copyright 2014 A.C.R. Development
 */

package acr.browser.thunder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.MailTo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Message;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.webkit.GeolocationPermissions;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LightningView {

	private Title mTitle;
	private WebView mWebView;
	private BrowserController mBrowserController;
	private GestureDetector mGestureDetector;
	private Activity mActivity;
	private WebSettings mSettings;
	private static int API = android.os.Build.VERSION.SDK_INT;
	private static String mPackageName;
	private static String mHomepage;
	private static String mDefaultUserAgent;
	private static Bitmap mWebpageBitmap;
	private static SharedPreferences mPreferences;
	private static boolean mWideViewPort;
	private static AdBlock mAdBlock;
	private boolean isDestroyed = false;
	
	public LightningView(Activity activity, String url) {
		mActivity = activity;
		mWebView = new WebView(activity);
		mAdBlock = new AdBlock(activity);
		mTitle = new Title(activity);
		mPackageName = activity.getPackageName();
		mWebpageBitmap = BitmapFactory.decodeResource(activity.getResources(),
				R.drawable.ic_webpage);

		try {
			mBrowserController = (BrowserController) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement BrowserController");
		}
		mWebView.setDrawingCacheBackgroundColor(0x00000000);
		mWebView.setFocusableInTouchMode(true);
		mWebView.setFocusable(true);
		mWebView.setAnimationCacheEnabled(false);
		mWebView.setDrawingCacheEnabled(true);
		mWebView.setBackgroundColor(activity.getResources().getColor(
				android.R.color.white));
		if (API > 15) {
			mWebView.getRootView().setBackground(null);
		} else {
			mWebView.getRootView().setBackgroundDrawable(null);
		}
		mWebView.setWillNotCacheDrawing(false);
		mWebView.setAlwaysDrawnWithCacheEnabled(true);
		mWebView.setScrollbarFadingEnabled(true);
		mWebView.setSaveEnabled(true);

		mWebView.setWebChromeClient(new LightningChromeClient(activity));
		mWebView.setWebViewClient(new LightningWebClient(activity));
		mWebView.setDownloadListener(new LightningDownloadListener(activity));
		mGestureDetector = new GestureDetector(activity,
				new CustomGestureListener());
		mWebView.setOnTouchListener(new OnTouchListener() {

			float mLocation = 0;
			float mY = 0;
			int mAction = 0;

			@Override
			public boolean onTouch(View view, MotionEvent arg1) {
				if (view != null && !view.hasFocus()) {
					view.requestFocus();
				}
				mAction = arg1.getAction();
				mY = arg1.getY();
				if (mAction == MotionEvent.ACTION_DOWN) {
					mLocation = mY;
				} else if (mAction == MotionEvent.ACTION_UP) {
					if ((mY - mLocation) > 10) {
						if (mWebView.getScrollY() < 5
								&& mBrowserController.isActionBarShown()) {
							mBrowserController.hideActionBar();
						} else {
							mBrowserController.showActionBar();
						}
					} else if ((mY - mLocation) < -10) {
						mBrowserController.hideActionBar();
					}
					mLocation = 0;
				}
				mGestureDetector.onTouchEvent(arg1);
				return false;
			}

		});
		mDefaultUserAgent = mWebView.getSettings().getUserAgentString();
		mSettings = mWebView.getSettings();
		initializeSettings(mWebView.getSettings(), activity);
		initializePreferences(activity);

		if (url != null) {
			if (!url.equals("")) {
				mWebView.loadUrl(url);
			}
		} else {
			if (mHomepage.startsWith("about:home")) {
				mSettings.setUseWideViewPort(false);
				mWebView.loadUrl(getHomepage());
			} else if (mHomepage.startsWith("about:bookmarks")) {
				mBrowserController.openBookmarkPage(mWebView);
			} else {
				mWebView.loadUrl(mHomepage);
			}
		}
	}

	public TextView getTitleView() {
		return mTitle.getTitleView();
	}

	public String getHomepage() {
		String home = "";
		home = HomepageVariables.HEAD;
		switch (mPreferences.getInt(PreferenceConstants.SEARCH, 1)) {
		case 0:
			// CUSTOM SEARCH
			home = home + "file:///android_asset/lightning.png";
			home = home + HomepageVariables.MIDDLE;
			home = home
					+ mPreferences.getString(PreferenceConstants.SEARCH_URL,
							Constants.GOOGLE_SEARCH);
			break;
		case 1:
			// GOOGLE_SEARCH;
			home = home + "file:///android_asset/google.png";
			// + "https://www.google.com/images/srpr/logo11w.png";
			home = home + HomepageVariables.MIDDLE;
			home = home + Constants.GOOGLE_SEARCH;
			break;
		case 2:
			// ANDROID SEARCH;
			home = home + "file:///android_asset/lightning.png";
			home = home + HomepageVariables.MIDDLE;
			home = home + Constants.ANDROID_SEARCH;
			break;
		case 3:
			// BING_SEARCH;
			home = home + "file:///android_asset/bing.png";
			// +
			// "http://upload.wikimedia.org/wikipedia/commons/thumb/b/b1/Bing_logo_%282013%29.svg/500px-Bing_logo_%282013%29.svg.png";
			home = home + HomepageVariables.MIDDLE;
			home = home + Constants.BING_SEARCH;
			break;
		case 4:
			// YAHOO_SEARCH;
			home = home + "file:///android_asset/yahoo.png";
			// +
			// "http://upload.wikimedia.org/wikipedia/commons/thumb/2/24/Yahoo%21_logo.svg/799px-Yahoo%21_logo.svg.png";
			home = home + HomepageVariables.MIDDLE;
			home = home + Constants.YAHOO_SEARCH;
			break;
		case 5:
			// STARTPAGE_SEARCH;
			home = home + "file:///android_asset/startpage.png";
			// + "https://startpage.com/graphics/startp_logo.gif";
			home = home + HomepageVariables.MIDDLE;
			home = home + Constants.STARTPAGE_SEARCH;
			break;
		case 6:
			// STARTPAGE_MOBILE
			home = home + "file:///android_asset/startpage.png";
			// + "https://startpage.com/graphics/startp_logo.gif";
			home = home + HomepageVariables.MIDDLE;
			home = home + Constants.STARTPAGE_MOBILE_SEARCH;
		case 7:
			// DUCK_SEARCH;
			home = home + "file:///android_asset/duckduckgo.png";
			// + "https://duckduckgo.com/assets/logo_homepage.normal.v101.png";
			home = home + HomepageVariables.MIDDLE;
			home = home + Constants.DUCK_SEARCH;
			break;
		case 8:
			// DUCK_LITE_SEARCH;
			home = home + "file:///android_asset/duckduckgo.png";
			// + "https://duckduckgo.com/assets/logo_homepage.normal.v101.png";
			home = home + HomepageVariables.MIDDLE;
			home = home + Constants.DUCK_LITE_SEARCH;
			break;
		case 9:
			// BAIDU_SEARCH;
			home = home + "file:///android_asset/baidu.png";
			// + "http://www.baidu.com/img/bdlogo.gif";
			home = home + HomepageVariables.MIDDLE;
			home = home + Constants.BAIDU_SEARCH;
			break;
		case 10:
			// YANDEX_SEARCH;
			home = home + "file:///android_asset/yandex.png";
			// +
			// "http://upload.wikimedia.org/wikipedia/commons/thumb/9/91/Yandex.svg/600px-Yandex.svg.png";
			home = home + HomepageVariables.MIDDLE;
			home = home + Constants.YANDEX_SEARCH;
			break;

		}

		home = home + HomepageVariables.END;

		File homepage = new File(mActivity.getCacheDir(), "homepage.html");
		try {
			FileWriter hWriter = new FileWriter(homepage, false);
			hWriter.write(home);
			hWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return Constants.FILE + homepage;
	}

	public synchronized void initializePreferences(Context context) {
		mPreferences = context.getSharedPreferences(
				PreferenceConstants.PREFERENCES, 0);
		mHomepage = mPreferences.getString(PreferenceConstants.HOMEPAGE,
				Constants.HOMEPAGE);
		mAdBlock.updatePreference();
		if (mSettings == null && mWebView != null) {
			mSettings = mWebView.getSettings();
		} else if (mSettings == null) {
			return;
		}
		mSettings.setGeolocationEnabled(mPreferences.getBoolean(
				PreferenceConstants.LOCATION, false));
		if (API < 19) {
			switch (mPreferences.getInt(
					PreferenceConstants.ADOBE_FLASH_SUPPORT, 0)) {
			case 0:
				mSettings.setPluginState(PluginState.OFF);
				break;
			case 1: {
				mSettings.setPluginState(PluginState.ON_DEMAND);
				break;
			}
			case 2: {
				mSettings.setPluginState(PluginState.ON);
				break;
			}
			default:
				break;
			}
		}

		switch (mPreferences.getInt(PreferenceConstants.USER_AGENT, 1)) {
		case 1:
			if (API > 16)
				mSettings.setUserAgentString(WebSettings
						.getDefaultUserAgent(context));
			else
				mSettings.setUserAgentString(mDefaultUserAgent);
			break;
		case 2:
			mSettings.setUserAgentString(Constants.DESKTOP_USER_AGENT);
			break;
		case 3:
			mSettings.setUserAgentString(Constants.MOBILE_USER_AGENT);
			break;
		case 4:
			mSettings.setUserAgentString(mPreferences.getString(
					PreferenceConstants.USER_AGENT_STRING, mDefaultUserAgent));
			break;
		}

		if (mPreferences.getBoolean(PreferenceConstants.SAVE_PASSWORDS, false)) {
			if (API < 18) {
				mSettings.setSavePassword(true);
			}
			mSettings.setSaveFormData(true);
		}

		if (mPreferences.getBoolean(PreferenceConstants.JAVASCRIPT, true)) {
			mSettings.setJavaScriptEnabled(true);
			mSettings.setJavaScriptCanOpenWindowsAutomatically(true);
		}

		if (mPreferences.getBoolean(PreferenceConstants.TEXT_REFLOW, false)) {
			mSettings.setLayoutAlgorithm(LayoutAlgorithm.NARROW_COLUMNS);
		} else {
			mSettings.setLayoutAlgorithm(LayoutAlgorithm.NORMAL);
		}

		mSettings.setBlockNetworkImage(mPreferences.getBoolean(
				PreferenceConstants.BLOCK_IMAGES, false));
		mSettings.setSupportMultipleWindows(mPreferences.getBoolean(
				PreferenceConstants.POPUPS, true));
		mSettings.setUseWideViewPort(mPreferences.getBoolean(
				PreferenceConstants.USE_WIDE_VIEWPORT, true));
		mWideViewPort = mPreferences.getBoolean(
				PreferenceConstants.USE_WIDE_VIEWPORT, true);
		mSettings.setLoadWithOverviewMode(mPreferences.getBoolean(
				PreferenceConstants.OVERVIEW_MODE, true));
		switch (mPreferences.getInt(PreferenceConstants.TEXT_SIZE, 3)) {
		case 1:
			mSettings.setTextZoom(200);
			break;
		case 2:
			mSettings.setTextZoom(150);
			break;
		case 3:
			mSettings.setTextZoom(100);
			break;
		case 4:
			mSettings.setTextZoom(75);
			break;
		case 5:
			mSettings.setTextZoom(50);
			break;
		}
	}

	@SuppressLint("SetJavaScriptEnabled")
	public void initializeSettings(WebSettings settings, Context context) {
		if (API < 18) {
			settings.setAppCacheMaxSize(Long.MAX_VALUE);
		}
		if (API < 17) {
			settings.setEnableSmoothTransition(true);
		}
		if (API > 16) {
			settings.setMediaPlaybackRequiresUserGesture(true);
		}
		if (API < 19) {
			settings.setDatabasePath(context.getFilesDir().getAbsolutePath()
					+ "/databases");
		}
		settings.setDomStorageEnabled(true);
		settings.setAppCachePath(context.getCacheDir().toString());
		settings.setAppCacheEnabled(true);
		settings.setCacheMode(WebSettings.LOAD_DEFAULT);
		settings.setGeolocationDatabasePath(context.getCacheDir()
				.getAbsolutePath());
		settings.setAllowFileAccess(true);
		settings.setDatabaseEnabled(true);
		settings.setSupportZoom(true);
		settings.setBuiltInZoomControls(true);
		settings.setDisplayZoomControls(false);
		settings.setAllowContentAccess(true);
		settings.setDefaultTextEncodingName("utf-8");
		if (API > 16) {
			settings.setAllowFileAccessFromFileURLs(false);
			settings.setAllowUniversalAccessFromFileURLs(false);
		}
	}

	public boolean isShown() {
		if (mWebView != null)
			return mWebView.isShown();
		else
			return false;
	}

	public synchronized void onPause() {
		if (mWebView != null)
			mWebView.onPause();
	}

	public synchronized void onResume() {
		if (mWebView != null)
			mWebView.onResume();
	}

	public int getProgress() {
		if (mWebView != null) {
			return mWebView.getProgress();
		} else {
			return 100;
		}
	}

	public synchronized void stopLoading() {
		if (mWebView != null) {
			mWebView.stopLoading();
		}
	}

	public synchronized void pauseTimers() {
		if (mWebView != null) {
			mWebView.pauseTimers();
		}
	}

	public synchronized void resumeTimers() {
		if (mWebView != null) {
			mWebView.resumeTimers();
		}
	}

	public void requestFocus() {
		if (mWebView != null) {
			if (!mWebView.hasFocus())
				mWebView.requestFocus();
		}
	}

	public void setVisibility(int visible) {
		if (mWebView != null) {
			mWebView.setVisibility(visible);
		}
	}

	public void clearCache(boolean disk) {
		if (mWebView != null) {
			mWebView.clearCache(disk);
		}
	}

	public synchronized void reload() {
		if (mWebView != null) {
			mWebView.reload();
		}
	}

	public void setId(int id) {
		mTitle.setId(id);
	}

	public int getId() {
		if (mTitle != null) {
			return mTitle.getId();
		} else {
			return 0;
		}
	}

	public synchronized void find(String text) {
		if (mWebView != null) {
			if (API > 16) {
				mWebView.findAllAsync(text);
			} else {
				mWebView.findAll(text);
			}
		}
	}

	public synchronized void onDestroy() {
		isDestroyed = true;
		if (mWebView != null) {
			mWebView.stopLoading();
			mWebView.onPause();
			mWebView.clearHistory();
			mWebView.setVisibility(View.GONE);
			mWebView.removeAllViews();
			mWebView.destroyDrawingCache();
			// mWebView.destroy(); //this is causing the segfault
			mWebView = null;
		}
	}

	public boolean isDestroyed() {
		return isDestroyed;
	}

	public synchronized void goBack() {
		if (mWebView != null)
			mWebView.goBack();
	}

	public String getUserAgent() {
		if (mWebView != null) {
			return mWebView.getSettings().getUserAgentString();
		} else {
			return "";
		}
	}

	public void setVisible() {
		if (mWebView != null) {
			mWebView.setVisibility(View.VISIBLE);
			mBrowserController.updateUrl(mWebView.getUrl());
		}
	}
	
	public void setInvisible() {
		if (mWebView != null) {
			mWebView.setVisibility(View.INVISIBLE);
		}
	}

	public synchronized void goForward() {
		if (mWebView != null)
			mWebView.goForward();
	}

	public boolean canGoBack() {
		if (mWebView != null) {
			return mWebView.canGoBack();
		} else {
			return false;
		}
	}

	public boolean canGoForward() {
		if (mWebView != null) {
			return mWebView.canGoForward();
		} else {
			return false;
		}
	}

	public WebView getWebView() {
		return mWebView;
	}

	public Bitmap getFavicon() {
		return mTitle.getFavicon();
	}

	public void deactivateTab() {
		if (mTitle != null) {
			mTitle.deactivateTab();
		}
		onPause();
	}

	public void activateTab() {
		if (mTitle != null) {
			mTitle.activateTab();
		}
		onResume();
	}

	public synchronized void loadUrl(String url) {
		if (mWebView != null)
			mWebView.loadUrl(url);
	}

	public synchronized void invalidate() {
		if (mWebView != null)
			mWebView.invalidate();
	}

	public String getTitle() {
		return mTitle.getTitle();
	}

	public String getUrl() {
		if (mWebView != null)
			return mWebView.getUrl();
		else
			return "";
	}

	public class LightningWebClient extends WebViewClient {

		Context mActivity;

		LightningWebClient(Context context) {
			mActivity = context;
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			if (view.isShown()) {
				mBrowserController.updateUrl(url);
				if (!url.startsWith(Constants.FILE)) {
					mBrowserController.hideActionBar();
				}
				view.invalidate();
			}
			mTitle.setTitle(view.getTitle());
			mBrowserController.update();
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			Log.i("Thunder: ", url);
			if (!mSettings.getUseWideViewPort()) {
				mSettings.setUseWideViewPort(mWideViewPort);
			}
			Log.i("Thunder: ", isShown() + "");
			if (isShown()) {
				Log.i("Thunder: setUrl", url);
				mBrowserController.updateUrl(url);
				mBrowserController.showActionBar();
			}
			mTitle.setFavicon(mWebpageBitmap);
			mBrowserController.update();
		}

		@Override
		public WebResourceResponse shouldInterceptRequest(WebView view,
				String url) {
			if (mAdBlock.isAd(url)) {
				Log.i("Blocked Domain:", url);
				ByteArrayInputStream EMPTY = new ByteArrayInputStream(
						"".getBytes());
				WebResourceResponse response = new WebResourceResponse(
						"text/plain", "utf-8", EMPTY);
				return response;
			}
			return super.shouldInterceptRequest(view, url);
		}

		@Override
		public void onReceivedHttpAuthRequest(final WebView view,
				final HttpAuthHandler handler, final String host,
				final String realm) {

			AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
			final EditText name = new EditText(mActivity);
			final EditText password = new EditText(mActivity);
			LinearLayout passLayout = new LinearLayout(mActivity);
			passLayout.setOrientation(LinearLayout.VERTICAL);

			passLayout.addView(name);
			passLayout.addView(password);

			name.setHint(mActivity.getString(R.string.hint_username));
			password.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
			password.setTransformationMethod(new PasswordTransformationMethod());
			password.setHint(mActivity.getString(R.string.hint_password));
			builder.setTitle(mActivity.getString(R.string.title_sign_in));
			builder.setView(passLayout);
			builder.setCancelable(true)
					.setPositiveButton(
							mActivity.getString(R.string.title_sign_in),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									String user = name.getText().toString();
									String pass = password.getText().toString();
									handler.proceed(user.trim(), pass.trim());
									Log.i("Lightning", "Request Login");

								}
							})
					.setNegativeButton(
							mActivity.getString(R.string.action_cancel),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									handler.cancel();

								}
							});
			AlertDialog alert = builder.create();
			alert.show();

		}

		@Override
		public void onScaleChanged(WebView view, float oldScale, float newScale) {
			if (view.isShown()) {
				view.invalidate();
			}
		}

		@Override
		public void onReceivedSslError(WebView view,
				final SslErrorHandler handler, SslError error) {
			AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
			builder.setTitle(mActivity.getString(R.string.title_warning));
			builder.setMessage(
					mActivity.getString(R.string.message_untrusted_certificate))
					.setCancelable(true)
					.setPositiveButton(
							mActivity.getString(R.string.action_yes),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									handler.proceed();
								}
							})
					.setNegativeButton(mActivity.getString(R.string.action_no),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									handler.cancel();
								}
							});
			AlertDialog alert = builder.create();
			if (error.getPrimaryError() == SslError.SSL_UNTRUSTED) {
				alert.show();
			} else {
				handler.proceed();
			}

		}

		@Override
		public void onFormResubmission(WebView view, final Message dontResend,
				final Message resend) {
			AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
			builder.setTitle(mActivity
					.getString(R.string.title_form_resubmission));
			builder.setMessage(
					mActivity.getString(R.string.message_form_resubmission))
					.setCancelable(true)
					.setPositiveButton(
							mActivity.getString(R.string.action_yes),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {

									resend.sendToTarget();
								}
							})
					.setNegativeButton(mActivity.getString(R.string.action_no),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {

									dontResend.sendToTarget();
								}
							});
			AlertDialog alert = builder.create();
			alert.show();
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			if (url.startsWith("market://")
					|| url.startsWith("http://play.google.com/store/apps")
					|| url.startsWith("https://play.google.com/store/apps")) {
				Intent urlIntent = new Intent(Intent.ACTION_VIEW,
						Uri.parse(url));
				urlIntent.putExtra(mPackageName + ".Origin", 1);
				mActivity.startActivity(urlIntent);
				return true;
			} else if (url.startsWith("http://www.youtube.com")
					|| url.startsWith("https://www.youtube.com")
					|| url.startsWith("http://m.youtube.com")
					|| url.startsWith("https://m.youtube.com")) {
				Intent urlIntent = new Intent(Intent.ACTION_VIEW,
						Uri.parse(url));
				urlIntent.putExtra(mPackageName + ".Origin", 1);
				mActivity.startActivity(urlIntent);
				return true;
			} else if (url.startsWith("http://maps.google.com")
					|| url.startsWith("https://maps.google.com")) {
				Intent urlIntent = new Intent(Intent.ACTION_VIEW,
						Uri.parse(url));
				urlIntent.putExtra(mPackageName + ".Origin", 1);
				mActivity.startActivity(urlIntent);
				return true;
			} else if (url.contains("tel:") || TextUtils.isDigitsOnly(url)) {
				mActivity.startActivity(new Intent(Intent.ACTION_DIAL, Uri
						.parse(url)));
				return true;
			} else if (url.contains("mailto:")) {
				MailTo mailTo = MailTo.parse(url);
				Intent i = Utils.newEmailIntent(mActivity, mailTo.getTo(),
						mailTo.getSubject(), mailTo.getBody(), mailTo.getCc());
				mActivity.startActivity(i);
				view.reload();
				return true;
			} else if (url.startsWith("magnet:?")) {
				Intent urlIntent = new Intent(Intent.ACTION_VIEW,
						Uri.parse(url));
				urlIntent.putExtra(mPackageName + ".Origin", 1);
				mActivity.startActivity(urlIntent);
			} else if (url.startsWith("intent://")) {
				Intent intent = null;
				try {
					intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
				} catch (URISyntaxException ex) {
					return false;
				}
				if (intent != null) {
					mActivity.startActivity(intent);
					return true;
				}
			}
			return super.shouldOverrideUrlLoading(view, url);
		}
	}

	public class LightningChromeClient extends WebChromeClient {

		Context mActivity;

		LightningChromeClient(Context context) {
			mActivity = context;
		}

		@Override
		public void onProgressChanged(WebView view, int newProgress) {
			if (isShown()) {
				mBrowserController.updateProgress(newProgress);
			}
		}

		@Override
		public void onReceivedIcon(WebView view, Bitmap icon) {
			mTitle.setFavicon(icon);
			mBrowserController.update();
		}

		@Override
		public void onReceivedTitle(WebView view, String title) {
			mTitle.setTitle(title);
			mBrowserController.update();
			mBrowserController.updateHistory(title, view.getUrl());
		}

		@Override
		public void onGeolocationPermissionsShowPrompt(final String origin,
				final GeolocationPermissions.Callback callback) {
			final boolean remember = true;
			AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
			builder.setTitle(mActivity.getString(R.string.location));
			String org = null;
			if (origin.length() > 50) {
				org = (String) origin.subSequence(0, 50) + "...";
			} else {
				org = origin;
			}
			builder.setMessage(
					org + mActivity.getString(R.string.message_location))
					.setCancelable(true)
					.setPositiveButton(
							mActivity.getString(R.string.action_allow),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									callback.invoke(origin, true, remember);
								}
							})
					.setNegativeButton(
							mActivity.getString(R.string.action_dont_allow),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									callback.invoke(origin, false, remember);
								}
							});
			AlertDialog alert = builder.create();
			alert.show();

		}

		@Override
		public boolean onCreateWindow(WebView view, boolean isDialog,
				boolean isUserGesture, Message resultMsg) {
			mBrowserController.onCreateWindow(isUserGesture, resultMsg);
			return isUserGesture;
		}

		@Override
		public void onCloseWindow(WebView window) {
			// TODO Auto-generated method stub
			super.onCloseWindow(window);
		}

		public void openFileChooser(ValueCallback<Uri> uploadMsg) {
			mBrowserController.openFileChooser(uploadMsg);
		}

		public void openFileChooser(ValueCallback<Uri> uploadMsg,
				String acceptType) {
			mBrowserController.openFileChooser(uploadMsg);
		}

		public void openFileChooser(ValueCallback<Uri> uploadMsg,
				String acceptType, String capture) {
			mBrowserController.openFileChooser(uploadMsg);
		}

		@Override
		public Bitmap getDefaultVideoPoster() {
			return mBrowserController.getDefaultVideoPoster();
		}

		@Override
		public View getVideoLoadingProgressView() {
			return mBrowserController.getVideoLoadingProgressView();
		}

		@Override
		public void onHideCustomView() {
			mBrowserController.onHideCustomView();
			super.onHideCustomView();
		}

		@Override
		public void onShowCustomView(View view, CustomViewCallback callback) {
			Activity activity = mBrowserController.getActivity();
			mBrowserController.onShowCustomView(view,
					activity.getRequestedOrientation(), callback);
			super.onShowCustomView(view, callback);
		}

		@Override
		@Deprecated
		public void onShowCustomView(View view, int requestedOrientation,
				CustomViewCallback callback) {
			mBrowserController.onShowCustomView(view, requestedOrientation,
					callback);
			super.onShowCustomView(view, requestedOrientation, callback);
		}

	}

	public class Title {
		private Bitmap mFavicon;
		private String mTitle;
		private Bitmap mDefaultIcon;
		private TextView mTitleView;
		private Drawable mCloseIcon;
		private int mFaviconSize;
		private Context mContext;
		private int mId = 0;

		public Title(Context context) {
			mDefaultIcon = BitmapFactory.decodeResource(context.getResources(),
					R.drawable.ic_webpage);
			mFavicon = mDefaultIcon;
			mTitle = mActivity.getString(R.string.action_new_tab);
			mContext = context;
			LayoutInflater inflater = LayoutInflater.from(context);

			mFaviconSize = context.getResources()
					.getDrawable(R.drawable.ic_webpage).getMinimumWidth() / 2;
			mCloseIcon = context.getResources().getDrawable(
					R.drawable.ic_action_delete);
			mCloseIcon.setBounds(0, 0, mFaviconSize * 4 / 3,
					mFaviconSize * 4 / 3);
			mTitleView = (TextView) inflater.inflate(R.layout.title, null);
			mTitleView.setPadding(Utils.convertToDensityPixels(mContext, 20),
					0, Utils.convertToDensityPixels(mContext, 15), 0);

			mTitleView.setText(mContext.getResources().getString(
					R.string.action_new_tab));

			mTitleView.setText(mTitle);
			Drawable icon = new BitmapDrawable(null, mFavicon);
			icon.setBounds(0, 0, mFaviconSize, mFaviconSize);
			mTitleView.setCompoundDrawables(icon, null, mCloseIcon, null);
			mTitleView.setOnTouchListener(new OnTouchListener() {

				@Override
				public boolean onTouch(View view, MotionEvent event) {
					if (mTitleView.getCompoundDrawables()[2] != null) {
						boolean tappedX = event.getX() > (mTitleView.getWidth()
								- mTitleView.getPaddingRight() - mCloseIcon
								.getIntrinsicWidth());
						if (tappedX) {
							if (event.getAction() == MotionEvent.ACTION_UP) {
								mBrowserController.deleteTab(mId);
							}
							return true;
						} else {
							if (event.getAction() == MotionEvent.ACTION_UP) {
								Log.i("Thunder", "here we are");
								view.performClick();
							}
							return true;
						}
					}
					return false;
				}

			});
			mTitleView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (!isShown() && !isDestroyed())
						mBrowserController.showSelectedTab(mId);
				}

			});
		}

		public void setFavicon(Bitmap favicon) {
			mFavicon = favicon;
			if (mFavicon == null) {
				mFavicon = mDefaultIcon;
			}
			Drawable icon = new BitmapDrawable(null, mFavicon);
			icon.setBounds(0, 0, mFaviconSize, mFaviconSize);
			mTitleView.setCompoundDrawables(icon, null, mCloseIcon, null);
		}

		public void setTitle(String title) {
			if (title == null) {
				title = "";
			}
			mTitle = title;
			mTitleView.setText(title);
		}

		public void setTitleAndFavicon(String title, Bitmap favicon) {
			mTitle = title;
			mFavicon = favicon;
			if (mFavicon == null) {
				mFavicon = mDefaultIcon;
			}
			mTitleView.setText(title);
			Drawable icon = new BitmapDrawable(null, mFavicon);
			icon.setBounds(0, 0, mFaviconSize, mFaviconSize);
			mTitleView.setCompoundDrawables(icon, null, mCloseIcon, null);
		}

		public String getTitle() {
			return mTitle;
		}

		public Bitmap getFavicon() {
			return mFavicon;
		}

		public TextView getTitleView() {
			return mTitleView;
		}

		public void activateTab() {
			if (mTitleView != null) {
				if (API > 15) {
					mTitleView.setBackground(mContext.getResources()
							.getDrawable(R.drawable.tab_background_active));
					mTitleView.setPadding(
							Utils.convertToDensityPixels(mContext, 20), 0,
							Utils.convertToDensityPixels(mContext, 15), 0);

				} else {
					mTitleView.setBackgroundDrawable(mContext.getResources()
							.getDrawable(R.drawable.tab_background_active));
					mTitleView.setPadding(
							Utils.convertToDensityPixels(mContext, 20), 0,
							Utils.convertToDensityPixels(mContext, 15), 0);

				}
			}
		}

		public void deactivateTab() {
			if (mTitleView != null) {
				if (API > 15) {
					mTitleView.setBackground(mContext.getResources()
							.getDrawable(R.drawable.tab_background_inactive));
					mTitleView.setPadding(
							Utils.convertToDensityPixels(mContext, 20), 0,
							Utils.convertToDensityPixels(mContext, 15), 0);

				} else {
					mTitleView.setBackgroundDrawable(mContext.getResources()
							.getDrawable(R.drawable.tab_background_inactive));
					mTitleView.setPadding(
							Utils.convertToDensityPixels(mContext, 20), 0,
							Utils.convertToDensityPixels(mContext, 15), 0);

				}
			}
		}

		public void setId(int id) {
			mTitleView.setId(id);
			mId = id;
		}

		public int getId() {
			return mId;
		}
	}

	private class CustomGestureListener extends SimpleOnGestureListener {

		@Override
		public void onLongPress(MotionEvent e) {
			mBrowserController.onLongPress();
		}

	}
}

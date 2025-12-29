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
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.OkHttpClient.Builder
import okhttp3.dnsoverhttps.DnsOverHttps
import java.util.concurrent.atomic.AtomicBoolean


class MainActivity : AppCompatActivity() {

    val DOH_PROVIDER_URLS =
        arrayOf(
            "https://dns11.quad9.net/dns-query/",
            "https://dns.google/dns-query/",
            "https://anycast.dns.nextdns.io/dns-query/",
            "https://130.59.31.248/dns-query/",
            "https://dns-doh.dnsforfamily.com/dns-query/",
            "https://94.140.14.14/dns-query/",
            "https://1.1.1.2/dns-query/",
            "https://pluton.plan9-dns.com/dns-query/",
            "https://syd.adfilter.net/dns-query/",
            "https://dnspub.restena.lu/dns-query/",
            "https://blank.dnsforge.de/dns-query/",
            "https://dns.mullvad.net/dns-query/",
            "https://dns.digitale-gesellschaft.ch/dns-query/",
            "https://dns.brahma.world/dns-query/",
            "https://94.140.14.140/dns-query/",
            "https://base.dns.mullvad.net/dns-query/",
            "https://per.adfilter.net/dns-query/",
            "https://adl.adfilter.net/dns-query/",
            "https://family.dns.mullvad.net/dns-query/",
            "https://dns.digitalsize.net/dns-query/",
            "https://unicast.uncensoreddns.org/dns-query/",
            "https://dns.circl.lu/dns-query/",
            "https://dns12.quad9.net/dns-query/",
            "https://all.dns.mullvad.net/dns-query/",
            "https://dns.aa.net.uk/dns-query/",
            "https://doh.libredns.gr/dns-query/",
            "https://extended.dns.mullvad.net/dns-query/",
            "https://dns.njal.la/dns-query/",
            "https://dns9.quad9.net/dns-query/",
            "https://149.112.112.9/dns-query/",
            "https://freedns.controld.com/dns-query/",
            "https://76.76.2.11/dns-query/",
            "https://149.112.112.12/dns-query/",
            "https://dnsforge.de/dns-query/",
            "https://94.140.15.16/dns-query/",
            "https://doq.dns4all.eu/dns-query/",
            "https://ns1.fdn.fr/dns-query/",
            "https://dns10.quad9.net/dns-query/",
            "https://9.9.9.10/dns-query/",
            "https://149.112.112.11/dns-query/",
            "https://doh.ffmuc.net/dns-query/",
            "https://odvr.nic.cz/dns-query/",
            "https://ibksturm.synology.me/dns-query/",
            "https://1.0.0.3/dns-query/",
            "https://adblock.dns.mullvad.net/dns-query/",
            "https://dns-doh-no-safe-search.dnsforfamily.com/dns-query/",
            "https://dns1.dnscrypt.ca/dns-query/",
            "https://cloudflare-dns.com/dns-query/",
            "https://anycast.uncensoreddns.org/dns-query/",
            "https://dns.quad9.net/dns-query/",
        )

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
      loadAd()
      showInterstitial()
    }
    binding = ActivityMainBinding.inflate(layoutInflater)
    val view = binding.root
    setContentView(view)
    binding.retryButton.visibility = View.INVISIBLE

    // Log the Mobile Ads SDK version.
    Log.d(TAG, "Google Mobile Ads SDK Version: " + MobileAds.getVersion())

    googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(this)
    googleMobileAdsConsentManager.gatherConsent(this) { consentError ->
      if (consentError != null) {
        // Consent not obtained in current session.
        Log.w(TAG, "${consentError.errorCode}: ${consentError.message}")
      }
      if (googleMobileAdsConsentManager.isPrivacyOptionsRequired) {
        // Regenerate the options menu to include a privacy setting.
        invalidateOptionsMenu()
      }
    }
    initializeMobileAdsSdk()
    loadAd()

    // Create the "retry" button, which triggers an interstitial between game plays.
    binding.retryButton.visibility = View.INVISIBLE
    binding.retryButton.setOnClickListener { showInterstitial() }
    showInterstitial()
  }

  private fun loadAd() {
    // Request a new ad if one isn't already loaded.
    if (adIsLoading || interstitialAd != null) {
      return
    }
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
          applicationContext.stopService(Intent(this@MainActivity, AdsService::class.java))
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(Intent(this@MainActivity,
              AdblockBypassService::class.java))
          } else {
            applicationContext.startService(Intent(this@MainActivity, AdblockBypassService::class.java))
          }
          finish()
          // [END_EXCLUDE]
        }
      },
    )
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



  // Create the game timer, which counts down to the end of the level
  // and shows the "retry" button.
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

  // Show the ad if it's ready. Otherwise restart the game.
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
            finish()
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
      }
    }
  }

  // Hide the button, and kick off the timer.
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

  companion object {
    // This is an ad unit ID for a test ad. Replace with your own interstitial ad unit ID.
    const val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val GAME_LENGTH_MILLISECONDS = 100L
    const val TAG = "MainActivity"

    // Check your logcat output for the test device hashed ID e.g.
    // "Use RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("ABCDEF012345"))
    // to get test ads on this device" or
    // "Use new ConsentDebugSettings.Builder().addTestDeviceHashedId("ABCDEF012345") to set this as
    // a debug device".
    const val TEST_DEVICE_HASHED_ID = "ABCDEF012345"
  }
}

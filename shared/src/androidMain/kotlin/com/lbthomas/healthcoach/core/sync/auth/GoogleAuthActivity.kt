package com.lbthomas.healthcoach.core.sync.auth

import android.accounts.Account
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import co.touchlab.kermit.Logger
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Transparent activity handling the Google Play Services Sign-In and Consent interactive flows.
 */
class GoogleAuthActivity : ComponentActivity() {

    private val driveScope = Scope("https://www.googleapis.com/auth/drive.appdata")
    private val scopeString = "oauth2:https://www.googleapis.com/auth/drive.appdata https://www.googleapis.com/auth/userinfo.email openid"

    private var googleSignInClient: GoogleSignInClient? = null
    private var pendingAccount: Account? = null
    private var pendingEmail: String = ""

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            handleSignInResult(result.data)
        } else {
            val ex = Exception("Google Sign-In was cancelled or failed with result code ${result.resultCode}")
            Logger.w("GoogleAuthActivity: Sign-in cancelled or returned non-OK: ${result.resultCode}")
            GoogleOAuthManager.completeAuth(Result.failure(ex))
            finish()
        }
    }

    private val recoverableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val account = pendingAccount
            if (account != null) {
                fetchOAuthToken(account, pendingEmail)
            } else {
                GoogleOAuthManager.completeAuth(Result.failure(Exception("Account information missing after user recovery")))
                finish()
            }
        } else {
            val ex = Exception("Google user authorization consent was not granted (result code: ${result.resultCode})")
            Logger.w("GoogleAuthActivity: Recoverable auth consent denied: ${result.resultCode}")
            GoogleOAuthManager.completeAuth(Result.failure(ex))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(driveScope)
                .build()

            googleSignInClient = GoogleSignIn.getClient(this, gso)

            // Check if already signed in with required scopes
            val lastAccount = GoogleSignIn.getLastSignedInAccount(this)
            if (lastAccount != null && GoogleSignIn.hasPermissions(lastAccount, driveScope)) {
                val account = lastAccount.account ?: Account(lastAccount.email ?: "", "com.google")
                fetchOAuthToken(account, lastAccount.email ?: "")
                return
            }

            // Launch interactive sign-in dialog
            val signInIntent = googleSignInClient!!.signInIntent
            signInLauncher.launch(signInIntent)
        } catch (e: Exception) {
            Logger.e("GoogleAuthActivity: Failed to initiate Google Sign-In", e)
            GoogleOAuthManager.completeAuth(Result.failure(e))
            finish()
        }
    }

    private fun handleSignInResult(data: Intent?) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val signedInAccount: GoogleSignInAccount = task.getResult(ApiException::class.java)
            val email = signedInAccount.email ?: ""
            val account = signedInAccount.account ?: Account(email, "com.google")
            fetchOAuthToken(account, email)
        } catch (e: ApiException) {
            Logger.e("GoogleAuthActivity: Google Sign-In API exception (status code ${e.statusCode}): ${e.message}", e)
            GoogleOAuthManager.completeAuth(Result.failure(e))
            finish()
        } catch (e: Exception) {
            Logger.e("GoogleAuthActivity: Failed to process sign-in result", e)
            GoogleOAuthManager.completeAuth(Result.failure(e))
            finish()
        }
    }

    private fun fetchOAuthToken(account: Account, email: String) {
        pendingAccount = account
        pendingEmail = email
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Logger.i("GoogleAuthActivity: Fetching OAuth2 token for ${account.name}...")
                val token = GoogleAuthUtil.getToken(this@GoogleAuthActivity, account, scopeString)
                Logger.i("GoogleAuthActivity: Successfully acquired OAuth2 access token")
                withContext(Dispatchers.Main) {
                    GoogleOAuthManager.completeAuth(
                        Result.success(
                            GoogleAuthSession(
                                accessToken = token,
                                refreshToken = "",
                                email = email,
                                expiresInSeconds = 3600
                            )
                        )
                    )
                    finish()
                }
            } catch (e: UserRecoverableAuthException) {
                Logger.i("GoogleAuthActivity: UserRecoverableAuthException encountered, launching consent intent...")
                withContext(Dispatchers.Main) {
                    val intent = e.intent
                    if (intent != null) {
                        recoverableLauncher.launch(intent)
                    } else {
                        GoogleOAuthManager.completeAuth(Result.failure(e))
                        finish()
                    }
                }
            } catch (e: GoogleAuthException) {
                Logger.e("GoogleAuthActivity: GoogleAuthException fetching token: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    GoogleOAuthManager.completeAuth(Result.failure(e))
                    finish()
                }
            } catch (e: Exception) {
                Logger.e("GoogleAuthActivity: Unexpected error fetching token: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    GoogleOAuthManager.completeAuth(Result.failure(e))
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure pending deferred is not left hanging if activity is destroyed prematurely
        if (isFinishing) {
            GoogleOAuthManager.completeAuthIfPending(
                Result.failure(Exception("Google Sign-In flow finished without completing authorization"))
            )
        }
    }
}

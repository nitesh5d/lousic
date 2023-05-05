package `in`.fivedegree.lousic

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import `in`.fivedegree.lousic.databinding.ActivitySignUpBinding
import java.text.SimpleDateFormat
import java.util.*

private const val RC_SIGN_IN = 100

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val rootView = findViewById<View>(android.R.id.content)
        auth = Firebase.auth
        sharedPreferences =  getSharedPreferences("signup_process", Context.MODE_PRIVATE)
        var isRegistered = sharedPreferences.getString("isRegistered",null)
        var isVerified = sharedPreferences.getString("isEmailVerified",null)
        var isSetupDone = sharedPreferences.getString("isEmailVerified",null)

        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(object : Runnable {
            override fun run() {
                if (isRegistered == "true"){
                    binding.mainCont.visibility = View.GONE
                    binding.emailVerifyCont.visibility = View.VISIBLE
                    binding.stepIndicator.visibility = View.VISIBLE
                    binding.stepIndicator.setImageResource(R.drawable.ind_verify)
                }
                if (isRegistered == "true" && isVerified == "true"){
                    binding.mainCont.visibility = View.GONE
                    binding.emailVerifyCont.visibility = View.GONE
                    binding.setupCont.visibility = View.VISIBLE
                    binding.stepIndicator.visibility = View.VISIBLE
                    binding.stepIndicator.setImageResource(R.drawable.ind_setup)
                }
                mainHandler.postDelayed(this, 1000)
            }
        })

        binding.signUpBtn.setOnClickListener {
            val textemail: String = binding.emailEt.text.toString().trim()
            val textpw: String = binding.pwEt.text.toString().trim()
            if(validateEmail(textemail)){
                hideKeybaord(it)
                auth.fetchSignInMethodsForEmail(textemail).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val result = task.result
                        if (result != null && result.signInMethods != null && result.signInMethods!!.isNotEmpty()) {
                            loginUser(textemail, textpw)
                        }
                        else {
                            createUser(textemail, textpw)
                        }
                    } else {
                        binding.loading.visibility = View.GONE
                        snackbarShow("Error Occured! Try again later.", rootView)
                    }
                }
            }
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        // check if user is already signed in
        val account = GoogleSignIn.getLastSignedInAccount(this)
        binding.googleSignIn.setOnClickListener {
            binding.loading.visibility = View.VISIBLE
            if (account != null) {
                firebaseAuthWithGoogle(account.idToken!!)
            } else {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        }

        binding.openEmailApp.setOnClickListener {
            val verifyIntent = Intent(Intent.ACTION_MAIN)
            verifyIntent.addCategory(Intent.CATEGORY_APP_EMAIL)
            verifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val packageManager = packageManager
            val activities = packageManager.queryIntentActivities(verifyIntent, 0)
            if (activities != null && activities.size > 0) {
                startActivity(verifyIntent)
            } else {
                snackbarShow("Couldn't open Email App. Please Open it manually.", rootView)
            }
        }

        binding.verifyDone.setOnClickListener {
            binding.loading.visibility = View.VISIBLE
            val user = FirebaseAuth.getInstance().currentUser
            user?.reload()?.addOnCompleteListener {
                val isEmailVerified = user.isEmailVerified
                if (isEmailVerified) {
                    val editor = sharedPreferences.edit()
                    editor.apply{
                        putString("isEmailVerified","true")
                    }.apply()
                    binding.emailVerifyCont.visibility = View.GONE
                    binding.setupCont.visibility = View.VISIBLE
                    binding.loading.visibility = View.GONE
                } else {
                    binding.loading.visibility = View.GONE
                    snackbarShow("Email is not verified.", rootView)
                }
            }
        }

        binding.dpimageView.setOnClickListener {

        }
    }

    private fun createUser(email: String, pw: String) {
        val rootView = findViewById<View>(android.R.id.content)

        val hasLetter = pw.any { it.isLetter() }
        val hasDigit = pw.any { it.isDigit() }
        if(TextUtils.isEmpty(pw)){
            binding.pwEt.error = "Please Create a Password."
            binding.pwEt.requestFocus()
        }
        else if(pw.length < 8){
            binding.pwEt.error = "Password length must be minimum 8"
            binding.pwEt.requestFocus()
        }
        else if(!hasDigit || !hasLetter){
            binding.pwEt.error = "Weak Password! Try mixing letters and numbers."
            binding.pwEt.requestFocus()
        }
        else{
            binding.loading.visibility = View.VISIBLE
            auth.createUserWithEmailAndPassword(email, pw)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val editor = sharedPreferences.edit()
                        editor.apply{
                            putString("isRegistered","true")
                            putString("isEmailVerified","false")
                            putString("isSetupDone", "false")
                        }.apply()
                        auth.currentUser!!.sendEmailVerification()
                        binding.loading.visibility = View.GONE
                        snackbarShow("SignUp Successful! Verify you Email Address", rootView)
                    } else {
                        binding.loading.visibility = View.GONE
                        snackbarShow("SignUp Failed!"+ task.exception, rootView)
                    }
                }
        }
    }

    private fun loginUser(email: String, pw: String) {
        val rootView = findViewById<View>(android.R.id.content)
        binding.loading.visibility = View.VISIBLE
        auth.signInWithEmailAndPassword(email, pw)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    binding.loading.visibility = View.GONE
                    val isEmailVerified = sharedPreferences.getString("isEmailVerified",null)
                    val isSetupDone = sharedPreferences.getString("isEmailVerified",null)
                    if(isEmailVerified == "true" && isSetupDone == "true"){
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }
                    else{
                        if(isEmailVerified == "false" && isSetupDone == "false"){
                            snackbarShow("Email not verified.",rootView)
                        }
                        else if(isEmailVerified == "true" && isSetupDone == "false"){
                            snackbarShow("Setup your Profile.",rootView)
                        }
                    }
                }
                else {
                    binding.loading.visibility = View.GONE
                    when (task.exception) {
                        is FirebaseAuthInvalidCredentialsException -> {
                            // If the user's password is wrong
                            snackbarShow("Invalid Credentials!",rootView)
                        }
                        is FirebaseAuthInvalidUserException -> {
                            // If the user does not exist
                            snackbarShow("Invalid User.",rootView)
                        }
                        else -> {
                            snackbarShow("Login Failed!"+ task.exception,rootView)
                        }
                    }
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val rootView = findViewById<View>(android.R.id.content)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                binding.loading.visibility = View.GONE
                 snackbarShow("Error Occured: "+e.statusCode.toString(), rootView)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val rootView = findViewById<View>(android.R.id.content)
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                binding.loading.visibility = View.GONE
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            } else {
                binding.loading.visibility = View.GONE
                snackbarShow("Sign in failed"+ task.exception, rootView)
                // ...
            }
        }
    }

    private fun validateEmail(email: String): Boolean {
        return if (TextUtils.isEmpty(email)){
            binding.emailEt.error = "Email is Required."
            binding.emailEt.requestFocus()
            false
        } else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            binding.emailEt.error = "Please enter a valid Email Address."
            binding.emailEt.requestFocus()
            false
        } else{
            true
        }
    }

    private fun getCurrentTime(): String? {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    }

    private fun getCurrentDate(): String? {
        return SimpleDateFormat("dd/LLL/yyyy", Locale.getDefault()).format(Date())
    }

    private fun snackbarShow(msg: String, v: View) {
        val snackbar = Snackbar.make(v, msg, Snackbar.LENGTH_LONG)
        snackbar.duration = 2000
        snackbar.setBackgroundTint(resources.getColor(R.color.matte_black))
        snackbar.setTextColor(resources.getColor(R.color.white))
        snackbar.show()
    }

    private fun hideKeybaord(v: View) {
        val inputMethodManager: InputMethodManager =
            getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(v.applicationWindowToken, 0)
    }

    public override fun onStart() {
        super.onStart()

        val currentUser = auth.currentUser
        sharedPreferences =  getSharedPreferences("signup_process", Context.MODE_PRIVATE)
        sharedPreferences =  getSharedPreferences("signup_process", Context.MODE_PRIVATE)
        var isRegistered = sharedPreferences.getString("isRegistered",null)
        var isVerified = sharedPreferences.getString("isEmailVerified",null)
        var isSetupDone = sharedPreferences.getString("isEmailVerified",null)

        if (currentUser != null) {
            if(auth.currentUser?.isEmailVerified == true){
                val editor = sharedPreferences.edit()
                editor.apply{
                    putString("isEmailVerified","true")
                }.apply()
            }
            if (isRegistered =="true" && isVerified == "true" && isSetupDone =="true"){
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
        }
    }

}
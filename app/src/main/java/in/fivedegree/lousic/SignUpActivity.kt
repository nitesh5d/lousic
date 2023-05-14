package `in`.fivedegree.lousic

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
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
import com.release.gfg1.DBHelper
import `in`.fivedegree.lousic.databinding.ActivitySignUpBinding
import java.text.SimpleDateFormat
import java.util.*


private const val RC_SIGN_IN = 100

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    lateinit var sharedPreferences: SharedPreferences
    val sdk = Build.VERSION.SDK_INT
    private var dpUpdated: Boolean = false

    private lateinit var emailFinal: String
    private lateinit var nameFinal: String
    private lateinit var dpUrlFinal: String
    private lateinit var signUpDateFinal: String
    private lateinit var signUpTimeFinal: String
    private lateinit var lognDateFinal: String
    private lateinit var lognTimeFinal: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val rootView = findViewById<View>(android.R.id.content)
        auth = Firebase.auth

        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(object : Runnable {
            override fun run() {
                sharedPreferences =  getSharedPreferences("signup_process", Context.MODE_PRIVATE)
                var isRegistered = sharedPreferences.getString("isRegistered",null)
                var isVerified = sharedPreferences.getString("isEmailVerified",null)
                var isSetupDone = sharedPreferences.getString("isSetupDone",null)
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
        binding.gSignIn.setOnClickListener {
            binding.loading.visibility = View.VISIBLE
            if (account != null) {
                firebaseAuthWithGoogle(account)
            } else {
                val signInIntent = googleSignInClient.signInIntent
                launcher.launch(signInIntent)
            }
        }

        binding.fSignIn.setOnClickListener { snackbarShow("Facebook Sign In not implemented yet.", rootView) }

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
            ImagePicker.with(this)
                .cropSquare()
                .compress(1024)
                .maxResultSize(1080, 1080)
                .start()
        }

        binding.usernameEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.textView3.text = "Let's setup\nyour Profile ${s.toString()}"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.setupDone.setOnClickListener {
            val nameFinal: String = binding.usernameEt.text.toString().trim()
            if (TextUtils.isEmpty(nameFinal)){
                binding.usernameEt.error = "Please Type your Name."
                binding.usernameEt.requestFocus()
            }
            else{
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                val db = DBHelper(this, null)
                if (uid != null){
                    db.addName(nameFinal, uid)
                    val editor = sharedPreferences.edit()
                    editor.apply{
                        putString("isSetupDone","true")
                    }.apply()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }

//        val dialog = Dialog(this@SignUpActivity)

        binding.helpBtn.setOnClickListener {

            binding.helpBox.visibility = View.VISIBLE

//            dialog.setContentView(R.layout.fragment_sign_up_help_box)
//            dialog.window!!.setLayout(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT
//            )
//            dialog.setCancelable(false)
//            dialog.window!!.attributes.windowAnimations = R.style.animation
//            dialog.show()
//
//            val resetSignUp = dialog.findViewById<LinearLayout>(R.id.resetSignUp)
//            val resetPw = dialog.findViewById<LinearLayout>(R.id.resetPw)
//
//            resetSignUp.setOnClickListener(View.OnClickListener {
//                dialog.dismiss()
//                Toast.makeText(this@SignUpActivity, "okay clicked", Toast.LENGTH_SHORT).show()
//            })
//
//            resetPw.setOnClickListener(View.OnClickListener {
//                dialog.dismiss()
//                Toast.makeText(this@SignUpActivity, "Cancel clicked", Toast.LENGTH_SHORT).show()
//            })
        }

        binding.helpBoxClose.setOnClickListener { binding.helpBox.visibility = View.GONE }
    }

    override fun onActivityResult(requestCode: Int, rCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, rCode, data)
        if (rCode == Activity.RESULT_OK) {
            dpUrlFinal = data?.data.toString()
            binding.dpimageView.setImageURI(data?.data)
            if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                binding.dpimageBorder.setBackgroundDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.signup_dp_bg_green));
            } else {
                binding.dpimageBorder.background = ContextCompat.getDrawable(applicationContext, R.drawable.signup_dp_bg_green)
            }
            dpUpdated = true
        }
        else if (rCode == ImagePicker.RESULT_ERROR) {
            if (!dpUpdated){
                if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    binding.dpimageBorder.setBackgroundDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.signup_dp_bg_red));
                } else {
                    binding.dpimageBorder.background =
                        ContextCompat.getDrawable(applicationContext, R.drawable.signup_dp_bg_red)
                }
            }
            Toast.makeText(this, "Error: "+ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
        }
        else {
            if (!dpUpdated){
                if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    binding.dpimageBorder.setBackgroundDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.signup_dp_bg_red));
                } else {
                    binding.dpimageBorder.setBackground(ContextCompat.getDrawable(applicationContext, R.drawable.signup_dp_bg_red))
                }
            }
            Toast.makeText(applicationContext, "Couldn't select a photo. Please try again.", Toast.LENGTH_SHORT).show()
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
                        emailFinal = email
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
                    val editor = sharedPreferences.edit()
                    editor.apply{
                        putString("isRegistered","true")
                    }.apply()
                    val user = FirebaseAuth.getInstance().currentUser
                    user?.reload()?.addOnCompleteListener {
                        val isEmailVerified = user.isEmailVerified
                        if (isEmailVerified) {
                            editor.apply{
                                putString("isEmailVerified","true")
                            }.apply()
                        }
                    }
                    val isEmailVerified = sharedPreferences.getString("isEmailVerified",null)
                    val isSetupDone = sharedPreferences.getString("isSetupDone",null)
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

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        result ->
        val rootView = findViewById<View>(android.R.id.content)

        if (result.resultCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account: GoogleSignInAccount = task.result
                if (account != null){
                    firebaseAuthWithGoogle(account)
                }
            } catch (e: ApiException) {
                binding.loading.visibility = View.GONE
                 snackbarShow("Error Occured: "+e.message, rootView)
            }
        }
        else{
            binding.loading.visibility = View.GONE
            snackbarShow("Error Occured", rootView)
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val rootView = findViewById<View>(android.R.id.content)

        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                emailFinal = account.email.toString()
                nameFinal = account.displayName.toString()
                dpUrlFinal = account.photoUrl.toString()
                binding.loading.visibility = View.GONE
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            } else {
                binding.loading.visibility = View.GONE
                snackbarShow("Sign in failed"+ task.exception, rootView)
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

    private fun snackbarShow(msg: String, v: View) {
        val snackbar = Snackbar.make(v, msg, Snackbar.LENGTH_LONG)
        snackbar.duration = 2000
        snackbar.setBackgroundTint(resources.getColor(R.color.matte_black))
        snackbar.setTextColor(resources.getColor(R.color.white))
        snackbar.show()
    }

    private fun getCurrentTime(): String? {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    }

    private fun getCurrentDate(): String? {
        return SimpleDateFormat("dd/LLL/yyyy", Locale.getDefault()).format(Date())
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
        var isSetupDone = sharedPreferences.getString("isSetupDone",null)

        if (isRegistered =="true" && isVerified == "true" && isSetupDone =="true"){
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
        if (currentUser != null) {
            if(auth.currentUser?.isEmailVerified == true){
                val editor = sharedPreferences.edit()
                editor.apply{
                    putString("isEmailVerified","true")
                }.apply()
            }
        }
    }

}
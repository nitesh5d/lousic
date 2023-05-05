package `in`.fivedegree.lousic

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer.OnPreparedListener
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SplashScreen : AppCompatActivity() {

    private lateinit var videoView : VideoView

    lateinit var sharedPreferences : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        videoView = findViewById(R.id.videoView)
        val user = FirebaseAuth.getInstance().currentUser
        sharedPreferences = getSharedPreferences("signup_process", Context.MODE_PRIVATE)

        val isEmailVerified = sharedPreferences.getString("isEmailVerified",null)
        val isSetupDone = sharedPreferences.getString("isSetupDone",null)
        val uri : Uri = Uri.parse("android.resource://" + packageName + "/" + R.raw.splashscreen_anim)

        videoView.setVideoURI(uri)
        videoView.setZOrderOnTop(true)
        videoView.start()
        videoView.setOnPreparedListener(OnPreparedListener { mediaPlayer ->
            mediaPlayer.isLooping = false
        })

        Handler().postDelayed(Runnable {
            if (user != null) {
                if(isEmailVerified=="true" && isSetupDone == "true"){
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
                else{
                    val intent = Intent(this, SignUpActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
            }
            else{
                val i = Intent(this@SplashScreen, SignUpActivity::class.java)
                i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(i)
                finish()
            }
        }, 2500)
    }
}
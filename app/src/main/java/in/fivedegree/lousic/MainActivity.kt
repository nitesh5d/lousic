package `in`.fivedegree.lousic

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var dp: ImageView
    private lateinit var name: TextView
    private lateinit var uid: TextView
    private lateinit var bnv: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        auth = Firebase.auth

        bnv = findViewById(R.id.bottom_navigation)

//        dp = findViewById(R.id.dp)
//        name = findViewById(R.id.name)
//        uid = findViewById(R.id.uid)
//        val db = DBHelper(this, null)
//        val cursor = db.getName()
//        cursor!!.moveToFirst()
//        name.text = cursor.getString(cursor.getColumnIndex(DBHelper.NAME_COl))
//        uid.text = cursor.getString(cursor.getColumnIndex(DBHelper.UID))
//        cursor.close()

        bnv.itemIconTintList = null
        loadFragment(ExploreFragment())
        bnv.setOnItemSelectedListener {
            when(it.itemId) {
                R.id.home -> {
                    loadFragment(ExploreFragment())
                    true
                }
                R.id.upload -> {
                    loadFragment(UploadFragment())
                    true
                }
                R.id.profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> {
                    loadFragment(ExploreFragment())
                    true}
            }
        }
    }
    private  fun loadFragment(fragment: Fragment){
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container,fragment)
        transaction.commit()
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser == null){
            auth.signOut()
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
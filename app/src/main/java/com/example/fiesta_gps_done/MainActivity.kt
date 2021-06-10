package com.example.fiesta_gps_done

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.concurrent.thread
import kotlin.concurrent.timerTask

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    //許可がおりたかどうかの結果を受け取るための変数
    private val MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 1

    //現在地の緯度経度を取得するクラス
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    //FusedLocationProviderClientクラスのオブジェクトと位置情報を受け取る
    private lateinit var lastLocation: Location

    //位置情報が更新されたときに呼出され、更新内容を受け取るリスナー
    private var locationCallback: LocationCallback? = null

    //タイマーの更新間隔（今はテストなのでとりあえず30秒おき）
    //10秒だと短すぎて落ちる //ほんとに？
    val timerInterval: Long = 60000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //FusedLocationProviderClientクラスのオブジェクトを生成
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        checkPermission()

        //ここを自動更新にできないか？
        //タイマーイベントにする　（多分別のスレッドで行うことになる）
        /*btnGPS.setOnClickListener {
            if (lastLocation != null) {
                //位置情報をS3testに渡してデータをアップロードしてほしい
                val intent = Intent(this, S3_test::class.java)
                var GPSdata_lat = lastLocation.latitude //緯度
                var GPSdata_lng = lastLocation.longitude //経度
                intent.putExtra("latData", GPSdata_lat)
                intent.putExtra("lngData", GPSdata_lng)
                startActivity(intent)
            }
        }*/
    }

    //メニューを表示する関数
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    //メニューから設定が選ばれたときの処理
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.settingGPS -> {
                //Settingアクティビティに遷移
                val intent = Intent(this, Setting::class.java)
                startActivity(intent)
            }
            R.id.settingCamera -> {
                val intent = Intent(this, Setting::class.java)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    //許可ダイアログに対してユーザーが選択した結果を受け取る
    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION -> {
                if (permissions.isNotEmpty()&&grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    //パーミッション取得済みの場合
                    myLocationenable() //許可された
                } else {
                    showToast("現在地を取得できませんでした") //ユーザーが許可ダイアログを拒否
                }
            }
        }
    }

    //アプリが指定したパーミッションを持っているか確認する関数
    private fun checkPermission(){
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            myLocationenable() //パーミッション取得済みなら緯度経度を取得して画面に表示する
        } else {
            //まだパーミッションがないのでダイアログを表示してユーザーに許可を求める
            requestLocationPermission()
        }
    }

    //許可ダイアログを表示してユーザーに許可を求める関数
    private fun requestLocationPermission(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
            Manifest.permission.ACCESS_FINE_LOCATION)){
                /*許可を求め、拒否された場合→
               （なぜ必要なのか論理的根拠を示して）もう一度許可を求めるダイアログを表示する
               許可を求めるダイアログはrequestPermissionsメソッドを使って表示
               第1引数には許可を求めたいPermissionを配列で、第2引数は結果を受け取る際に識別するための数値を与える*/
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION)
        } else {
            //ユーザーはまだ許可するか判断していないので許可を求めるダイアログを表示する
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun myLocationenable(){
        val locationRequest = LocationRequest().apply {
            interval = 10000 //最も長い更新時間
            fastestInterval = 5000 //最も短い更新時間
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY //最も高精度
        }
        locationCallback = object : LocationCallback(){
            @SuppressLint("SetTextI18n")
            //位置情報を取得したときに呼出されるメソッド
            override fun onLocationResult(p0: LocationResult?) {
                if (p0?.lastLocation != null){
                    lastLocation = p0.lastLocation //位置情報を取得
                    textView.text = "緯度:${lastLocation.latitude}, 経度:${lastLocation.longitude}"
                }
            }
        }
        //引数にコールバック関数を与えてリアルタイムに位置情報を取得できるようにする
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null)

        giveGPS_S3() //位置情報をs3クラスに渡す
    }

    private fun showToast(msg: String){
        val toast = Toast.makeText(this, msg, Toast.LENGTH_LONG)
        toast.show()
    }

    //タイマーを使ってX秒間隔で位置情報をs3クラスに渡す関数
    private fun giveGPS_S3() {
        /*var timer = Timer()

        var timerTask: TimerTask.() -> Unit = {
            //位置情報をS3testに渡してデータをアップロードしてほしい
            var GPSdata_lat = lastLocation.latitude //緯度
            var GPSdata_lng = lastLocation.longitude //経度
            intent.putExtra("latData", GPSdata_lat)
            intent.putExtra("lngData", GPSdata_lng)
            startActivity(intent)
            //this.cancel()
        }
        timer.scheduleAtFixedRate(timerTask, 0, timerInterval) //delay(ms)後にTimerTaskをperiod(ms)間隔で実行*/

        val intent = Intent(this, S3_test::class.java)

        thread{
            //delay(ms)後にTimerTaskをperiod(ms)間隔で実行
            Timer().scheduleAtFixedRate(timerTask {
                Log.e("NIlu_TAG","Hello World")
                if (::lastLocation.isInitialized) {
                    //位置情報が初期化済みならそのデータをs3クラスに渡す
                    var GPSdata_lat = lastLocation.latitude //緯度
                    var GPSdata_lng = lastLocation.longitude //経度
                    intent.putExtra("latData", GPSdata_lat)
                    intent.putExtra("lngData", GPSdata_lng)
                    startActivity(intent)
                }
            }, 0, timerInterval)
        }

    }
}
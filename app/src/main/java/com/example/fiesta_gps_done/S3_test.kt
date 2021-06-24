@file:Suppress("DEPRECATION")

package com.example.fiesta_gps_done


import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSSessionCredentials
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import kotlinx.android.synthetic.main.activity_s3_test.*
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.concurrent.thread


@Suppress("DEPRECATION")
class S3_test :AppCompatActivity() {
    // この辺の個人情報を載せたままコミットしない！ Nunca cometas el mismo error; Ten muchísimo cuidado para no mostrar estas llaves a GitHub

            /*private val mContext:Context?
            init {
                val context: Context? = null
                mContext = context
            }*/

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_s3_test)

        //MainActivityから受け取ったデータ（緯度経度）を画面に表示する
        val gpsData_lat = intent.getDoubleExtra("latData", 0.0) //デフォルト値って何？
        val gpsData_lng = intent.getDoubleExtra("lngData",0.0)

        //GPSview.text = "緯度は${GPSdata_lat} で、 経度は${GPSdata_lng}"

        //MainActivityから受け取ったデータは浮動小数点型なので文字列型にキャストして次の関数に渡す
        val gpsData_lat_str:String = gpsData_lat.toString()
        val gpsData_lng_str:String = gpsData_lng.toString()

        doInBackground(gpsData_lat_str, gpsData_lng_str)
    }



    @RequiresApi(Build.VERSION_CODES.O)
    //ファイルに位置情報を書いてs3に上げる関数
    fun doInBackground(lat: String, lng: String): Int {
        //Amazon Cognito 認証情報プロバイダーを初期化
        TODO("ログイン画面をアプリに出すにはどうしたらいい？")
        val credentialsProvider = CognitoCachingCredentialsProvider(
                applicationContext,
                "ap-northeast-1:1990f673-d620-426c-82e8-2f11cdd2477a", // ID プールの ID
                Regions.AP_NORTHEAST_1
        )

        //val endpoint = "https://test-android-programming-27-de-abril.ap-northeast-1.amazonaws.com"
        //val client = AmazonKinesisClient(cognitoProvider)
        //client.setEndpoint(endpoint)

        try {
            // クライアントを生成
            val basicAWSCredentials =  BasicAWSCredentials(ACCESS_KEY,SECRET_ACCESS_KEY)
            val s3 =  AmazonS3Client(basicAWSCredentials)

            val file = File(this.cacheDir, TEST_FILE)
            //File()で新規ファイル作成, ただし空のファイルは送れない //cacheDir:ファイル書き込みできるディレクトリに移動する
            FileWriter(file).use { fw ->
                BufferedWriter(fw).use { bw ->
                    PrintWriter(bw).use { pw ->

                        pw.println(lat)
                        pw.println(lng) // 位置情報を書き込み
                    }
                }
            }

            Log.i("File", file.readText())

            //ネットワークとのやりとりはメインスレッドではできない！
            thread {
                val transferUtility = TransferUtility.builder().s3Client(s3).context(applicationContext).build()

                Files.createFile(Paths.get(TEST_FILE))
                val transferObserver = transferUtility.upload(BUCKET, "test.txt", file)
                s3.putObject(BUCKET, TEST_FILE, file)

                val result = s3.listObjects(BUCKET)
                for (objectSummary in result.objectSummaries) {
                    Log.i("listObjectsV2", " - ${objectSummary.key} (size: ${objectSummary.size})")
                }
            }

            /*thread {
                val objListing: ObjectListing = s3.listObjects(BUCKET) // バケット名を指定

                val objList = objListing.objectSummaries

                // ファイル一覧を出力
                for (obj in objList) {
                    // キー(ファイルパス)・サイズ・最終更新日
                        Log.i("info", obj.key)
                    textView3.text = (obj.key)
                }
            }*/
        }catch (e: Exception){
            Log.e("エラー", e as String)
        }

        /*val buckets: List<Bucket> = s3.listBuckets()
        textView3.text = ("Your Amazon S3 buckets are:")
        for (b in buckets) {
            textView3.text = ("* " + b.getName())
        }*/

        /*val transferUtility = TransferUtility.builder().s3Client(s3).context(applicationContext).build()
        val file = File(TEST_FILE) //File()で新規ファイル作成
        val transferObserver = transferUtility.upload(BUCKET, "test.txt", file)
        transferObserver.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState?) {}

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {}

            override fun onError(id: Int, ex: Exception?) {}
        })*/
        return 0
    }
}
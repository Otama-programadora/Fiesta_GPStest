@file:Suppress("DEPRECATION")

package com.example.fiesta_gpstest


import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import kotlinx.android.synthetic.main.activity_s3_test.*
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.lang.Exception
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.concurrent.thread


@Suppress("DEPRECATION")
class S3_test :AppCompatActivity() {
    private val TEST_FILE: String = "test.txt"     //S3に上げるファイルの名前

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
        val GPSdata = intent.getStringExtra("locationData")
        GPSview.text = GPSdata

        doInBackground(GPSdata)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    // paramsにGPSdataを入れてるよ！
    fun doInBackground(vararg params: String?): Int {
        //Amazon Cognito 認証情報プロバイダーを初期化
        /*val credentialsProvider = CognitoCachingCredentialsProvider(
                applicationContext,
                "ap-northeast-1:de70fba0-c8f9-44ec-8a81-7632762dc4b7", // ID プールの ID
                Regions.AP_NORTHEAST_1
        )
        val cred: AWSCredentials = BasicAWSCredentials(ACCESS_KEY2, SECRET_ACCESS_KEY2)
        val s3: AmazonS3 = AmazonS3Client(cred)*/

        try {

            // クライアントを生成
            var basicAWSCredentials =  BasicAWSCredentials(ACCESS_KEY,SECRET_ACCESS_KEY)
            var s3 =  AmazonS3Client(basicAWSCredentials)

            val file = File(this.cacheDir, TEST_FILE) //File()で新規ファイル作成 // ただし空のファイルは送れない
            FileWriter(file).use { fw ->
                BufferedWriter(fw).use { bw ->
                    PrintWriter(bw).use { pw ->
                        pw.println("tengo hambre!") //params
                    }
                }
            }

            File(this.cacheDir, TEST_FILE)

            //var file = File(TEST_FILE)
            Log.i("File", file.readText())

            thread {
                //val transferUtility = TransferUtility.builder().s3Client(s3).context(applicationContext).build()

                //Files.createFile(Paths.get(TEST_FILE))
                //val transferObserver = transferUtility.upload(BUCKET, "test.txt", file)
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
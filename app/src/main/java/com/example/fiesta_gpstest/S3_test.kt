@file:Suppress("DEPRECATION")

package com.example.fiesta_gpstest

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import kotlinx.android.synthetic.main.activity_s3_test.*
import java.io.File

@Suppress("DEPRECATION")
class S3_test :AppCompatActivity() {
    private val BUCKET = "test-android-programming-27-de-abril" //バケット名
    private val TEST_FILE: String = "test.txt"     //S3に上げるファイルの名前

    /*private val mContext:Context?
    init {
        val context: Context? = null
        mContext = context
    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_s3_test)

        //MainActivityから受け取ったデータ（緯度経度）を画面に表示する
        var GPSdata = intent.getStringExtra("locationData")
        GPSview.text = GPSdata

        doInBackground()
    }

    fun doInBackground(vararg params: Integer?): Int {
        //Amazon Cognito 認証情報プロバイダーを初期化
        val credentialsProvider = CognitoCachingCredentialsProvider(
                applicationContext,
                "ap-northeast-1:279ccfe3-6d86-4b42-86c2-03be265bacae", // ID プールの ID
                Regions.AP_NORTHEAST_1
        )
        val s3 = AmazonS3Client(credentialsProvider, Region.getRegion(Regions.AP_NORTHEAST_1))
        val transferUtility = TransferUtility.builder().s3Client(s3).context(applicationContext).build()
        val file = File(TEST_FILE) //File()で新規ファイル作成
        val transferObserver = transferUtility.upload(BUCKET, "test.txt", file)
        transferObserver.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState?) {}

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {}

            override fun onError(id: Int, ex: Exception?) {}
        })
        return 0
    }
}
package com.pardeep.sbfsassignment

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.firestore
import com.pardeep.sbfsassignment.databinding.ActivityMainBinding
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.UploadStatus
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.uploadAsFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() , RecInterface {
    //-----------declaration parts--------------
    var binding : ActivityMainBinding?=null
    var itemData = arrayListOf<ItemData>()
    var myAdapter = MyAdapter(itemData, this,this)
    lateinit var linearLayoutManager: LinearLayoutManager
    var db = Firebase.firestore
    var collectName = "Student data"
    private val TAG = "MainActivity"
    var imageUri : Uri ?=null
    var state : Boolean = false
    var imageViewBtn :ImageView?= null
    var cusImageViewBtn :ImageView?= null
    var supabaseClient : SupabaseClient?=null
    var imageUrl : String?=null
    //-----------declaration parts---------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --------------------Supa base ------------------------------
        supabaseClient = (applicationContext as MyApplication).supabaseClient
        // --------------------Supa base ------------------------------
        // ------------------- firestore functionality part ---------------------
        db.collection(collectName).addSnapshotListener{snapshots , e ->

            if (e !=null){
                return@addSnapshotListener
            }

            for (snapshot in snapshots!!.documentChanges){
                val itemModel = convertObject(snapshot.document)

                when(snapshot.type){

                    DocumentChange.Type.ADDED -> {
                        itemModel.let { itemData.add(it) }
                        Log.d(TAG, "onCreate: ${itemData.size}")
                        Log.d(TAG, "onCreate: $itemData")
                    }
                    DocumentChange.Type.REMOVED -> {
                        var index = getIndex(itemModel)
                        if (index > -1) {
                            itemData.removeAt(index)
                        }
                    }
                    DocumentChange.Type.MODIFIED -> {
                        itemModel.let {
                            var index = getIndex(itemModel)
                            if (index >-1){
                                itemData.set(index , it)
                            }
                        }


                    }
                    else ->{}
                }

            }
            myAdapter.notifyDataSetChanged()


        }
        // ------------------- firestore functionality part ---------------------


        // ------------------- add functionality part ---------------------
        binding?.fab?.setOnClickListener {
            Dialog(this@MainActivity).apply {
                setContentView(R.layout.custom_layout)
                window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                var nameEt = findViewById<EditText>(R.id.nameEt)
                imageViewBtn = findViewById<ImageView>(R.id.cusImageView)
                var addBtn = findViewById<Button>(R.id.addBtn)

                imageViewBtn?.setOnClickListener {
                    requestAndCheckPermission()
                    imagePicker()
                }

                addBtn.setOnClickListener {
                    if (nameEt.text.isNullOrEmpty()){
                        nameEt.error = "Enter name"
                    }else{
                        state = true
                        stateChecker()
                        Handler(Looper.getMainLooper()).postDelayed({
                            db.collection(collectName).add(ItemData(name = nameEt.text.toString() , image = imageUrl))
                                .addOnCompleteListener {
                                    Toast.makeText(this@MainActivity, "Uploaded successfully", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e->
                                    Log.e(TAG, "onCreate: ${e.message}", )
                                }
                            myAdapter.notifyDataSetChanged()
                            dismiss()
                            state = false
                        },2000)

                    }
                }
            }.show()
        }
        // ------------------- add functionality part ---------------------

        // ------------------- Recycler functionality part ---------------------

        binding?.recyclerView?.adapter = myAdapter
        linearLayoutManager = LinearLayoutManager(
            this@MainActivity ,
            LinearLayoutManager.VERTICAL,
            false)
        binding?.recyclerView?.layoutManager = linearLayoutManager
        // -------------------Recycler functionality part ---------------------
    }

    private fun requestAndCheckPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                if (Environment.isExternalStorageManager()){
                    //permission granted
                }else{
                    if (ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED){
                        requestExternalStorageManager()
                    }
                }
            }else{
                if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
                )!= PackageManager.PERMISSION_GRANTED){
                    requestExternalStorageManager()
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf( android.Manifest.permission.READ_EXTERNAL_STORAGE),
                        100
                    )
                }
            }

        }
    }

    private fun requestExternalStorageManager() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            try {
             val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
             startActivityForResult(intent ,100)   
            }
            catch (e : Exception){
                Log.d(TAG, "requestExternalStorageManager: Activity not found for these content.")
            }
        }else{
            Log.d(TAG, "requestExternalStorageManager: This is only aplicable on android 11")
        }
    }

    private fun imagePicker() {
        startActivityForResult(
            Intent(Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            ,1)
    }

    private fun getIndex(itemModel: ItemData): Int {
        var index = -1
         index = itemData.indexOfFirst { element ->
            element.id?.equals(itemModel.id) == true
        }
        return index
    }

    // ------------------- convert object functionality part ---------------------

    private fun convertObject(snapshot: QueryDocumentSnapshot): ItemData {

        val itemModel : ItemData = snapshot.toObject(ItemData::class.java)
        if (itemModel !=null){
            itemModel.id = snapshot.id?:""
        }
        return itemModel
    }
    // ------------------- convert object functionality part ---------------------

    // ------------------ interface body ---------------------
    override fun onClick(position: Int) {
        Log.d(TAG, "onClick: ${itemData[position].id}")
        AlertDialog.Builder(this@MainActivity).apply {
            setTitle("Select what you want?")
            setPositiveButton("Update"){
                _,_ ->
                updateData(position)
            }
            setNegativeButton("Delete"){
                _,_ ->
                deleteData(position)
            }
        }.show()
    }
    // ------------------ interface body ---------------------


    //--------------------- crud functionality-----------------------
    private fun deleteData(position: Int) {
        db.collection(collectName).document(itemData[position].id.toString()).delete()
        myAdapter.notifyDataSetChanged()
    }

    private fun updateData(position: Int) {
        Dialog(this).apply {
            setContentView(R.layout.custom_layout)
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            cusImageViewBtn= findViewById(R.id.cusImageView)
            var nameEt = findViewById<EditText>(R.id.nameEt)
            var updateBtn = findViewById<Button>(R.id.addBtn)
            updateBtn.setText("Update")
            nameEt.setText(itemData[position].name)

            Glide.with(this@MainActivity)
                .load(itemData[position].image)
                .into(cusImageViewBtn!!)
            cusImageViewBtn?.setOnClickListener {
                requestAndCheckPermission()
                imagePicker()
            }

            updateBtn.setOnClickListener {
                if (nameEt.text.trim().isNullOrEmpty()){
                    nameEt.error = "Enter name"
                }else{
                    state = true
                    stateChecker()
                    Handler(Looper.getMainLooper()).postDelayed({
                        var id = itemData[position].id.toString()
                        val itemObject = ItemData( id = id, name = nameEt.text.toString() , image = imageUrl)
                        db.collection(collectName).document(id).set(itemObject)
                            .addOnCompleteListener {
                                Log.d(TAG, "updateData: Modification successfully")
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "updateData: ${e.message}", )
                            }
                        myAdapter.notifyDataSetChanged()
                        dismiss()
                        state = false
                    },2000)

                }
            }

        }.show()
    }

    //--------------------- crud functionality-----------------------


    // -------------------- checker and upload----------------
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data?.data.let { uri ->
            imageUri = uri
            imageViewBtn?.setImageURI(imageUri)
            cusImageViewBtn?.setImageURI(imageUri)
            stateChecker()
        }
    }

    private fun stateChecker() {
        if (state){
            uploadToSupabase(imageUri!!)
        }
    }

    private fun uploadToSupabase(imageUri: Uri) {
        val bucket = supabaseClient?.storage?.from("Fs_assignment")
        val byteArray = convertToByteArray(imageUri,this)
        var fileName = "upload/${System.currentTimeMillis()}.jpg"

        lifecycleScope.launch {
            try {
                bucket?.uploadAsFlow(fileName,byteArray)?.collect{status ->
                    when(status){
                        is UploadStatus.Progress ->{
                            Log.d(TAG, "uploadToSupabase: progress %")
                        }

                        is UploadStatus.Success ->{
                            imageUrl = bucket.publicUrl(fileName)
                            Toast.makeText(this@MainActivity, "${imageUrl}", Toast.LENGTH_SHORT).show()
                            Log.d(TAG, "uploadToSupaBase: ${imageUrl}")

                        }
                    }
                }
            }
            catch (e : Exception){
                withContext(Dispatchers.Main){
                    Log.e(TAG, "uploadToSupaBase:Error uploading image : ${e.message}" )
                    Toast.makeText(this@MainActivity, "Error uploading image : ${e.message}", Toast.LENGTH_SHORT).show()
                }

            }
        }
    }

    private fun convertToByteArray(imageUri: Uri, context: Context): ByteArray {
        var inputStream = context.contentResolver.openInputStream(imageUri)
        return inputStream?.readBytes()?:ByteArray(0)
    }
    // -------------------- checker and upload image----------------

    //-------------------------request permission -----------------------
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            100 ->{
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "onRequestPermissionsResult: Permission granted")
                }
            }
            101 -> {
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "full storage access granted", Toast.LENGTH_SHORT).show()

                } else {
                    Toast.makeText(this, " storage access denied", Toast.LENGTH_SHORT).show()

                }
            }
        }
    }
    //-------------------------request permission -----------------------
}
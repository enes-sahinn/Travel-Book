package com.enessahin.travelbook.view

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Room
import com.enessahin.travelbook.R

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.enessahin.travelbook.databinding.ActivityMapsBinding
import com.enessahin.travelbook.model.Place
import com.enessahin.travelbook.roomdb.PlaceDao
import com.enessahin.travelbook.roomdb.PlaceDatabase
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var sharredPreferences: SharedPreferences
    private var trackBoolean: Boolean? = null
    private var selectedLongitude: Double? = null
    private var selectedLatitude: Double? = null
    private lateinit var db: PlaceDatabase
    private lateinit var placeDao: PlaceDao
    private val compositeDisposable = CompositeDisposable()
    private var placeFromMainAct: Place? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        registerLauncher()
        sharredPreferences = this.getSharedPreferences("com.enessahin.travelbook", MODE_PRIVATE)
        trackBoolean = false
        selectedLatitude = 0.0
        selectedLongitude = 0.0

        db = Room.databaseBuilder(applicationContext, PlaceDatabase::class.java, "Places")
            //.allowMainThreadQueries() // main threadde asenkrona izin verir. tehlikelidir.
            .build()
        placeDao = db.placeDao()

        binding.saveButton.isEnabled = false
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // harita ile listener arasındaki bağlantı
        mMap.setOnMapLongClickListener(this)

        val intent = intent
        val info = intent.getStringExtra("info")

        // if user tries to add new place
        if(info == "new") {
            binding.saveButton.visibility = View.VISIBLE
            binding.deleteButton.visibility = View.GONE

            locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager
            locationListener = object : LocationListener {
                override fun onLocationChanged(p0: Location) {
                    trackBoolean = sharredPreferences.getBoolean("trackBoolean", false)
                    if (trackBoolean == false) {
                        var userLocation = LatLng(p0.latitude, p0.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 16f))
                        sharredPreferences.edit().putBoolean("trackBoolean", true).apply()
                    }
                }
            }
            // location permission settings
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Snackbar.make(binding.root, "Permission needed for location!", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission") {
                        // request permission
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                } else {
                    // request permission
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            } else {
                // permission granted
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
                val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastLocation != null) {
                    val lastUserLocation = LatLng(lastLocation.latitude, lastLocation.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation, 16f))
                }
                mMap.isMyLocationEnabled = true
            }
        // if the user tries to view the details of the place
        } else {
            mMap.clear()
            placeFromMainAct = intent.getSerializableExtra("selectedPlace") as? Place
            placeFromMainAct?.let {
                val latlng = LatLng(it.latitude, it.longitude)
                mMap.addMarker(MarkerOptions().position(latlng).title(it.name))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 15f))

                binding.saveButton.visibility = View.GONE
                binding.deleteButton.visibility = View.VISIBLE
                binding.placeText.setText(it.name)
            }
        }


    }

    private fun registerLauncher() {
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if(result) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    //permission granted
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
                    val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (lastLocation != null) {
                        val lastUserLocation = LatLng(lastLocation.latitude, lastLocation.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation, 16f))
                    }
                    mMap.isMyLocationEnabled = true
                }
            }
            else {
                //permission denied
                Toast.makeText(this@MapsActivity, "Permission needed!", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onMapLongClick(p0: LatLng) {
        mMap.clear()
        mMap.addMarker(MarkerOptions().position(p0))
        selectedLatitude = p0.latitude
        selectedLongitude = p0.longitude

        binding.saveButton.isEnabled = true
    }

    fun save(view: View) {
        if(selectedLatitude != null && selectedLongitude != null) {
            val place = Place(binding.placeText.text.toString(), selectedLatitude!!, selectedLongitude!!)
            compositeDisposable.add(
                placeDao.insert(place)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponse) // bu iş bittiğinde ne yapılması gerektiği bir fonksyionda parantezsiz referans verilir.
            )
        }
    }

    private fun handleResponse() {
        val intent = Intent(this@MapsActivity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    fun delete(view: View) {
        compositeDisposable.add(
            placeDao.delete(placeFromMainAct!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleResponse)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }
}
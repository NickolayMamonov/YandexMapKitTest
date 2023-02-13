package com.example.yandexmapkittest

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PointF
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import com.yandex.mapkit.*
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingRouter
import com.yandex.mapkit.directions.driving.DrivingSession
import com.yandex.mapkit.directions.driving.VehicleOptions
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.*
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.search.*
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.Error
import com.yandex.runtime.image.ImageProvider
import com.yandex.runtime.network.NetworkError
import com.yandex.runtime.network.RemoteError


class MainActivity : AppCompatActivity(), UserLocationObjectListener, Session.SearchListener,CameraListener, DrivingSession.DrivingRouteListener {
    lateinit var mapView: MapView
    lateinit var trafficButton: Button
    lateinit var locationMapKit : UserLocationLayer
    lateinit var searchEdit: EditText
    lateinit var searchManager: SearchManager
    lateinit var searchSession: Session
    private val ROUTE_START_LOCATION = Point(55.272821, 38.748488)
    private val ROUTE_END_LOCATION = Point(55.271861, 38.735037)
    private val SCREEN_CENTER = Point((ROUTE_START_LOCATION.latitude+ROUTE_END_LOCATION.latitude)/2,
        (ROUTE_START_LOCATION.longitude+ROUTE_END_LOCATION.longitude)/2
    )
    private var mapObjects:MapObjectCollection? = null
    private var drivingRouter: DrivingRouter? = null
    private var drivingSession: DrivingSession? = null


    private fun submitQuery(query:String){
        searchSession = searchManager.submit(query, VisibleRegionUtils.toPolygon(mapView.map.visibleRegion), SearchOptions(), this)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.setApiKey("0a7e76da-0513-4064-b71e-ba004f5d6160")
        MapKitFactory.initialize(this)
        setContentView(R.layout.activity_main)
        mapView = findViewById(R.id.mapview)
        mapView.map.move(CameraPosition(Point(55.787842, 37.607905),11.0f,0.0f,0.0f), Animation(Animation.Type.SMOOTH, 10f), null)
        var mapKit:MapKit =MapKitFactory.getInstance()
        requestLocationPermission()
        var probki = mapKit.createTrafficLayer(mapView.mapWindow)
        probki.isTrafficVisible =true
//        var locationonmapkit = mapKit.createUserLocationLayer(mapView.mapWindow)
//        locationonmapkit.isVisible =true
        trafficButton = findViewById(R.id.trafficbtn)
        trafficButton.setOnClickListener {
            if(probki.isTrafficVisible == false){
                probki.isTrafficVisible = true
                trafficButton.setBackgroundResource(R.drawable.sampleblue)
            }
            else{
                probki.isTrafficVisible = false
                trafficButton.setBackgroundResource(R.drawable.blueoff)
            }
        }
        locationMapKit = mapKit.createUserLocationLayer(mapView.mapWindow)
        locationMapKit.isVisible = false
        locationMapKit.setObjectListener(this)
        SearchFactory.initialize(this)
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
        mapView.map.addCameraListener(this)
        searchEdit = findViewById(R.id.search_edit)
        searchEdit.setOnEditorActionListener { v, actionId, event ->
            if(actionId == EditorInfo.IME_ACTION_SEARCH){
                submitQuery(searchEdit.text.toString())
            }
            false
        }

        drivingRouter = DirectionsFactory.getInstance().createDrivingRouter()
        mapObjects = mapView.map.mapObjects.addCollection()
        submitRequest()

    }

    private fun requestLocationPermission(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION),0)
            return
        }
    }

    override fun onStart() {
        mapView.onStart()
        MapKitFactory.getInstance().onStart()
        super.onStart()
    }

    override fun onStop() {
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onObjectAdded(userLocationView: UserLocationView) {
        locationMapKit.setAnchor(
            PointF((mapView.width() *0.5).toFloat(),(mapView.height() *0.5).toFloat()),
            PointF((mapView.width() *0.5).toFloat(),(mapView.height() *0.83).toFloat())
        )
        userLocationView.arrow.setIcon(ImageProvider.fromResource(this,R.drawable.user_arrow))
        val picIcon = userLocationView.pin.useCompositeIcon()
        picIcon.setIcon("icon", ImageProvider.fromResource(this,R.drawable.search_result),
            IconStyle().setAnchor(PointF(0f,0f)).setRotationType(RotationType.NO_ROTATION).setZIndex(0f).setScale(1f)
        )
        picIcon.setIcon("pin", ImageProvider.fromResource(this,R.drawable.nothing),
            IconStyle().setAnchor(PointF(0.5f,0.5f)).setRotationType(RotationType.ROTATE).setZIndex(1f).setScale(0.5f)
        )
        userLocationView.accuracyCircle.fillColor = Color.BLUE and -0x66000001
    }

    override fun onObjectRemoved(p0: UserLocationView) {

    }

    override fun onObjectUpdated(p0: UserLocationView, p1: ObjectEvent) {

    }

    override fun onSearchResponse(response: Response) {
        val mapObjects:MapObjectCollection = mapView.map.mapObjects
        for(searchResult in response.collection.children){
            val resultLocation = searchResult.obj!!.geometry[0].point!!
            if(response!=null){
                mapObjects.addPlacemark(resultLocation,ImageProvider.fromResource(this,R.drawable.search_result))
            }
        }
    }

    override fun onSearchError(error: Error) {
        var errorMessage = "Неизвестная ошибка!"
        if(error is RemoteError){
            errorMessage = "Беспроводная ошибка!"
        } else if(error is NetworkError){
            errorMessage = "Проблема с интернетом!"
        }
        Toast.makeText(this,errorMessage,Toast.LENGTH_SHORT).show()

    }

    override fun onCameraPositionChanged(
        map: Map,
        cameraPosition: CameraPosition,
        cameraUpdateReason: CameraUpdateReason,
        finished: Boolean
    ) {
        if(finished){
            submitQuery(searchEdit.text.toString())
        }
    }

    override fun onDrivingRoutes(p0: MutableList<DrivingRoute>) {
       for(route in p0){
           mapObjects!!.addPolyline(route.geometry)
       }
    }

    override fun onDrivingRoutesError(p0: Error) {
        var errorMessage = "Неизвестная ошибка!"
        Toast.makeText(this,errorMessage,Toast.LENGTH_SHORT)
    }
    private fun submitRequest() {
        val drivingOptions = DrivingOptions()
        val vehicleOptions = VehicleOptions()
        val requestPoints: ArrayList<RequestPoint> = ArrayList()
        requestPoints.add(RequestPoint(ROUTE_START_LOCATION,RequestPointType.WAYPOINT,null))
        requestPoints.add(RequestPoint(ROUTE_END_LOCATION,RequestPointType.WAYPOINT,null))
        drivingSession = drivingRouter!!.requestRoutes(requestPoints,drivingOptions,vehicleOptions,this)
    }
}
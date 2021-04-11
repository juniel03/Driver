package com.bluesolution.driver.ui.directions

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bluesolution.driver.data.models.Coordinates
import com.bluesolution.driver.data.models.Order
import com.google.firebase.database.*


class MainActivityViewModel : ViewModel() {
    private val db = FirebaseDatabase.getInstance()
    var data: MutableLiveData<Order> = MutableLiveData()

    fun delete(){
        db.getReference("order").removeValue()
    }

    fun accept (accepted: Boolean){
        db.getReference("order").child("accepted").setValue(accepted)
    }
    fun pickedUp (accepted: Boolean){
        db.getReference("order").child("orderPickedUp").setValue(accepted)
    }
    fun sendRoute (path: String){
        db.getReference("order").child("pathToUser").setValue(path)
    }
    fun arrived (arrived: Boolean){
        db.getReference("order").child("arrived").setValue(arrived)
    }
    fun uploadCoordinates (coordinates: Coordinates){
        db.getReference("order").child("driverLocation").setValue(coordinates)
    }

    fun getUserCoord(onGetDataListener: onGetDataListener){
        db.getReference().child("order").child("deliveryAddress").addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(error: DatabaseError) {
                Log.d("tag", "database error: $error")
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("tag", "data on viewmodel latitude: ${snapshot.child("latitude").value} longitude: ${snapshot.child("longitude").value}")
                onGetDataListener.onSuccess(Coordinates(snapshot.child("latitude").value as Double, snapshot.child("longitude").value as Double))
            }
        })
    }
    fun getStoreCoord(onGetDataListener: onGetDataListener){
        db.getReference().child("order").child("storeAddress").addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(error: DatabaseError) {
                Log.d("tag", "database error: $error")
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("tag", "data on viewmodel latitude: ${snapshot.child("latitude").value} longitude: ${snapshot.child("longitude").value}")
                onGetDataListener.onSuccess(Coordinates(snapshot.child("latitude").value as Double, snapshot.child("longitude").value as Double))
            }
        })
    }


    fun fetchData(): LiveData<Order> {
        db.getReference().addChildEventListener(object : ChildEventListener{
            override fun onCancelled(error: DatabaseError) {
                Log.d("tag", "database error: $error")
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                Log.d("tag", "Not needed")
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                Log.d("tag", "snapshot: $snapshot")
                data.postValue(Order(snapshot.child("accepted").value as Boolean,
                        snapshot.child("arrived").value as Boolean,
                        snapshot.child("pathToUser").value as String,
                        snapshot.child("arrived").value as Boolean,
                        Coordinates(snapshot.child("deliveryAddress").child("latitude").value as Double,
                                snapshot.child("deliveryAddress").child("longitude").value as Double),
                        Coordinates(snapshot.child("storeAddress").child("latitude").value as Double,
                                snapshot.child("storeAddress").child("longitude").value as Double),
                        Coordinates(snapshot.child("driverLocation").child("latitude").value as Double,
                                snapshot.child("driverLocation").child("longitude").value as Double)
                ))
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                Log.d("tag", "not needed")
            }

        })
        return data
    }
    interface onGetDataListener{
        fun onSuccess(coordinates: Coordinates) {
        }
    }
}



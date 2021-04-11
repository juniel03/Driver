package com.bluesolution.driver.ui.user

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bluesolution.driver.data.models.Coordinates
import com.bluesolution.driver.data.models.Order
import com.bluesolution.driver.ui.directions.MainActivityViewModel
import com.google.firebase.database.*

class UserViewModel: ViewModel() {
    private val db = FirebaseDatabase.getInstance()
    var isPickedUp: MutableLiveData<Boolean> = MutableLiveData()
    var isAccepted: MutableLiveData<Boolean> = MutableLiveData()
    var isDelivered: MutableLiveData<Boolean> = MutableLiveData()
    var driverCoord: MutableLiveData<Coordinates> = MutableLiveData()

    fun order (order: Order){
        db.getReference().child("order").setValue(order)
    }
    fun getPickedUpStatus(): LiveData<Boolean>{
        db.getReference().child("order").child("orderPickedUp").addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Log.d("tag", "database error: $error")
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("tag", "data on viewmodel isPickedUP: ${snapshot.value}")
                isPickedUp.postValue(snapshot.value as Boolean)
            }
        })
        return isPickedUp
    }
    fun getDeliveredStatus(): LiveData<Boolean>{
        db.getReference().child("order").child("arrived").addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Log.d("tag", "database error: $error")
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("tag", "data on viewmodel isDelivered: ${snapshot.value}")
                isDelivered.postValue(snapshot.value as Boolean)
            }
        })
        return isDelivered
    }
    fun getAcceptStatus(): LiveData<Boolean>{
        db.getReference().child("order").child("accepted").addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Log.d("tag", "database error: $error")
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("tag", "data on viewmodel isAccepted: ${snapshot.value}")
                isAccepted.postValue(snapshot.value as Boolean)
            }
        })
        return isAccepted
    }
    fun getPath(onGetDataListener: UserViewModel.onGetDataListener){
        db.getReference().child("order").child("pathToUser").addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Log.d("tag", "database error: $error")
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                onGetDataListener.onSuccess(snapshot.value as String)
            }
        })
    }

    fun getDriverCoord(): LiveData<Coordinates>{
        db.getReference().child("order").child("driverLocation").addValueEventListener(object : ValueEventListener{
            override fun onCancelled(error: DatabaseError) {
                Log.d("tag", "database error: $error")
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                driverCoord.postValue(Coordinates(snapshot.child("latitude").value as Double, snapshot.child("longitude").value as Double))
            }
        })
        return driverCoord
    }
    interface onGetDataListener{
        fun onSuccess(path: String) {
        }
    }
}
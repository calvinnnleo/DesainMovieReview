// com.example.desainmoviereview2.NotificationManager.kt

package com.example.desainmoviereview2

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class NotificationManager {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance("https://movie-recommendation-b7ce0-default-rtdb.asia-southeast1.firebasedatabase.app")

    private val _unreadCount = MutableLiveData<Int>(0)
    val unreadCount: LiveData<Int> = _unreadCount

    private var listener: ValueEventListener? = null

    fun startListening() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        val notificationsRef = db.getReference("notifications").child(uid)

        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.children.count {
                    it.child("read").getValue(Boolean::class.java) == false
                }
                _unreadCount.postValue(count)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("NotificationManager", "Failed to listen", error.toException())
            }
        }

        notificationsRef.addValueEventListener(listener!!)
    }

    fun stopListening() {
        listener?.let {
            db.getReference("notifications").child(auth.currentUser?.uid ?: "").removeEventListener(it)
        }
    }

    fun markAllAsRead() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        val notificationsRef = db.getReference("notifications").child(uid)
        notificationsRef.get().addOnSuccessListener { snapshot ->
            for (child in snapshot.children) {
                if (child.child("read").getValue(Boolean::class.java) == false) {
                    child.ref.child("read").setValue(true)
                }
            }
        }
    }
}
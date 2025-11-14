package com.example.desainmoviereview2.forum

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.desainmoviereview2.MovieItem
import com.example.desainmoviereview2.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ForumViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance("https://movie-recommendation-b7ce0-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val forumPostsRef: DatabaseReference = db.getReference("forum_posts")
    private val usersRef: DatabaseReference = db.getReference("users")

    private val _posts = MutableStateFlow<List<ForumPost>>(emptyList())
    val posts: StateFlow<List<ForumPost>> = _posts.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private var valueEventListener: ValueEventListener? = null
    private var query: Query? = null

    init {
        _currentUserId.value = auth.currentUser?.uid
        auth.addAuthStateListener { firebaseAuth ->
            _currentUserId.value = firebaseAuth.currentUser?.uid
        }
    }

    fun initialize(movieItem: MovieItem?) {
        fetchForumPosts(movieItem?.movie_id)
    }

    private fun fetchForumPosts(movieId: String?) {
        val currentMovieId = movieId ?: return
        query = forumPostsRef.orderByChild("movie_id").equalTo(currentMovieId)

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newPosts = mutableListOf<ForumPost>()
                if (snapshot.exists()) {
                    for (postSnapshot in snapshot.children) {
                        val post = parseForumPost(postSnapshot)
                        if (post != null) {
                            newPosts.add(post)
                        }
                    }
                }
                _posts.value = newPosts.sortedByDescending { it.created_at as? Long ?: 0 }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("ForumViewModel", "loadPosts:onCancelled", error.toException())
            }
        }
        query?.addValueEventListener(valueEventListener!!)
    }

    fun submitNewPost(content: String, rating: Int, movieItem: MovieItem?) {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            val currentMovieId = movieItem?.movie_id

            if (content.isEmpty()) return@launch

            try {
                val dataSnapshot = usersRef.child(user.uid).get().await()
                val userProfile = dataSnapshot.getValue(User::class.java)
                val username = userProfile?.username ?: "Anonymous"
                val avatarBase64 = userProfile?.avatarBase64 ?: ""

                val newPostRef = forumPostsRef.push()
                val postId = newPostRef.key

                val postMap = mapOf(
                    "post_id" to postId,
                    "movie_id" to currentMovieId,
                    "content" to content,
                    "author_uid" to user.uid,
                    "author_username" to username,
                    "author_avatar_base64" to avatarBase64,
                    "user_rating" to rating,
                    "created_at" to ServerValue.TIMESTAMP
                )

                newPostRef.setValue(postMap).await()
            } catch (e: Exception) {
                Log.e("ForumViewModel", "Error submitting new post", e)
            }
        }
    }

    fun submitReplyToPost(post: ForumPost, replyContent: String) {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            val parentPostId = post.post_id ?: return@launch

            if (replyContent.isEmpty()) return@launch

            try {
                val dataSnapshot = usersRef.child(user.uid).get().await()
                val userProfile = dataSnapshot.getValue(User::class.java)
                val username = userProfile?.username ?: "Anonymous"
                val avatarBase64 = userProfile?.avatarBase64 ?: ""

                val repliesRef = forumPostsRef.child(parentPostId).child("replies")
                val newReplyRef = repliesRef.push()
                val replyId = newReplyRef.key

                val replyMap = mapOf(
                    "post_id" to replyId,
                    "author_uid" to user.uid,
                    "author_username" to username,
                    "author_avatar_base64" to avatarBase64,
                    "content" to replyContent,
                    "created_at" to ServerValue.TIMESTAMP
                )

                newReplyRef.setValue(replyMap).await()
            } catch (e: Exception) {
                Log.e("ForumViewModel", "Error submitting reply", e)
            }
        }
    }

    fun updatePost(post: ForumPost, newContent: String, newRating: Int) {
        val postRef = forumPostsRef.child(post.post_id!!)
        val updates = mapOf(
            "content" to newContent,
            "user_rating" to newRating,
            "isEdited" to true
        )
        postRef.updateChildren(updates)
    }

    fun deletePost(post: ForumPost) {
        forumPostsRef.child(post.post_id!!).removeValue()
    }

    fun updateReply(post: ForumPost, reply: Reply, newContent: String) {
        val replyRef = forumPostsRef.child(post.post_id!!).child("replies").child(reply.post_id!!)
        val updates = mapOf(
            "content" to newContent,
            "isEdited" to true
        )
        replyRef.updateChildren(updates)
    }

    fun deleteReply(post: ForumPost, reply: Reply) {
        forumPostsRef.child(post.post_id!!).child("replies").child(reply.post_id!!).removeValue()
    }

    private fun parseForumPost(postSnapshot: DataSnapshot): ForumPost? {
        val postMap = postSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any?>>() {})
        return if (postMap != null) {
            val userRating = (postMap["user_rating"] as? Number)?.toInt()
            val createdAt = (postMap["created_at"] as? Number)?.toLong()
            val isEdited = postMap["isEdited"] as? Boolean ?: false

            val repliesMap = postMap["replies"] as? Map<String, Any>
            val replies = mutableMapOf<String, Reply>()
            if (repliesMap != null) {
                for ((replyId, replyData) in repliesMap) {
                    val replyMap = replyData as? Map<String, Any?>
                    if (replyMap != null) {
                        val reply = parseReply(replyMap)
                        if (reply != null) {
                            replies[replyId] = reply
                        }
                    }
                }
            }

            ForumPost(
                post_id = postSnapshot.key,
                movie_id = postMap["movie_id"] as? String,
                title = postMap["title"] as? String,
                content = postMap["content"] as? String,
                author_uid = postMap["author_uid"] as? String,
                author_username = postMap["author_username"] as? String,
                author_avatar_base64 = postMap["author_avatar_base64"] as? String ?: "",
                user_rating = userRating,
                created_at = createdAt,
                replies = replies,
                isEdited = isEdited
            )
        } else {
            null
        }
    }

    private fun parseReply(replyMap: Map<String, Any?>): Reply? {
        val createdAt = (replyMap["created_at"] as? Number)?.toLong()
        val isEdited = replyMap["isEdited"] as? Boolean ?: false

        return Reply(
            post_id = replyMap["post_id"] as? String,
            author_uid = replyMap["author_uid"] as? String,
            author_username = replyMap["author_username"] as? String,
            author_avatar_base64 = replyMap["author_avatar_base64"] as? String ?: "",
            content = replyMap["content"] as? String,
            created_at = createdAt,
            isEdited = isEdited
        )
    }

    override fun onCleared() {
        super.onCleared()
        valueEventListener?.let { listener ->
            query?.removeEventListener(listener)
        }
    }
}

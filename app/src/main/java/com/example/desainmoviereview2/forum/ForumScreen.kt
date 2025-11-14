package com.example.desainmoviereview2.forum

import android.util.Base64
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.example.desainmoviereview2.MovieItem
import com.example.desainmoviereview2.R
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ForumScreen(
    movieItem: MovieItem,
    posts: List<ForumPost>,
    currentUserId: String?,
    onReplySubmit: (post: ForumPost, replyContent: String) -> Unit,
    onPostEdit: (post: ForumPost) -> Unit,
    onPostDelete: (post: ForumPost) -> Unit,
    onReplyEdit: (post: ForumPost, reply: Reply) -> Unit,
    onReplyDelete: (post: ForumPost, reply: Reply) -> Unit,
    onAddNewPost: (content: String, rating: Int) -> Unit
) {
    var showAddPostDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            if (currentUserId != null) {
                FloatingActionButton(onClick = { showAddPostDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Post")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                MovieHeader(movieItem = movieItem)
            }
            item {
                Text(
                    text = "Forum",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )
            }
            items(posts) { post ->
                ForumPostItem(
                    post = post,
                    currentUserId = currentUserId,
                    onReplySubmit = onReplySubmit,
                    onPostEdit = onPostEdit,
                    onPostDelete = onPostDelete,
                    onReplyEdit = onReplyEdit,
                    onReplyDelete = onReplyDelete
                )
            }
        }

        if (showAddPostDialog) {
            AddNewPostDialog(
                onDismiss = { showAddPostDialog = false },
                onSubmit = { content, rating ->
                    onAddNewPost(content, rating)
                    showAddPostDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MovieHeader(movieItem: MovieItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        GlideImage(
            model = movieItem.primary_image_url,
            contentDescription = "Movie Poster Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(16.dp)
        )
        GlideImage(
            model = movieItem.primary_image_url,
            contentDescription = "Movie Poster",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = movieItem.title ?: "No Title",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1.0f)
            )
            RatingIndicator(rating = (movieItem.rating?.toFloat() ?: 0f) / 2f)
        }
        Spacer(modifier = Modifier.height(8.dp))

        var isExpanded by remember { mutableStateOf(false) }
        var canBeExpanded by remember { mutableStateOf(false) }
        val overview = movieItem.overview ?: ""

        Column(
            modifier = Modifier.animateContentSize(animationSpec = spring())
        ) {
            Text(
                text = overview,
                maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                onTextLayout = { result ->
                    if (!isExpanded) {
                        canBeExpanded = result.hasVisualOverflow
                    }
                }
            )
            if (canBeExpanded) {
                Text(
                    text = if (isExpanded) "Show less" else "Show more",
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp)
                        .clickable { isExpanded = !isExpanded },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ForumPostItem(
    post: ForumPost,
    currentUserId: String?,
    onReplySubmit: (post: ForumPost, replyContent: String) -> Unit,
    onPostEdit: (post: ForumPost) -> Unit,
    onPostDelete: (post: ForumPost) -> Unit,
    onReplyEdit: (post: ForumPost, reply: Reply) -> Unit,
    onReplyDelete: (post: ForumPost, reply: Reply) -> Unit,
) {
    var showReplyInput by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Post Author and Menu
            Row(verticalAlignment = Alignment.CenterVertically) {
                val decodedImage = remember(post.author_avatar_base64) {
                    try {
                        Base64.decode(post.author_avatar_base64, Base64.DEFAULT)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }
                GlideImage(
                    model = decodedImage,
                    contentDescription = "Author Avatar",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    failure = placeholder {
                        Image(
                            painter = painterResource(R.drawable.ic_anonymous),
                            contentDescription = "Anonymous avatar"
                        )
                    }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = post.author_username ?: "Anonymous", fontWeight = FontWeight.Bold)
                    post.created_at?.let {
                        Text(
                            text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(it)),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                if (currentUserId == post.author_uid) {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Edit Post")
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { Text("Edit") }, onClick = { onPostEdit(post); showMenu = false })
                            DropdownMenuItem(text = { Text("Delete") }, onClick = { onPostDelete(post); showMenu = false })
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Post Content
            Text(text = post.content ?: "")
            if (post.isEdited) {
                Text(
                    text = "(edited)",
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            RatingIndicator(rating = post.user_rating?.toFloat() ?: 0f)
            Spacer(modifier = Modifier.height(8.dp))

            // Actions and Replies
            if (currentUserId != null) {
                TextButton(onClick = { showReplyInput = !showReplyInput }) {
                    Text(text = "Reply")
                }
            }

            if (showReplyInput) {
                var replyText by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    label = { Text("Write a reply...") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row {
                    TextButton(onClick = { showReplyInput = false }) { Text("Cancel") }
                    TextButton(onClick = {
                        onReplySubmit(post, replyText)
                        replyText = ""
                        showReplyInput = false
                    }) {
                        Text("Submit")
                    }
                }
            }

            // Comments
            val sortedReplies = post.replies.values.toList().sortedBy { it.created_at }
            if (sortedReplies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                sortedReplies.forEach { reply ->
                    CommentItem(
                        post = post,
                        reply = reply,
                        currentUserId = currentUserId,
                        onReplyEdit = onReplyEdit,
                        onReplyDelete = onReplyDelete
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun CommentItem(
    post: ForumPost,
    reply: Reply,
    currentUserId: String?,
    onReplyEdit: (post: ForumPost, reply: Reply) -> Unit,
    onReplyDelete: (post: ForumPost, reply: Reply) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        val decodedImage = remember(reply.author_avatar_base64) {
            try {
                Base64.decode(reply.author_avatar_base64, Base64.DEFAULT)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
        GlideImage(
            model = decodedImage,
            contentDescription = "Author Avatar",
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape),
            failure = placeholder {
                Image(
                    painter = painterResource(R.drawable.ic_anonymous),
                    contentDescription = "Anonymous avatar"
                )
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reply.author_username ?: "Anonymous",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = reply.content ?: "", fontSize = 14.sp)
            if (reply.isEdited) {
                Text(
                    text = "(edited)",
                    fontSize = 10.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color.Gray
                )
            }
        }
        if (currentUserId == reply.author_uid) {
            var showMenu by remember { mutableStateOf(false) }
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "Edit Reply")
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { onReplyEdit(post, reply); showMenu = false })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { onReplyDelete(post, reply); showMenu = false })
                }
            }
        }
    }
}

@Composable
fun RatingIndicator(rating: Float, maxRating: Int = 5) {
    Row {
        for (i in 1..maxRating) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (i <= rating) Color.Yellow else Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun AddNewPostDialog(
    onDismiss: () -> Unit,
    onSubmit: (content: String, rating: Int) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a New Post") },
        text = {
            Column {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Your post...") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "rating",
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { rating = i },
                            tint = if (i <= rating) Color.Yellow else Color.Gray
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(content, rating) }) {
                Text("Submit")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

val mockMovieItem = MovieItem(
    movie_id = "1",
    title = "Awesome Movie Title",
    overview = "This is a really cool movie about a developer who fixes a tricky bug. The story is compelling and full of suspense. It has many twists and turns that will keep you on the edge of your seat. The character development is fantastic, and the cinematography is breathtaking. We highly recommend watching this film as soon as possible. You won't regret it!",
    rating = 8.5,
    primary_image_url = "" // Add a placeholder image URL if you have one
)

val mockReplies = mapOf(
    "reply1" to Reply(
        post_id = "reply1",
        author_uid = "user2",
        author_username = "JaneDoe",
        content = "I totally agree with this post!",
        created_at = System.currentTimeMillis() - 100000,
        isEdited = true
    )
)

val mockPosts = listOf(
    ForumPost(
        post_id = "post1",
        author_uid = "user1",
        author_username = "JohnAppleseed",
        content = "This is the first forum post. I think the movie's ending was fantastic and really thought-provoking. What did everyone else think?",
        user_rating = 4,
        created_at = System.currentTimeMillis(),
        replies = mockReplies,
        isEdited = false
    ),
    ForumPost(
        post_id = "post2",
        author_uid = "user2",
        author_username = "JaneDoe",
        content = "This is another post. I felt the pacing in the second act was a bit slow, but the final action sequence made up for it.",
        user_rating = 3,
        created_at = System.currentTimeMillis() - 200000,
        isEdited = true
    )
)

@Preview(showBackground = true)
@Composable
fun ForumScreenPreview() {
    MaterialTheme {
        ForumScreen(
            movieItem = mockMovieItem,
            posts = mockPosts,
            currentUserId = "user1",
            onReplySubmit = { _, _ -> },
            onPostEdit = {},
            onPostDelete = {},
            onReplyEdit = { _, _ -> },
            onReplyDelete = { _, _ -> },
            onAddNewPost = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ForumPostItemPreview() {
    MaterialTheme {
        ForumPostItem(
            post = mockPosts.first(),
            currentUserId = "user1",
            onReplySubmit = { _, _ -> },
            onPostEdit = {},
            onPostDelete = {},
            onReplyEdit = { _, _ -> },
            onReplyDelete = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CommentItemPreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CommentItem(
                post = mockPosts.first(),
                reply = mockReplies.values.first(),
                currentUserId = "user2",
                onReplyEdit = { _, _ -> },
                onReplyDelete = { _, _ -> }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddNewPostDialogPreview() {
    MaterialTheme {
        AddNewPostDialog(
            onDismiss = {},
            onSubmit = { _, _ -> }
        )
    }
}

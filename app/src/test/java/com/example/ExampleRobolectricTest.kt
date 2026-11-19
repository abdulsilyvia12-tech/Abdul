package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.cache.BlinkOfflineCache
import com.example.data.firebase.FirebaseRtdbManager
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.ReportReason
import com.example.data.model.User
import com.example.data.repository.BlinkRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Blinkchat", appName)
    }

    @Test
    fun `repository initial state and account creation`() {
        val repo = BlinkRepository()
        assertNotNull(repo.currentUser.value)
        assertEquals("alex", repo.currentUser.value?.username)

        // Test creating new account
        val created = repo.createAccount("sarah_connor", "Sarah Connor", "sarah@blink.com", "Future survivor")
        assertTrue(created)
        assertEquals("sarah_connor", repo.currentUser.value?.username)
    }

    @Test
    fun `repository posts and likes workflow`() {
        val repo = BlinkRepository()
        val initialPosts = repo.posts.value
        assertTrue(initialPosts.isNotEmpty())

        val firstPost = initialPosts.first()

        // Toggle like
        repo.toggleLikePost(firstPost.id)
        val updatedPost = repo.posts.value.find { it.id == firstPost.id }
        assertNotNull(updatedPost)

        // Post creation
        repo.createPost("Test snap with Cyberpunk vibe!", "Cyberpunk Neon")
        val newPost = repo.posts.value.first()
        assertEquals("Test snap with Cyberpunk vibe!", newPost.caption)
    }

    @Test
    fun `repository comments workflow`() {
        val repo = BlinkRepository()
        val postId = "post_1"
        repo.addComment(postId, "Insane night lighting!")

        val comments = repo.comments.value[postId]
        assertNotNull(comments)
        assertTrue(comments!!.any { it.text == "Insane night lighting!" })
    }

    @Test
    fun `repository chat and disappearing blink messages`() {
        val repo = BlinkRepository()
        repo.sendMessage("conv_maya", "Check this snap!", isBlink = true)

        val messages = repo.messages.value["conv_maya"]
        assertNotNull(messages)
        val lastMsg = messages!!.last()
        assertEquals("Check this snap!", lastMsg.text)
        assertTrue(lastMsg.isBlink)
        assertEquals(10, lastMsg.blinkSecondsRemaining)

        // Test normal text message
        repo.sendMessage("conv_maya", "See you tonight at 8 PM", isBlink = false)
        val updatedMessages = repo.messages.value["conv_maya"]
        assertNotNull(updatedMessages)
        val secondMsg = updatedMessages!!.last()
        assertEquals("See you tonight at 8 PM", secondMsg.text)
        assertFalse(secondMsg.isBlink)
        assertTrue(secondMsg.isFromMe)
    }

    @Test
    fun `repository block user and filter content`() {
        val repo = BlinkRepository()
        val maya = repo.allUsers.value.find { it.username == "maya_lin" }
        assertNotNull(maya)

        // Block Maya
        repo.blockUser(maya!!)
        assertTrue(repo.blockedUsers.value.any { it.username == "maya_lin" })

        // Maya's posts should be filtered out
        assertFalse(repo.posts.value.any { it.authorUsername == "maya_lin" })

        // Unblock Maya
        repo.unblockUser(maya.id)
        assertFalse(repo.blockedUsers.value.any { it.username == "maya_lin" })
    }

    @Test
    fun `repository safety report and privacy settings`() {
        val repo = BlinkRepository()
        repo.reportContent("user_kai", "post_1", ReportReason.HARASSMENT, "Testing safety report")
        assertEquals(1, repo.reports.value.size)

        // Toggle ghost mode
        repo.updateProfile("Alex Rivers", "Updated bio", "In Ghost Mode 👻", isGhostMode = true)
        assertTrue(repo.currentUser.value?.isGhostMode == true)
        assertTrue(repo.privacySettings.value.ghostMode)
    }

    @Test
    fun `rtdb config schema nodes and canonical chat id symmetry`() {
        assertEquals(
            "https://blinkchat-8d1a7-default-rtdb.europe-west1.firebasedatabase.app",
            FirebaseRtdbManager.DATABASE_URL
        )
        assertEquals("blinkchat-8d1a7", FirebaseRtdbManager.PROJECT_ID)
        assertEquals("users", FirebaseRtdbManager.NODE_USERS)
        assertEquals("chats", FirebaseRtdbManager.NODE_CHATS)
        assertEquals("messages", FirebaseRtdbManager.NODE_MESSAGES)
        assertEquals("blinks", FirebaseRtdbManager.NODE_BLINKS)
        assertEquals("reports", FirebaseRtdbManager.NODE_REPORTS)

        val chatAB = FirebaseRtdbManager.canonicalDirectChatId("phone_user_1", "phone_user_2")
        val chatBA = FirebaseRtdbManager.canonicalDirectChatId("phone_user_2", "phone_user_1")
        assertEquals(chatAB, chatBA)
        assertEquals("dm_phone_user_1_phone_user_2", chatAB)

        assertEquals("a_b_c_d_e", FirebaseRtdbManager.sanitizeKey("a.b#c\$d[e"))
    }

    @Test
    fun `offline cache persists messages and conversations`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cache = BlinkOfflineCache(context)
        val convId = FirebaseRtdbManager.canonicalDirectChatId("user_alex", "user_zoe")
        val conv = Conversation(
            id = convId,
            participantId = "user_zoe",
            name = "Zoe",
            username = "zoe_k",
            avatarColorHex = "#06B6D4",
            lastMessage = "Sync check!",
            lastMessageTime = "10:15 PM",
            isOnline = true
        )
        val msg = ChatMessage(
            id = "msg_persist_1",
            conversationId = convId,
            senderId = "user_alex",
            senderName = "Alex",
            text = "Sync check!",
            timestamp = 1700000000000L,
            timeFormatted = "10:15 PM",
            isFromMe = true,
            reactions = mapOf("🔥" to listOf("user_zoe"))
        )

        cache.saveOfflineState(
            currentUser = null,
            posts = emptyList(),
            stories = emptyList(),
            conversations = listOf(conv),
            messages = mapOf(convId to listOf(msg)),
            customers = emptyList(),
            payments = emptyList()
        )

        val reloadedCache = BlinkOfflineCache(context)
        val loadedConvs = reloadedCache.loadCachedConversations()
        val loadedMessages = reloadedCache.loadCachedMessages()
        assertNotNull(loadedConvs)
        assertNotNull(loadedMessages)
        assertTrue(loadedConvs!!.any { it.id == convId && it.lastMessage == "Sync check!" })
        val persistedMsg = loadedMessages!![convId]?.firstOrNull()
        assertNotNull(persistedMsg)
        assertEquals("Sync check!", persistedMsg?.text)
        assertEquals(listOf("user_zoe"), persistedMsg?.reactions?.get("🔥"))
    }

    @Test
    fun `repository resolves canonical direct chat and persists messages`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = BlinkRepository(context)
        val target = User(
            id = "phone_two_user",
            username = "phone_two",
            displayName = "Phone Two",
            email = "two@blinkchat.com"
        )
        val chat = repo.getOrCreateDirectChat(target)
        val expectedId = FirebaseRtdbManager.canonicalDirectChatId(
            repo.currentUser.value!!.id,
            target.id
        )
        assertEquals(expectedId, chat.id)

        repo.sendMessage(chat.id, "Hello across phones!", messageId = "msg_cross_1")
        val saved = repo.messages.value[chat.id]?.find { it.id == "msg_cross_1" }
        assertNotNull(saved)
        assertEquals("Hello across phones!", saved?.text)
    }
}

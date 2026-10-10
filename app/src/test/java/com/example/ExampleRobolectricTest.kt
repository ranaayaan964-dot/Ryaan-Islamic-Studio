package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.database.UserDao
import com.example.data.database.UserEntity
import com.example.data.database.UserSessionManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var db: AppDatabase
  private lateinit var userDao: UserDao
  private lateinit var context: Context

  @Before
  fun createDb() {
    context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    userDao = db.userDao()
  }

  @After
  fun closeDb() {
    db.close()
  }

  @Test
  fun `read string from context`() {
    val appName = context.getString(R.string.app_name)
    assertEquals("Prayer Times", appName)
  }

  @Test
  fun `initial check returns no active session when database is empty`() = runBlocking {
    val currentUser = userDao.getCurrentUser()
    assertNull(currentUser)

    val activeCount = userDao.getActiveSessionCount()
    assertEquals(0, activeCount)
  }

  @Test
  fun `insert user credentials stores session properly`() = runBlocking {
    val user = UserEntity(
      id = 1L,
      username = "ryaan",
      passwordHash = "11221122",
      fullName = "Ryaan Usman",
      email = "ranaayaan964@gmail.com",
      sessionToken = "session_token_123",
      isLoggedIn = true
    )
    userDao.insertUser(user)

    val storedUser = userDao.getCurrentUser()
    assertNotNull(storedUser)
    assertEquals("ryaan", storedUser?.username)
    assertEquals("11221122", storedUser?.passwordHash)
    assertTrue(storedUser?.isLoggedIn == true)

    val activeSession = userDao.getActiveSession()
    assertNotNull(activeSession)
    assertEquals(1, userDao.getActiveSessionCount())
  }

  @Test
  fun `logout updates session to not logged in`() = runBlocking {
    val user = UserEntity(
      id = 1L,
      username = "ryaan",
      passwordHash = "11221122",
      isLoggedIn = true,
      sessionToken = "token_abc"
    )
    userDao.insertUser(user)
    assertTrue(userDao.getCurrentUser()?.isLoggedIn == true)

    userDao.logout()
    val afterLogout = userDao.getCurrentUser()
    assertNotNull(afterLogout)
    assertFalse(afterLogout?.isLoggedIn == true)
    assertNull(afterLogout?.sessionToken)
    assertNull(userDao.getActiveSession())
  }
}

package tv.caffeine.app.profile


import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.databinding.FragmentMyProfileBinding
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.setOnAction
import java.io.ByteArrayOutputStream
import javax.inject.Inject

private const val REQUEST_GET_PHOTO = 1

class MyProfileFragment : CaffeineFragment() {
    @Inject lateinit var accountsService: AccountsService
    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var followManager: FollowManager
    @Inject lateinit var gson: Gson

    private lateinit var binding: FragmentMyProfileBinding

    private val viewModel by lazy { viewModelProvider.get(MyProfileViewModel::class.java) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentMyProfileBinding.inflate(inflater, container, false)
        binding.setLifecycleOwner(viewLifecycleOwner)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = viewModel
        binding.signOutButton.setOnClickListener { signOut() }
        binding.infoButton.setOnClickListener {
            val action = MyProfileFragmentDirections.actionMyProfileFragmentToEditBioFragment()
            findNavController().navigate(action)
        }
        binding.nameEditText.setOnAction(EditorInfo.IME_ACTION_SEND) {
            val updatedName = binding.nameEditText.text.toString()
            viewModel.updateName(updatedName)
        }
        binding.settingsButton.setOnClickListener {
            val action = MyProfileFragmentDirections.actionMyProfileFragmentToSettingsFragment()
            findNavController().navigate(action)
        }
        binding.goldAndCreditsButton.setOnClickListener {
            val action = MyProfileFragmentDirections.actionMyProfileFragmentToGoldAndCreditsFragment()
            findNavController().navigate(action)
        }
        binding.numberFollowingTextView.setOnClickListener { showFollowingList() }
        binding.numberOfFollowersTextView.setOnClickListener { showFollowersList() }
        binding.avatarImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            startActivityForResult(intent, REQUEST_GET_PHOTO)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != REQUEST_GET_PHOTO) {
            return super.onActivityResult(requestCode, resultCode, data)
        }
        if (resultCode != Activity.RESULT_OK) return
        val uri = data?.data ?: return
        launch(Dispatchers.IO) {
            val inputStream = context?.contentResolver?.openInputStream(uri) ?: return@launch
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val width = (1024 * aspectRatio).toInt()
            val height = 1024
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false)
            val stream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
            val body = RequestBody.create(MediaType.get("image/png"), stream.toByteArray())
            stream.close()
            val result = accountsService.uploadAvatar(body).awaitAndParseErrors(gson)
            val view = view ?: return@launch
            withContext(Dispatchers.Main) {
                handle(result, view) {
                    viewModel.reload()
                }
            }
        }
    }

    private fun showFollowingList() {
        val caid = tokenStore.caid ?: return
        val action = MyProfileFragmentDirections.actionMyProfileFragmentToFollowingFragment(caid)
        findNavController().navigate(action)
    }

    private fun showFollowersList() {
        val caid = tokenStore.caid ?: return
        val action = MyProfileFragmentDirections.actionMyProfileFragmentToFollowersFragment(caid)
        findNavController().navigate(action)
    }

    private fun signOut() {
        tokenStore.clear()
        findNavController().popBackStack()
        findNavController().navigate(R.id.action_global_landingFragment)
        accountsService.signOut().enqueue(object : Callback<Unit?> {
            override fun onFailure(call: Call<Unit?>?, t: Throwable?) {
                Timber.e(t, "Failed to sign out")
            }

            override fun onResponse(call: Call<Unit?>?, response: Response<Unit?>?) {
                Timber.d("Signed out successfully $response")
            }
        })
    }

}
